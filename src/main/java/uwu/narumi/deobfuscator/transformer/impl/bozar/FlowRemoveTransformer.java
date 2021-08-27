package uwu.narumi.deobfuscator.transformer.impl.bozar;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.transformer.Transformer;

import java.util.Arrays;

public class FlowRemoveTransformer extends Transformer {

    /*
    Dumb as fuck but working (in 90%) xd
     */
    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        deobfuscator.classes().stream()
                .flatMap(classNode -> classNode.methods.stream())
                .forEach(methodNode -> {
                    Arrays.stream(methodNode.instructions.toArray())
                            .filter(node -> node.getOpcode() == GETSTATIC)
                            .map(FieldInsnNode.class::cast)
                            .filter(node -> node.desc.equals("J"))
                            .filter(node -> node.name.charAt(0) > 127)
                            .filter(node -> node.getNext() != null)
                            .filter(node -> isLong(node.getNext()))
                            .filter(node -> node.getNext().getNext() instanceof JumpInsnNode)
                            .forEach(node -> {
                                JumpInsnNode GOTO = (JumpInsnNode) node.getNext().getNext();
                                LabelNode labelNode = GOTO.label;
                                AbstractInsnNode end = labelNode;

                                int position = 0;
                                boolean failed = false;

                                if (labelNode.getNext().getOpcode() == LXOR) {
                                    position = 9;
                                } else if (labelNode.getNext().getOpcode() == LCMP) {
                                    position = 7;
                                } else if (labelNode.getNext().getOpcode() == LAND) {
                                    position = 12;
                                }

                                for (int i = 0; i < position; i++) {
                                    if (end == null) {
                                        failed = true;
                                        break;
                                    }

                                    end = end.getNext();
                                }

                                if (!failed) {
                                    getInstructionsBetween(node, end).forEach(a -> methodNode.instructions.remove(a));
                                }
                            });

                    methodNode.instructions.resetLabels();
                });
    }
}
