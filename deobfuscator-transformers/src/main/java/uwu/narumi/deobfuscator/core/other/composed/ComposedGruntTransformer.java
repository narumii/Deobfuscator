package uwu.narumi.deobfuscator.core.other.composed;

import uwu.narumi.deobfuscator.api.transformer.ComposedTransformer;
import uwu.narumi.deobfuscator.core.other.composed.general.ComposedGeneralFlowTransformer;
import uwu.narumi.deobfuscator.core.other.impl.pool.InlineStaticFieldTransformer;
import uwu.narumi.deobfuscator.core.other.impl.universal.AccessRepairTransformer;
import uwu.narumi.deobfuscator.core.other.impl.universal.InlinePureFunctionsTransformer;

/**
 * https://github.com/SpartanB312/Grunt
 */
// TODO: String encryption
public class ComposedGruntTransformer extends ComposedTransformer {
  public ComposedGruntTransformer() {
    super(
        // Repair access
        AccessRepairTransformer::new,
        () -> new ComposedTransformer(true,
            // Fix flow
            ComposedGeneralFlowTransformer::new,
            InlineStaticFieldTransformer::new,
            // Inline pure functions
            InlinePureFunctionsTransformer::new
        )
    );
  }
}
