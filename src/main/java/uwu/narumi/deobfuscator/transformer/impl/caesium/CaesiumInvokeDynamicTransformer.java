package uwu.narumi.deobfuscator.transformer.impl.caesium;

import org.objectweb.asm.tree.*;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.exception.TransformerException;
import uwu.narumi.deobfuscator.helper.ASMHelper;
import uwu.narumi.deobfuscator.transformer.Transformer;

import java.util.*;

public class CaesiumInvokeDynamicTransformer extends Transformer {

    private static final String BSM_DESC_START = "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;";
    private static final String BSM_DESC_END = ")Ljava/lang/Object;";

    private final Map<ClassNode, List<MethodNode>> toRemove = new HashMap<>();

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        deobfuscator.classes().forEach(classNode -> classNode.methods.forEach(methodNode -> Arrays.stream(methodNode.instructions.toArray())
                .filter(node -> node instanceof InvokeDynamicInsnNode)
                .map(InvokeDynamicInsnNode.class::cast)
                .filter(node -> node.bsm.getDesc().startsWith(BSM_DESC_START))
                .filter(node -> node.bsm.getDesc().endsWith(BSM_DESC_END))
                .filter(node -> node.bsmArgs.length >= 4)
                .forEach(node -> {
                    ClassNode handleClass = deobfuscator.getClasses().get(node.bsm.getOwner());
                    if (handleClass == null)
                        return;

                    MethodNode bootstrap = handleClass.methods.stream()
                            .filter(method -> method.name.equals(node.bsm.getName()))
                            .filter(method -> method.desc.equals(node.bsm.getDesc()))
                            .findAny()
                            .orElse(null);
                    if (bootstrap == null)
                        return;

                    AbstractInsnNode replacement;
                    try {
                        int key = getKey(bootstrap);
                        replacement = firstDecrypt(key, node.bsmArgs[0], node.bsmArgs[1], node.bsmArgs[2], node.bsmArgs[3]);
                    } catch (UnsupportedOperationException ignored) {
                        replacement = secondDecrypt(node.bsmArgs[0], node.bsmArgs[1], node.bsmArgs[2], node.bsmArgs[3]);
                    } catch (Exception e) {
                        throw new TransformerException(e);
                    }

                    methodNode.instructions.set(node, replacement);
                    toRemove.computeIfAbsent(handleClass, ignored -> new ArrayList<>()).add(bootstrap);
                })));

        toRemove.forEach(((classNode, methodNodes) -> classNode.methods.removeAll(methodNodes)));
        toRemove.clear();
    }

    private Integer getKey(MethodNode methodNode) {
        return getInteger(Arrays.stream(methodNode.instructions.toArray())
                .filter(ASMHelper::isInteger)
                .filter(node -> node.getNext().getOpcode() == IXOR)
                .filter(node -> isInteger(node.getNext().getNext()))
                .filter(node -> getInteger(node.getNext().getNext()) == 0xFF)
                .filter(node -> node.getNext().getNext().getNext().getOpcode() == IAND)
                .findFirst()
                .orElseThrow(UnsupportedOperationException::new));

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
