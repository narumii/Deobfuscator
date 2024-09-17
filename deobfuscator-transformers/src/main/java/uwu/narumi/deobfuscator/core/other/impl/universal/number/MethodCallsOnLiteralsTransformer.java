package uwu.narumi.deobfuscator.core.other.impl.universal.number;

import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.asm.matcher.Match;
import uwu.narumi.deobfuscator.api.context.Context;
import uwu.narumi.deobfuscator.api.helper.AsmMathHelper;
import uwu.narumi.deobfuscator.api.helper.FramedInstructionsStream;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

/**
 * Simplifies method calls on constant literals.
 */
public class MethodCallsOnLiteralsTransformer extends Transformer {

  @Override
  protected void transform(ClassWrapper scope, Context context) throws Exception {
    FramedInstructionsStream.of(scope, context).forEach(insnContext -> {
      // Transform method calls on literals
      for (Match mathMatch : AsmMathHelper.METHOD_CALLS_ON_LITERALS) {
        if (mathMatch.matches(insnContext)) {
          boolean success = mathMatch.transformation().transform(insnContext);
          if (success) {
            markChange();
          }
        }
      }
    });
  }
}
