package uwu.narumi.deobfuscator.transformer.impl.sb27;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.helper.MathHelper;
import uwu.narumi.deobfuscator.transformer.Transformer;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/*
    TODO: Code clean up
    TODO: Checks
 */
public class SuperblaubeereInvokeDynamicTransformer extends Transformer {

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        deobfuscator.classes().forEach(classNode -> {
            List<MethodNode> methodsToRemove = new ArrayList<>();
            List<FieldInsnNode> fieldsToRemove = new ArrayList<>();

            Map<Integer, String> callInfos = new HashMap<>();
            Map<Integer, Type> types = new HashMap<>();

            classNode.methods.stream()
                    .filter(methodNode -> isAccess(methodNode.access, ACC_PRIVATE))
                    .filter(methodNode -> isAccess(methodNode.access, ACC_STATIC))
                    .filter(methodNode -> methodNode.desc.equals("()V"))
                    .forEach(methodNode -> {
                        AtomicBoolean found = new AtomicBoolean();
                        Arrays.stream(methodNode.instructions.toArray())
                                .filter(node -> node.getOpcode() == GETSTATIC)
                                .filter(node -> isInteger(node.getNext()))
                                .filter(node -> isType(node.getNext().getNext()) || isString(node.getNext().getNext()))
                                .filter(node -> node.getNext().getNext().getNext().getOpcode() == AASTORE)
                                .map(FieldInsnNode.class::cast)
                                .forEach(node -> {
                                    int position = getInteger(node.getNext());
                                    LdcInsnNode ldc = (LdcInsnNode) node.getNext().getNext();
                                    if (isType(ldc))
                                        types.put(position, getType(ldc));
                                    else
                                        callInfos.put(position, getString(ldc));

                                    fieldsToRemove.add(node);
                                    found.set(true);
                                });

                        if (found.get())
                            methodsToRemove.add(methodNode);
                    });

            classNode.methods.forEach(methodNode -> Arrays.stream(methodNode.instructions.toArray())
                    .filter(node -> node instanceof InvokeDynamicInsnNode)
                    .map(InvokeDynamicInsnNode.class::cast)
                    .filter(node -> MathHelper.INTEGER_PATTERN.matcher(node.name).matches())
                    .filter(node -> callInfos.containsKey(Integer.parseInt(node.name)))
                    .forEach(node -> {
                        String[] parts = callInfos.get(Integer.parseInt(node.name)).split(":");

                        String owner = parts[0].replace('.', '/');
                        String name = parts[1];
                        String desc = parts[2];
                        int type = parts[3].length();

                        //TODO: Maybe use switch?
                        //TODO: Interface checking
                        if (type <= 2) {
                            if (type == 2) {
                                methodNode.instructions.set(node, new MethodInsnNode(INVOKEVIRTUAL, owner, name, desc, false));
                            } else {
                                methodNode.instructions.set(node, new MethodInsnNode(INVOKESTATIC, owner, name, desc, false));
                            }
                        } else {
                            Type fieldType = types.get(Integer.parseInt(desc));
                            if (fieldType == null)
                                return;

                            if (type == 3) {
                                methodNode.instructions.set(node, new FieldInsnNode(GETFIELD, owner, name, fieldType.getDescriptor()));
                            } else if (type == 4) {
                                methodNode.instructions.set(node, new FieldInsnNode(GETSTATIC, owner, name, fieldType.getDescriptor()));
                            } else if (type == 5) {
                                methodNode.instructions.set(node, new FieldInsnNode(PUTFIELD, owner, name, fieldType.getDescriptor()));
                            } else {
                                methodNode.instructions.set(node, new FieldInsnNode(PUTSTATIC, owner, name, fieldType.getDescriptor()));
                            }
                        }
                    }));

            findClInit(classNode).ifPresent(clinit -> Arrays.stream(clinit.instructions.toArray())
                    .filter(node -> node.getOpcode() == INVOKESTATIC)
                    .map(MethodInsnNode.class::cast)
                    .filter(node -> node.owner.equals(classNode.name))
                    .filter(node -> methodsToRemove.stream().anyMatch(method -> method.name.equals(node.name) && method.desc.equals(node.desc)))
                    .forEach(node -> clinit.instructions.remove(node)));

            fieldsToRemove.forEach(fieldInsnNode -> classNode.fields.removeIf(fieldNode -> fieldNode.name.equals(fieldInsnNode.name)
                    && fieldNode.desc.equals(fieldInsnNode.desc)));
            classNode.methods.removeAll(methodsToRemove);
            classNode.methods.removeIf(methodNode -> methodNode.desc.equals("(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;"));

            fieldsToRemove.clear();
            methodsToRemove.clear();
            callInfos.clear();
            types.clear();
        });
    }
}