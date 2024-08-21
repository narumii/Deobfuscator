package uwu.narumi.deobfuscator.core.other.impl.universal.number;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.OriginalSourceValue;
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.asm.matcher.rule.Match;
import uwu.narumi.deobfuscator.api.helper.AsmMathHelper;
import uwu.narumi.deobfuscator.api.transformer.FramedInstructionsTransformer;

/**
 * Simplifies method calls on constant literals.
 */
public class MethodCallsOnLiteralsTransformer extends FramedInstructionsTransformer {

  @Override
  protected boolean transformInstruction(ClassWrapper classWrapper, MethodNode methodNode, AbstractInsnNode insn, Frame<OriginalSourceValue> frame) {
    // Transform method calls on literals
    for (Match mathMatch : AsmMathHelper.METHOD_CALLS_ON_LITERALS) {
      if (mathMatch.test(insn)) {
        boolean success = mathMatch.transformation().transform(methodNode, insn, frame);
        if (success) {
          return true;
        }
      }
    }

    return false;
  }
}
