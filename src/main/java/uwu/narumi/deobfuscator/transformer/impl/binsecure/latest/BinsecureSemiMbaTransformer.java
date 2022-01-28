package uwu.narumi.deobfuscator.transformer.impl.binsecure.latest;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.asm.InstructionMatcher;
import uwu.narumi.deobfuscator.asm.InstructionOpcodes;
import uwu.narumi.deobfuscator.transformer.Transformer;

/*
    If repeat is lower than 4 it's working in 100% i think
    TODO: Find a better way to fix this shit
    TODO: hwfuihwuifhwauihdawoid
 */
public class BinsecureSemiMbaTransformer extends Transformer implements InstructionOpcodes {

    private static final InstructionMatcher[] arithmeticSubstitutionInstructionMatchers = {
            //-------------------------------------- Fixes
            InstructionMatcher.of(
                    ICONST_1,
                    ISUB,
                    ICONST_1,
                    IADD
            ).replacement(),

            InstructionMatcher.of(
                    ICONST_1,
                    IADD,
                    ICONST_1,
                    ISUB
            ).replacement(),

            InstructionMatcher.of(
                    SWAP,
                    DUP_X1,
                    ICONST_M1,
                    IXOR,
                    IOR,
                    SWAP,
                    IADD,
                    ICONST_1,
                    IADD
            ).replacement(IAND),

            InstructionMatcher.of(
                    ICONST_0,
                    ISUB
            ).replacement(),

            InstructionMatcher.of(
                    ICONST_M1,
                    ICONST_M1,
                    IXOR
            ).replacement(ICONST_0),

            //--------------------------------------

            InstructionMatcher.of(
                    iconst_m1,
                    imul
            ).replacement(INEG),

            InstructionMatcher.of(
                    iconst_m1,
                    ixor,
                    iconst_m1,
                    isub
            ).replacement(INEG),

            InstructionMatcher.of(
                    iconst_m1,
                    ixor,
                    iconst_1,
                    iadd
            ).replacement(INEG),

            InstructionMatcher.of(
                    iconst_1,
                    isub,
                    iconst_m1,
                    ixor
            ).replacement(INEG),

            InstructionMatcher.of(
                    iconst_m1,
                    iadd,
                    iconst_m1,
                    ixor
            ).replacement(INEG),

            InstructionMatcher.of(
                    ineg,
                    isub
            ).replacement(IADD),

            InstructionMatcher.of(
                    dup2, // [a, b, a, b]
                    ior, // [x, a, b]
                    dup_x2, // [x, a, b, x]
                    pop, // [a, b, x]
                    iand, // [y, x]
                    iadd // [out]
            ).replacement(IADD),

            InstructionMatcher.of(
                    ineg,
                    iadd
            ).replacement(ISUB),

            InstructionMatcher.of(
                    swap, // [a, b]
                    iconst_m1, // [-1, a, b]
                    ixor, // [x, b]
                    iadd, // [y]
                    iconst_m1,
                    ixor
            ).replacement(ISUB),
    };
    private final static InstructionMatcher[] mbaInstructionMatchers = {
            InstructionMatcher.of(
                    dup_x1,
                    ior,
                    swap,
                    isub
            ).replacement(ICONST_M1, IXOR, IAND),

            InstructionMatcher.of(
                    swap,
                    dup_x1,
                    iand,
                    isub
            ).replacement(ICONST_M1, IXOR, IAND),

            InstructionMatcher.of(
                    swap,
                    isub,
                    iconst_1,
                    isub
            ).replacement(ISUB, ICONST_M1, IXOR),

            InstructionMatcher.of(
                    swap,
                    iconst_m1,
                    ixor,
                    iadd
            ).replacement(ISUB, ICONST_M1, IXOR),

            InstructionMatcher.of(
                    ineg,
                    iconst_m1,
                    iadd
            ).replacement(ICONST_M1, IXOR),

            InstructionMatcher.of(
                    iconst_m1,
                    ixor,
                    iconst_1,
                    iadd
            ).replacement(INEG),

            InstructionMatcher.of(
                    iconst_1,
                    isub,
                    iconst_m1,
                    ixor
            ).replacement(INEG),

            InstructionMatcher.of(
                    iconst_m1,
                    ixor,
                    isub,
                    iconst_1,
                    isub
            ).replacement(IADD),

            InstructionMatcher.of(
                    dup2, // [x, y, x, y]
                    ixor, // [x^y, x, y]
                    dup_x2, // [x^y, x, y, x^y]
                    pop, // [x, y, x^y]
                    iand, // [x&y, x^y]
                    iconst_2, // [2, (x&y), x^y]
                    imul, // [2*(x&y), x^y]
                    iadd
            ).replacement(IADD),

            InstructionMatcher.of(
                    dup2,
                    ior,
                    dup_x2,
                    pop,
                    iand,
                    iadd
            ).replacement(IADD),

            InstructionMatcher.of(
                    dup2, // [x, y, x, y]
                    ior, // [x|y, x, y]
                    iconst_2, // [2, x|y, x, y]
                    imul, // [2*(x|y), x, y]
                    dup_x2, // [x|y, x, y, x|y]
                    pop, // [x, y, x|y]
                    ixor, // [x^y, x|y]
                    isub
            ).replacement(IADD),

            InstructionMatcher.of(
                    iconst_m1,
                    ixor,
                    iadd,
                    iconst_1,
                    iadd
            ).replacement(ISUB),

            InstructionMatcher.of(
                    dup2,
                    ixor,
                    dup_x2,
                    pop,
                    swap,
                    iconst_m1,
                    ixor,
                    iand,
                    iconst_2,
                    imul,
                    isub
            ).replacement(ISUB),

            InstructionMatcher.of(
                    dup2,
                    iconst_m1,
                    ixor,
                    iand,
                    dup_x2,
                    pop,
                    swap,
                    iconst_m1,
                    ixor,
                    iand,
                    isub
            ).replacement(ISUB),

            InstructionMatcher.of(
                    dup2,
                    iconst_m1,
                    ixor,
                    iand,
                    iconst_2,
                    imul,
                    dup_x2,
                    pop,
                    ixor,
                    isub
            ).replacement(ISUB),

            InstructionMatcher.of(
                    dup2,
                    ior,
                    dup_x2,
                    pop,
                    iand,
                    isub
            ).replacement(IXOR),

            InstructionMatcher.of(
                    dup2,
                    iconst_m1,
                    ixor,
                    iand,
                    dup_x2,
                    pop,
                    swap,
                    iconst_m1,
                    ixor,
                    iand,
                    ior
            ).replacement(IXOR),

            InstructionMatcher.of(
                    dup2,
                    ior,
                    dup_x2,
                    pop,
                    iconst_m1,
                    ixor,
                    swap,
                    iconst_m1,
                    ixor,
                    ior,
                    iand
            ).replacement(IXOR),

            InstructionMatcher.of(
                    dup2,
                    ior,
                    dup_x2,
                    pop,
                    iand,
                    iconst_m1,
                    ixor,
                    iand
            ).replacement(IXOR),

            InstructionMatcher.of(
                    dup_x1,
                    iconst_m1,
                    ixor,
                    iand,
                    iadd
            ).replacement(IOR),

            InstructionMatcher.of(
                    swap,
                    dup_x1,
                    iconst_m1,
                    ixor,
                    ior,
                    swap,
                    iconst_m1,
                    ixor,
                    isub
            ).replacement(IAND),
    };

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        deobfuscator.classes().stream()
                .flatMap(classNode -> classNode.methods.stream())
                .forEach(methodNode -> {
                    resolve(methodNode, mbaInstructionMatchers);
                    resolve(methodNode, arithmeticSubstitutionInstructionMatchers);
                });
    }

    private void resolve(MethodNode methodNode, InstructionMatcher... matchers) {
        boolean modified;

        do {
            modified = false;

            AbstractInsnNode[] nodes = methodNode.instructions.toArray();
            for (int i = nodes.length - 1; i > 0; i--) {
                for (int j = matchers.length - 1; j >= 0; j--) {
                    if (matchers[j].matchAndReplace(methodNode, nodes[i])) {
                        modified = true;
                    }
                }
            }
        } while (modified);
    }
}
