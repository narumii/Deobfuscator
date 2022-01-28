package uwu.narumi.deobfuscator.transformer.impl.radon;

import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.transformer.Transformer;

import java.util.*;

/*
    TODO: Cast/Size checks etc
 */
public class RadonStringPoolTransformer extends Transformer {

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        deobfuscator.classes().forEach(classNode -> {
            List<MethodNode> toRemove = new ArrayList<>();
            Map<Integer, String> strings = new HashMap<>();

            classNode.methods.stream()
                    .filter(methodNode -> isAccess(methodNode.access, ACC_PRIVATE))
                    .filter(methodNode -> isAccess(methodNode.access, ACC_STATIC))
                    .filter(methodNode -> methodNode.desc.equals("()V"))
                    .forEach(methodNode -> Arrays.stream(methodNode.instructions.toArray())
                            .filter(node -> node.getOpcode() == PUTSTATIC)
                            //.filter(node -> node.getNext().getOpcode() == GETSTATIC)
                            //.filter(node -> node.getPrevious().getOpcode() == ANEWARRAY)
                            .map(FieldInsnNode.class::cast)
                            .filter(node -> node.desc.equals("[Ljava/lang/String;"))
                            .findFirst().flatMap(node -> classNode.fields.stream().filter(field -> field.desc.equals(node.desc)).filter(field -> field.name.equals(node.name)).findFirst()).ifPresent(field -> {
                                int size = strings.size();
                                //TODO: Optimize this as fuck
                                Arrays.stream(methodNode.instructions.toArray())
                                        .filter(insn -> insn.getOpcode() == AASTORE)
                                        .filter(insn -> isString(insn.getPrevious()))
                                        .filter(insn -> isInteger(insn.getPrevious().getPrevious()))
                                        .forEach(insn -> strings.put(getInteger(insn.getPrevious().getPrevious()), getString(insn.getPrevious())));

                                if (strings.size() != size) {
                                    toRemove.add(methodNode);
                                    field.value = REMOVEABLE;
                                }
                            }));


            classNode.methods.forEach(methodNode -> Arrays.stream(methodNode.instructions.toArray())
                    .filter(node -> node.getOpcode() == GETSTATIC)
                    .filter(node -> isInteger(node.getNext()))
                    .filter(node -> node.getNext().getNext().getOpcode() == AALOAD)
                    .map(FieldInsnNode.class::cast)
                    .forEach(node -> classNode.fields.stream()
                            .filter(field -> field.value instanceof Removeable)
                            .filter(field -> field.desc.equals(node.desc))
                            .findFirst().ifPresent(field -> {
                                int position = getInteger(node.getNext());
                                methodNode.instructions.remove(node.getNext().getNext());
                                methodNode.instructions.remove(node.getNext());
                                methodNode.instructions.set(node, new LdcInsnNode(strings.get(position)));
                            })));

            findClInit(classNode).ifPresent(clinit -> Arrays.stream(clinit.instructions.toArray())
                    .filter(node -> node.getOpcode() == INVOKESTATIC)
                    .map(MethodInsnNode.class::cast)
                    .filter(node -> node.owner.equals(classNode.name))
                    .filter(node -> toRemove.stream().anyMatch(method -> method.name.equals(node.name) && method.desc.equals(node.desc)))
                    .forEach(node -> clinit.instructions.remove(node)));

            classNode.methods.removeAll(toRemove);
            classNode.fields.removeIf(fieldNode -> fieldNode.desc.equals("[Ljava/lang/String;") && fieldNode.value instanceof Removeable);
            toRemove.clear();
            strings.clear();
        });
    }
}