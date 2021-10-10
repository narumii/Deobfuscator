package uwu.narumi.deobfuscator.transformer.impl.caesium;

import org.objectweb.asm.Handle;
import org.objectweb.asm.tree.*;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.helper.MathHelper;
import uwu.narumi.deobfuscator.transformer.Transformer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class CaesiumInvokeDynamicTransformer extends Transformer {

    private final List<MethodNode> methodsToRemove = new ArrayList<>();
    private final List<FieldInsnNode> fieldsToRemove = new ArrayList<>();

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        deobfuscator.classes().forEach(classNode -> {
            classNode.methods.forEach(methodNode -> Arrays.stream(methodNode.instructions.toArray())
                    .filter(node -> node instanceof InvokeDynamicInsnNode)
                    .map(InvokeDynamicInsnNode.class::cast)
                    .filter(node -> MathHelper.INTEGER_PATTERN.matcher(node.name).matches())
                    .filter(node -> node.bsmArgs.length >= 4)
                    .forEach(node -> {
                        Object key = getKey(classNode, node.bsm);

                        methodNode.instructions.set(node,
                                key == null ? secondDecrypt(node.bsmArgs[0], node.bsmArgs[1], node.bsmArgs[2], node.bsmArgs[3])
                                        : firstDecrypt((int) key, node.bsmArgs[0], node.bsmArgs[1], node.bsmArgs[2], node.bsmArgs[3]));
                    }));

            /*
                <clinit> needs to be cleared
             */
            classNode.methods.removeAll(methodsToRemove);
            fieldsToRemove.forEach(fieldInsnNode ->
                    classNode.fields.removeIf(fieldNode -> fieldNode.name.equals(fieldInsnNode.name) && fieldNode.desc.equals(fieldInsnNode.desc)));

            methodsToRemove.clear();
            fieldsToRemove.clear();
        });
    }

    /*
    Ugly as fuck
     */
    private Object getKey(ClassNode classNode, Handle handle) {
        AtomicReference<FieldInsnNode> fieldInsnNode = new AtomicReference<>();
        AtomicInteger key = new AtomicInteger();

        findMethod(classNode, methodNode -> methodNode.name.equals(handle.getName()) && methodNode.desc.equals(handle.getDesc()))
                .ifPresent(methodNode -> {
                    methodsToRemove.add(methodNode);

                    fieldInsnNode.set(Arrays.stream(methodNode.instructions.toArray())
                            .filter(node -> node.getOpcode() == GETSTATIC)
                            .map(FieldInsnNode.class::cast)
                            .filter(node -> node.desc.equals("I"))
                            .filter(node -> check(node.getNext(), IXOR))
                            .findFirst()
                            .orElse(null));
                });

        if (fieldInsnNode.get() != null) {
            fieldsToRemove.add(fieldInsnNode.get());

            findClInit(classNode).ifPresent(methodNode -> Arrays.stream(methodNode.instructions.toArray())
                    .filter(node -> node instanceof FieldInsnNode)
                    .map(FieldInsnNode.class::cast)
                    .filter(node -> node.name.equals(fieldInsnNode.get().name))
                    .filter(node -> node.desc.equals("I"))
                    .filter(node -> isInteger(node.getPrevious()))
                    .forEach(node -> key.set(getInteger(node.getPrevious()))));

            return key.get();
        } else {
            return null;
        }
    }

    private MethodInsnNode firstDecrypt(int key, Object var3, Object var4, Object var5, Object var6) {
        int type = (((int) var3) ^ key) & 255;

        String className = new String(Base64.getDecoder().decode((String) var4)).replace('.', '/');
        String methodName = new String(Base64.getDecoder().decode((String) var5));
        String desc = (String) var6;

        return new MethodInsnNode(type == 184 ? INVOKESTATIC : INVOKEVIRTUAL, className, methodName, desc, false);
    }

    private MethodInsnNode secondDecrypt(Object var3, Object var4, Object var5, Object var6) {
        String className = new String(Base64.getDecoder().decode((String) var4)).replace('.', '/');
        String methodName = new String(Base64.getDecoder().decode((String) var5));
        String desc = (String) var6;

        return new MethodInsnNode((int) var3 == 184 ? INVOKESTATIC : INVOKEVIRTUAL, className, methodName, desc, false);
    }
}
