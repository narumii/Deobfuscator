package uwu.narumi.deobfuscator.transformer.impl.sb27;

import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.transformer.Transformer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/*
    Should i move this to SuperblaubeereStringPool?
    TODO: Don't use FieldNode from ASM
    TODO: Array length checks
 */
public class SuperblaubeereSourceInfoStringTransformer extends Transformer {

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        deobfuscator.classes().stream()
                .filter(classNode -> classNode.sourceFile != null)
                .filter(classNode -> classNode.sourceFile.contains("ä"))
                .filter(classNode -> classNode.sourceFile.contains("ü"))
                .filter(classNode -> classNode.sourceFile.contains("ö"))
                .forEach(classNode -> {
                    List<MethodNode> toRemove = new ArrayList<>();

                    classNode.methods.stream()
                            .filter(methodNode -> isAccess(methodNode.access, ACC_PRIVATE))
                            .filter(methodNode -> isAccess(methodNode.access, ACC_STATIC))
                            .filter(methodNode -> methodNode.desc.equals("()V"))
                            .forEach(methodNode -> Arrays.stream(methodNode.instructions.toArray())
                                    .filter(node -> node.getOpcode() == PUTSTATIC)
                                    .filter(node -> node.getPrevious().getOpcode() == INVOKEVIRTUAL)
                                    .filter(node -> isString(node.getPrevious().getPrevious()))
                                    .filter(node -> getString(node.getPrevious().getPrevious()).equals("ö"))
                                    .map(FieldInsnNode.class::cast)
                                    .filter(node -> node.desc.equals("[Ljava/lang/String;"))
                                    .findFirst().flatMap(node -> classNode.fields.stream().filter(field -> field.desc.equals(node.desc)).filter(field -> field.name.equals(node.name)).findFirst()).ifPresent(field -> {
                                        field.value = classNode.sourceFile.substring(classNode.sourceFile.indexOf("ä") + 1, classNode.sourceFile.lastIndexOf("ü")).split("ö");
                                        toRemove.add(methodNode);
                                    }));

                    classNode.methods.forEach(methodNode -> Arrays.stream(methodNode.instructions.toArray())
                            .filter(node -> node.getOpcode() == GETSTATIC)
                            .filter(node -> isInteger(node.getNext()))
                            .filter(node -> node.getNext().getNext().getOpcode() == AALOAD)
                            .map(FieldInsnNode.class::cast)
                            .forEach(node -> classNode.fields.stream()
                                    .filter(field -> field.value instanceof String[])
                                    .filter(field -> field.desc.equals(node.desc))
                                    .filter(field -> field.name.equals(node.name))
                                    .findFirst().ifPresent(field -> {
                                        int position = getInteger(node.getNext());
                                        methodNode.instructions.remove(node.getNext().getNext());
                                        methodNode.instructions.remove(node.getNext());
                                        methodNode.instructions.set(node, new LdcInsnNode(((String[]) field.value)[position]));
                                    })));

                    findClInit(classNode).ifPresent(clinit -> Arrays.stream(clinit.instructions.toArray())
                            .filter(node -> node.getOpcode() == INVOKESTATIC)
                            .map(MethodInsnNode.class::cast)
                            .filter(node -> node.owner.equals(classNode.name))
                            .filter(node -> toRemove.stream().anyMatch(method -> method.name.equals(node.name) && method.desc.equals(node.desc)))
                            .forEach(node -> clinit.instructions.remove(node)));

                    classNode.methods.removeAll(toRemove);
                    classNode.fields.removeIf(fieldNode -> fieldNode.desc.equals("[Ljava/lang/String;") && fieldNode.value instanceof String[]);
                    toRemove.clear();
                });
    }
}