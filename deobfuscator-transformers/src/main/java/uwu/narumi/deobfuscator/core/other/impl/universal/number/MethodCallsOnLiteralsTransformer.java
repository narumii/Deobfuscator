package uwu.narumi.deobfuscator.core.other.impl.universal.number;

import uwu.narumi.deobfuscator.api.asm.InstructionContext;
import uwu.narumi.deobfuscator.api.asm.matcher.Match;
import uwu.narumi.deobfuscator.api.context.Context;
import uwu.narumi.deobfuscator.api.helper.AsmMathHelper;
import uwu.narumi.deobfuscator.api.transformer.FramedInstructionsTransformer;

/**
 * Simplifies method calls on constant literals.
 */
public class MethodCallsOnLiteralsTransformer extends FramedInstructionsTransformer {

  @Override
  protected boolean transformInstruction(Context context, InstructionContext insnContext) {
    // Transform method calls on literals
    for (Match mathMatch : AsmMathHelper.METHOD_CALLS_ON_LITERALS) {
      if (mathMatch.matches(insnContext)) {
        boolean success = mathMatch.transformation().transform(insnContext);
        if (success) {
          return true;
        }
      }
    }

    return false;
  }
}
