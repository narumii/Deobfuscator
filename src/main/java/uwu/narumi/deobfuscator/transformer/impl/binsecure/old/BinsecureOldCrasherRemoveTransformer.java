package uwu.narumi.deobfuscator.transformer.impl.binsecure.old;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.transformer.Transformer;

public class BinsecureOldCrasherRemoveTransformer extends Transformer {

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        deobfuscator.classes().stream().flatMap(classNode -> classNode.methods.stream())
                .forEach(methodNode -> {
                    for (AbstractInsnNode node : methodNode.instructions.toArray()) {
                        if (node instanceof InvokeDynamicInsnNode
                                && node.getPrevious() instanceof InvokeDynamicInsnNode
                                && node.getPrevious().getPrevious() instanceof JumpInsnNode
                                && node.getPrevious().getPrevious().getPrevious().getOpcode() == GETSTATIC) {

                            methodNode.instructions.remove(node.getPrevious().getPrevious().getPrevious());
                            methodNode.instructions.remove(node.getPrevious().getPrevious());
                            methodNode.instructions.remove(node.getPrevious());
                            methodNode.instructions.remove(node);
                        } else if (node.getOpcode() == IFNULL
                                && node.getPrevious().getOpcode() == DUP
                                && node.getNext().getOpcode() == ATHROW) {

                            if (node.getPrevious().getPrevious().getOpcode() == GOTO)
                                methodNode.instructions.remove(node.getPrevious().getPrevious());

                            methodNode.instructions.remove(node.getPrevious());
                            methodNode.instructions.remove(node.getNext());
                            methodNode.instructions.remove(node);
                        } else if (node instanceof TypeInsnNode && ((TypeInsnNode) node).desc.equals("give up")
                                && node.getPrevious() instanceof JumpInsnNode
                                && node.getPrevious().getPrevious().getOpcode() == GETSTATIC
                                && node.getPrevious().getPrevious().getPrevious().getOpcode() == ACONST_NULL) {

                            if (node.getNext().getNext().getOpcode() == POP)
                                methodNode.instructions.remove(node.getNext().getNext());

                            methodNode.instructions.remove(node.getPrevious().getPrevious().getPrevious());
                            methodNode.instructions.remove(node.getPrevious().getPrevious());
                            methodNode.instructions.remove(node.getPrevious());
                            methodNode.instructions.remove(node);
                        }
                    }
                });
    }
}
