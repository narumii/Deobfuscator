package uwu.narumi.deobfuscator.core.other.impl.universal;

import uwu.narumi.deobfuscator.api.transformer.ComposedTransformer;
import uwu.narumi.deobfuscator.core.other.impl.clean.peephole.UselessPopCleanTransformer;
import uwu.narumi.deobfuscator.core.other.impl.universal.number.*;

/**
 * Simplifies number operations on constant values.
 */
public class UniversalNumberTransformer extends ComposedTransformer {
  public UniversalNumberTransformer() {
    super(
        InlineConstantValuesTransformer::new,
        EmptyArrayLengthTransformer::new,

        UselessPopCleanTransformer::new
    );
  }
}
