package uwu.narumi.deobfuscator.transformer.impl.qprotect.v1_7_2;

import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.transformer.Transformer;

import java.util.*;

//I think i've seen this shit before, but where? (also idk if it's original qprotect)
public class qProtectNumberPoolTransformer extends Transformer {

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        deobfuscator.classes().forEach(classNode -> {
            List<MethodNode> toRemove = new ArrayList<>();
            Map<Integer, Integer> numbers = new HashMap<>();

            classNode.methods.stream()
                    .filter(methodNode -> isAccess(methodNode.access, ACC_PUBLIC))
                    .filter(methodNode -> isAccess(methodNode.access, ACC_STATIC))
                    .filter(methodNode -> methodNode.desc.equals("()V"))
                    .forEach(methodNode -> Arrays.stream(methodNode.instructions.toArray())
                            .filter(node -> node.getOpcode() == PUTSTATIC)
                            .map(FieldInsnNode.class::cast)
                            .filter(node -> node.desc.equals("[Ljava/lang/Integer;"))
                            .findFirst().flatMap(node -> classNode.fields.stream().filter(field -> field.desc.equals(node.desc)).filter(field -> field.name.equals(node.name)).findFirst()).ifPresent(field -> {
                                field.value = REMOVEABLE;

                                //TODO: Optimize this as fuck
                                Arrays.stream(methodNode.instructions.toArray())
                                        .filter(insn -> insn.getOpcode() == GETSTATIC)
                                        .filter(insn -> isInteger(insn.getNext()))
                                        .filter(insn -> isInteger(insn.getNext().getNext()))
                                        .filter(insn -> insn.getNext().getNext().getNext() instanceof MethodInsnNode)
                                        .filter(insn -> insn.getNext().getNext().getNext().getNext().getOpcode() == AASTORE)
                                        .map(FieldInsnNode.class::cast)
                                        .filter(insn -> insn.name.equals(field.name) && insn.desc.equals(field.desc))
                                        .forEach(insn -> numbers.put(getInteger(insn.getNext()), getInteger(insn.getNext().getNext())));

                                if (!numbers.isEmpty())
                                    toRemove.add(methodNode);
                            }));


            classNode.methods.forEach(methodNode -> Arrays.stream(methodNode.instructions.toArray())
                    .filter(node -> node.getOpcode() == GETSTATIC)
                    .filter(node -> isInteger(node.getNext()))
                    .filter(node -> node.getNext().getNext().getOpcode() == AALOAD)
                    .map(FieldInsnNode.class::cast)
                    .forEach(node -> classNode.fields.stream()
                            .filter(field -> field.value instanceof Removeable)
                            .filter(field -> field.desc.equals(node.desc))
                            .filter(field -> field.name.equals(node.name)).findFirst().ifPresent(field -> {
                                int position = getInteger(node.getNext());

                                if (node.getNext().getNext().getNext() instanceof MethodInsnNode) {
                                    MethodInsnNode methodInsnNode = (MethodInsnNode) node.getNext().getNext().getNext();
                                    if (methodInsnNode.owner.equals("java/lang/Integer") && methodInsnNode.name.equals("intValue")) {
                                        methodNode.instructions.remove(node.getNext().getNext().getNext());
                                    }
                                }

                                methodNode.instructions.remove(node.getNext().getNext());
                                methodNode.instructions.remove(node.getNext());

                                methodNode.instructions.set(node, getNumber(numbers.get(position)));
                            })));

            findClInit(classNode).ifPresent(clinit -> Arrays.stream(clinit.instructions.toArray())
                    .filter(node -> node.getOpcode() == INVOKESTATIC)
                    .map(MethodInsnNode.class::cast)
                    .filter(node -> node.owner.equals(classNode.name))
                    .filter(node -> toRemove.stream().anyMatch(method -> method.name.equals(node.name) && method.desc.equals(node.desc)))
                    .forEach(node -> clinit.instructions.remove(node)));

            classNode.methods.removeAll(toRemove);
            classNode.fields.removeIf(fieldNode -> fieldNode.desc.equals("[Ljava/lang/Integer;") && fieldNode.value instanceof Removeable);
            toRemove.clear();
            numbers.clear();
        });
    }
}