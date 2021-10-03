package uwu.narumi.deobfuscator.transformer.impl.paramorphism;

import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.VarInsnNode;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.transformer.Transformer;

import java.util.Arrays;

public class ParamorphismFlowTransformer extends Transformer {

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        deobfuscator.classes().stream()
                .flatMap(classNode -> classNode.methods.stream())
                .forEach(methodNode -> {
                    Arrays.stream(methodNode.instructions.toArray())
                            .filter(node -> node.getOpcode() == GOTO)
                            .map(JumpInsnNode.class::cast)
                            .forEach(node -> {
                                LabelNode labelNode = node.label;
                                if (labelNode.getNext().getOpcode() == GOTO
                                        && methodNode.instructions.indexOf(labelNode.getNext()) > methodNode.instructions.indexOf(node)) {
                                    methodNode.instructions.set(node, new JumpInsnNode(GOTO, ((JumpInsnNode) labelNode.getNext()).label));
                                } else if (labelNode.getNext() instanceof JumpInsnNode
                                        && labelNode.getNext().getNext() instanceof VarInsnNode
                                        && labelNode.getNext().getNext().getNext() instanceof JumpInsnNode) {
                                    methodNode.instructions.set(node, new JumpInsnNode(labelNode.getNext().getOpcode(), ((JumpInsnNode) labelNode.getNext()).label));
                                }
                            });

                    methodNode.tryCatchBlocks.removeIf(tbce -> tbce.type == null || tbce.type.isEmpty() || tbce.type.isBlank());
                });
    }
}
