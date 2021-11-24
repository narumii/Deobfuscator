package uwu.narumi.deobfuscator.transformer.impl.monsey;

import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.transformer.Transformer;

import java.util.Arrays;

public class MonseyFakeJumpTransformer extends Transformer {

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        deobfuscator.classes().stream()
                .flatMap(classNode -> classNode.methods.stream())
                .forEach(methodNode -> Arrays.stream(methodNode.instructions.toArray())
                        .filter(node -> node.getOpcode() == IFEQ || node.getOpcode() == IFNE || node.getOpcode() == IFNULL || node.getOpcode() == IFNONNULL)
                        .forEach(node -> {
                            LabelNode labelNode = ((JumpInsnNode) node).label;
                            switch (node.getOpcode()) {
                                case IFNULL: {
                                    methodNode.instructions.remove(node.getPrevious());
                                    methodNode.instructions.set(node, new JumpInsnNode(GOTO, labelNode));
                                    break;
                                }
                                case IFNONNULL: {
                                    methodNode.instructions.remove(node.getPrevious());
                                    methodNode.instructions.remove(node);

                                    methodNode.instructions.remove(labelNode.getNext().getNext());
                                    methodNode.instructions.remove(labelNode.getNext());
                                    break;
                                }
                                case IFEQ: {
                                    if (isInteger(node.getPrevious())) {
                                        int number = getInteger(node.getPrevious());

                                        methodNode.instructions.remove(node.getPrevious());
                                        if (number == 0) {
                                            methodNode.instructions.set(node, new JumpInsnNode(GOTO, labelNode));
                                        } else {
                                            methodNode.instructions.remove(node);

                                            methodNode.instructions.remove(labelNode.getNext().getNext());
                                            methodNode.instructions.remove(labelNode.getNext());
                                        }
                                    } else if (isMethod(node.getPrevious(), "java/lang/String", "equals", "(Ljava/lang/Object;)Z")) {
                                        methodNode.instructions.remove(node.getPrevious().getPrevious().getPrevious());
                                        methodNode.instructions.remove(node.getPrevious().getPrevious());
                                        methodNode.instructions.remove(node.getPrevious());
                                        methodNode.instructions.set(node, new JumpInsnNode(GOTO, labelNode));
                                    }
                                    break;
                                }
                                case IFNE: {
                                    if (isInteger(node.getPrevious())) {
                                        int number = getInteger(node.getPrevious());

                                        methodNode.instructions.remove(node.getPrevious());
                                        if (number == 0) {
                                            methodNode.instructions.remove(node);
                                            methodNode.instructions.remove(labelNode.getNext().getNext());
                                            methodNode.instructions.remove(labelNode.getNext());
                                        } else {
                                            methodNode.instructions.set(node, new JumpInsnNode(GOTO, labelNode));
                                        }
                                    } else if (isMethod(node.getPrevious(), "java/lang/String", "equals", "(Ljava/lang/Object;)Z")) {
                                        methodNode.instructions.remove(node.getPrevious().getPrevious().getPrevious());
                                        methodNode.instructions.remove(node.getPrevious().getPrevious());
                                        methodNode.instructions.remove(node.getPrevious());
                                        methodNode.instructions.remove(node);

                                        methodNode.instructions.remove(labelNode.getNext().getNext());
                                        methodNode.instructions.remove(labelNode.getNext());
                                    }
                                    break;
                                }
                            }
                        }));
    }
}
