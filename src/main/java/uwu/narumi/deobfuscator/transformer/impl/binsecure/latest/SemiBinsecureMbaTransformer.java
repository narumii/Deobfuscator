package uwu.narumi.deobfuscator.transformer.impl.binsecure.latest;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.asm.ReplaceableInstructionMatcher;
import uwu.narumi.deobfuscator.transformer.Transformer;

/*
    TODO: Idk doesn't work in 100% i fucked up something xd (when repeat is above 1) but it's working
 */
public class SemiBinsecureMbaTransformer extends Transformer {

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        deobfuscator.classes().stream()
                .flatMap(classNode -> classNode.methods.stream())
                .forEach(methodNode -> {
                    boolean modified;

                    do {
                        modified = false;

                        for (AbstractInsnNode node : methodNode.instructions.toArray()) {

                            // x & ~y
                            if (new ReplaceableInstructionMatcher(methodNode, node,
                                    insn -> insn.getOpcode() == DUP_X1,
                                    insn -> insn.getOpcode() == IOR,
                                    insn -> insn.getOpcode() == SWAP,
                                    insn -> insn.getOpcode() == ISUB
                            )
                                    .matchAndReplace(
                                            new InsnNode(ICONST_M1),
                                            new InsnNode(IXOR),
                                            new InsnNode(IAND)
                                    )) {
                                modified = true;

                            } else if (new ReplaceableInstructionMatcher(methodNode, node,
                                    insn -> insn.getOpcode() == SWAP,
                                    insn -> insn.getOpcode() == DUP_X1,
                                    insn -> insn.getOpcode() == IADD,
                                    insn -> insn.getOpcode() == ISUB
                            )
                                    .matchAndReplace(
                                            new InsnNode(ICONST_M1),
                                            new InsnNode(IXOR),
                                            new InsnNode(IAND)
                                    )) {
                                modified = true;


                                // ~(x - y)
                            } else if (new ReplaceableInstructionMatcher(methodNode, node,
                                    insn -> insn.getOpcode() == SWAP,
                                    insn -> insn.getOpcode() == ISUB,
                                    insn -> insn.getOpcode() == ICONST_1,
                                    insn -> insn.getOpcode() == ISUB
                            )
                                    .matchAndReplace(
                                            new InsnNode(ISUB),
                                            new InsnNode(ICONST_M1),
                                            new InsnNode(IXOR)
                                    )) {
                                modified = true;

                            } else if (new ReplaceableInstructionMatcher(methodNode, node,
                                    insn -> insn.getOpcode() == SWAP,
                                    insn -> insn.getOpcode() == ICONST_M1,
                                    insn -> insn.getOpcode() == IXOR,
                                    insn -> insn.getOpcode() == IADD
                            )
                                    .matchAndReplace(
                                            new InsnNode(ISUB),
                                            new InsnNode(ICONST_M1),
                                            new InsnNode(IXOR)
                                    )) {
                                modified = true;


                                // ~x
                            } else if (new ReplaceableInstructionMatcher(methodNode, node,
                                    insn -> insn.getOpcode() == INEG,
                                    insn -> insn.getOpcode() == ICONST_M1,
                                    insn -> insn.getOpcode() == IADD
                            )
                                    .matchAndReplace(
                                            new InsnNode(ICONST_M1),
                                            new InsnNode(IXOR)
                                    )) {
                                modified = true;


                                //-x
                            } else if (new ReplaceableInstructionMatcher(methodNode, node,
                                    insn -> insn.getOpcode() == ICONST_M1,
                                    insn -> insn.getOpcode() == IXOR,
                                    insn -> insn.getOpcode() == ICONST_1,
                                    insn -> insn.getOpcode() == IADD
                            )
                                    .matchAndReplace(
                                            new InsnNode(INEG)
                                    )) {
                                modified = true;

                            } else if (new ReplaceableInstructionMatcher(methodNode, node,
                                    insn -> insn.getOpcode() == ICONST_1,
                                    insn -> insn.getOpcode() == ISUB,
                                    insn -> insn.getOpcode() == ICONST_M1,
                                    insn -> insn.getOpcode() == IXOR
                            )
                                    .matchAndReplace(
                                            new InsnNode(INEG)
                                    )) {
                                modified = true;


                                // x + y
                            } else if (new ReplaceableInstructionMatcher(methodNode, node,
                                    insn -> insn.getOpcode() == DUP2,
                                    insn -> insn.getOpcode() == IOR,
                                    insn -> insn.getOpcode() == ICONST_2,
                                    insn -> insn.getOpcode() == IMUL,
                                    insn -> insn.getOpcode() == DUP_X2,
                                    insn -> insn.getOpcode() == POP,
                                    insn -> insn.getOpcode() == IXOR,
                                    insn -> insn.getOpcode() == ISUB
                            )
                                    .matchAndReplace(
                                            new InsnNode(IADD)
                                    )) {
                                modified = true;

                            } else if (new ReplaceableInstructionMatcher(methodNode, node,
                                    insn -> insn.getOpcode() == DUP2,
                                    insn -> insn.getOpcode() == IOR,
                                    insn -> insn.getOpcode() == DUP_X2,
                                    insn -> insn.getOpcode() == POP,
                                    insn -> insn.getOpcode() == IAND,
                                    insn -> insn.getOpcode() == IADD
                            )
                                    .matchAndReplace(
                                            new InsnNode(IADD)
                                    )) {
                                modified = true;

                            } else if (new ReplaceableInstructionMatcher(methodNode, node,
                                    insn -> insn.getOpcode() == DUP2,
                                    insn -> insn.getOpcode() == IXOR,
                                    insn -> insn.getOpcode() == DUP_X2,
                                    insn -> insn.getOpcode() == POP,
                                    insn -> insn.getOpcode() == IAND,
                                    insn -> insn.getOpcode() == ICONST_2,
                                    insn -> insn.getOpcode() == IMUL,
                                    insn -> insn.getOpcode() == IADD
                            )
                                    .matchAndReplace(
                                            new InsnNode(IADD)
                                    )) {
                                modified = true;

                            } else if (new ReplaceableInstructionMatcher(methodNode, node,
                                    insn -> insn.getOpcode() == ICONST_M1,
                                    insn -> insn.getOpcode() == IXOR,
                                    insn -> insn.getOpcode() == ISUB,
                                    insn -> insn.getOpcode() == ICONST_1,
                                    insn -> insn.getOpcode() == ISUB
                            )
                                    .matchAndReplace(
                                            new InsnNode(IADD)
                                    )) {
                                modified = true;


                                // x - y
                            } else if (new ReplaceableInstructionMatcher(methodNode, node,
                                    insn -> insn.getOpcode() == DUP2,
                                    insn -> insn.getOpcode() == ICONST_M1,
                                    insn -> insn.getOpcode() == IXOR,
                                    insn -> insn.getOpcode() == IAND,
                                    insn -> insn.getOpcode() == ICONST_2,
                                    insn -> insn.getOpcode() == IMUL,
                                    insn -> insn.getOpcode() == DUP_X2,
                                    insn -> insn.getOpcode() == POP,
                                    insn -> insn.getOpcode() == IXOR,
                                    insn -> insn.getOpcode() == ISUB
                            )
                                    .matchAndReplace(
                                            new InsnNode(ISUB)
                                    )) {
                                modified = true;

                            } else if (new ReplaceableInstructionMatcher(methodNode, node,
                                    insn -> insn.getOpcode() == DUP2,
                                    insn -> insn.getOpcode() == ICONST_M1,
                                    insn -> insn.getOpcode() == IXOR,
                                    insn -> insn.getOpcode() == IAND,
                                    insn -> insn.getOpcode() == DUP_X2,
                                    insn -> insn.getOpcode() == POP,
                                    insn -> insn.getOpcode() == SWAP,
                                    insn -> insn.getOpcode() == ICONST_M1,
                                    insn -> insn.getOpcode() == IXOR,
                                    insn -> insn.getOpcode() == IAND,
                                    insn -> insn.getOpcode() == ISUB
                            )
                                    .matchAndReplace(
                                            new InsnNode(ISUB)
                                    )) {
                                modified = true;

                            } else if (new ReplaceableInstructionMatcher(methodNode, node,
                                    insn -> insn.getOpcode() == DUP2,
                                    insn -> insn.getOpcode() == IXOR,
                                    insn -> insn.getOpcode() == DUP_X2,
                                    insn -> insn.getOpcode() == POP,
                                    insn -> insn.getOpcode() == SWAP,
                                    insn -> insn.getOpcode() == ICONST_M1,
                                    insn -> insn.getOpcode() == IXOR,
                                    insn -> insn.getOpcode() == IAND,
                                    insn -> insn.getOpcode() == ICONST_2,
                                    insn -> insn.getOpcode() == IMUL,
                                    insn -> insn.getOpcode() == ISUB
                            )
                                    .matchAndReplace(
                                            new InsnNode(ISUB)
                                    )) {
                                modified = true;

                            } else if (new ReplaceableInstructionMatcher(methodNode, node,
                                    insn -> insn.getOpcode() == ICONST_M1,
                                    insn -> insn.getOpcode() == IXOR,
                                    insn -> insn.getOpcode() == IADD,
                                    insn -> insn.getOpcode() == ICONST_1,
                                    insn -> insn.getOpcode() == IADD
                            )
                                    .matchAndReplace(
                                            new InsnNode(ISUB)
                                    )) {
                                modified = true;


                                // x ^ y
                            } else if (new ReplaceableInstructionMatcher(methodNode, node,
                                    insn -> insn.getOpcode() == DUP2,
                                    insn -> insn.getOpcode() == IOR,
                                    insn -> insn.getOpcode() == DUP_X2,
                                    insn -> insn.getOpcode() == POP,
                                    insn -> insn.getOpcode() == IAND,
                                    insn -> insn.getOpcode() == ICONST_M1,
                                    insn -> insn.getOpcode() == IXOR,
                                    insn -> insn.getOpcode() == IAND
                            )
                                    .matchAndReplace(
                                            new InsnNode(IXOR)
                                    )) {
                                modified = true;

                            } else if (new ReplaceableInstructionMatcher(methodNode, node,
                                    insn -> insn.getOpcode() == DUP2,
                                    insn -> insn.getOpcode() == IOR,
                                    insn -> insn.getOpcode() == DUP_X2,
                                    insn -> insn.getOpcode() == POP,
                                    insn -> insn.getOpcode() == ICONST_M1,
                                    insn -> insn.getOpcode() == IXOR,
                                    insn -> insn.getOpcode() == SWAP,
                                    insn -> insn.getOpcode() == ICONST_M1,
                                    insn -> insn.getOpcode() == IXOR,
                                    insn -> insn.getOpcode() == IOR,
                                    insn -> insn.getOpcode() == IAND
                            )
                                    .matchAndReplace(
                                            new InsnNode(IXOR)
                                    )) {
                                modified = true;

                            } else if (new ReplaceableInstructionMatcher(methodNode, node,
                                    insn -> insn.getOpcode() == DUP2,
                                    insn -> insn.getOpcode() == ICONST_M1,
                                    insn -> insn.getOpcode() == IXOR,
                                    insn -> insn.getOpcode() == IAND,
                                    insn -> insn.getOpcode() == DUP_X2,
                                    insn -> insn.getOpcode() == POP,
                                    insn -> insn.getOpcode() == SWAP,
                                    insn -> insn.getOpcode() == ICONST_M1,
                                    insn -> insn.getOpcode() == IXOR,
                                    insn -> insn.getOpcode() == IAND,
                                    insn -> insn.getOpcode() == IOR
                            )
                                    .matchAndReplace(
                                            new InsnNode(IXOR)
                                    )) {
                                modified = true;

                            } else if (new ReplaceableInstructionMatcher(methodNode, node,
                                    insn -> insn.getOpcode() == DUP2,
                                    insn -> insn.getOpcode() == IOR,
                                    insn -> insn.getOpcode() == DUP_X2,
                                    insn -> insn.getOpcode() == POP,
                                    insn -> insn.getOpcode() == IAND,
                                    insn -> insn.getOpcode() == ISUB
                            )
                                    .matchAndReplace(
                                            new InsnNode(IXOR)
                                    )) {
                                modified = true;


                                // x | y
                            } else if (new ReplaceableInstructionMatcher(methodNode, node,
                                    insn -> insn.getOpcode() == DUP_X1,
                                    insn -> insn.getOpcode() == ICONST_M1,
                                    insn -> insn.getOpcode() == IXOR,
                                    insn -> insn.getOpcode() == IAND,
                                    insn -> insn.getOpcode() == IADD
                            )
                                    .matchAndReplace(
                                            new InsnNode(IOR)
                                    )) {
                                modified = true;


                                // x & y
                            } else if (new ReplaceableInstructionMatcher(methodNode, node,
                                    insn -> insn.getOpcode() == SWAP,
                                    insn -> insn.getOpcode() == DUP_X1,
                                    insn -> insn.getOpcode() == ICONST_M1,
                                    insn -> insn.getOpcode() == IXOR,
                                    insn -> insn.getOpcode() == IOR,
                                    insn -> insn.getOpcode() == SWAP,
                                    insn -> insn.getOpcode() == ICONST_M1,
                                    insn -> insn.getOpcode() == IXOR,
                                    insn -> insn.getOpcode() == ISUB
                            )
                                    .matchAndReplace(
                                            new InsnNode(IAND)
                                    )) {
                                modified = true;
                            }

                        }

                    } while (modified);
                });
    }
}
