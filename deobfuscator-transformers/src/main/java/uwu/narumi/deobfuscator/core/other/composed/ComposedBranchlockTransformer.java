package uwu.narumi.deobfuscator.core.other.composed;

import uwu.narumi.deobfuscator.api.transformer.ComposedTransformer;
import uwu.narumi.deobfuscator.core.other.impl.branchlock.BranchlockCompabilityStringTransformer;
import uwu.narumi.deobfuscator.core.other.impl.branchlock.BranchlockSaltingTransformer;
import uwu.narumi.deobfuscator.core.other.impl.universal.UniversalNumberTransformer;

public class ComposedBranchlockTransformer extends ComposedTransformer {
  public ComposedBranchlockTransformer() {
    super(
        () -> new ComposedTransformer(true,
            UniversalNumberTransformer::new,
            () -> new BranchlockCompabilityStringTransformer(false),
            BranchlockSaltingTransformer::new)
    );
  }
}
