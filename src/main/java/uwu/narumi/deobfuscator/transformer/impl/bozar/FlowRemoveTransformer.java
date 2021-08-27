package uwu.narumi.deobfuscator.transformer.impl.bozar;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
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
                                AbstractInsnNode end = GOTO.label;

                                boolean failed = false;
                                int position = end.getNext().getOpcode() == LXOR ? 9
                                        : end.getNext().getOpcode() == LCMP ? 7
                                        : end.getNext().getOpcode() == LAND ? 12
                                        : 0;

                                for (int i = 0; i < position; i++) {
                                    if (end == null) {
                                        failed = true;
                                        break;
                                    }

                                    end = end.getNext();
                                }

                                if (!failed) {
                                    getInstructionsBetween(node, end).forEach(methodNode.instructions::remove);
                                }
                            });

                    methodNode.instructions.resetLabels();
                });
    }
}
