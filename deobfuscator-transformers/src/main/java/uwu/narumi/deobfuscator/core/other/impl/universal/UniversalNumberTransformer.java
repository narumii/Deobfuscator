package uwu.narumi.deobfuscator.core.other.impl.universal;

import uwu.narumi.deobfuscator.api.transformer.ComposedTransformer;
import uwu.narumi.deobfuscator.core.other.impl.universal.number.CompareInstructionsTransformer;
import uwu.narumi.deobfuscator.core.other.impl.universal.number.MathOperationsTransformer;
import uwu.narumi.deobfuscator.core.other.impl.universal.number.MethodCallsOnLiteralsTransformer;
import uwu.narumi.deobfuscator.core.other.impl.universal.number.NumberCastsTransformer;

/**
 * Simplifies number operations on constant values.
 */
public class UniversalNumberTransformer extends ComposedTransformer {
  public UniversalNumberTransformer() {
    super(
        MethodCallsOnLiteralsTransformer::new,
        MathOperationsTransformer::new,
        NumberCastsTransformer::new,
        CompareInstructionsTransformer::new
    );

    this.rerunOnChange = true;
  }
}
