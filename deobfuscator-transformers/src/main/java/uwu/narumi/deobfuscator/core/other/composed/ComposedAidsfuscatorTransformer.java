package uwu.narumi.deobfuscator.core.other.composed;

import uwu.narumi.deobfuscator.api.transformer.ComposedTransformer;
import uwu.narumi.deobfuscator.core.other.composed.general.ComposedGeneralRepairTransformer;
import uwu.narumi.deobfuscator.core.other.composed.general.ComposedPeepholeCleanTransformer;
import uwu.narumi.deobfuscator.core.other.impl.aidsfuscator.AidsfuscatorClassSaltTransformer;
import uwu.narumi.deobfuscator.core.other.impl.aidsfuscator.AidsfuscatorConstantsFixTransformer;
import uwu.narumi.deobfuscator.core.other.impl.aidsfuscator.AidsfuscatorFlowFlatteningTransformer;
import uwu.narumi.deobfuscator.core.other.impl.aidsfuscator.AidsfuscatorIntegerTransformer;
import uwu.narumi.deobfuscator.core.other.impl.aidsfuscator.AidsfuscatorReferenceObfuscationTransformer;
import uwu.narumi.deobfuscator.core.other.impl.aidsfuscator.AidsfuscatorStringTransformer;
import uwu.narumi.deobfuscator.core.other.impl.universal.StringBuilderTransformer;
import uwu.narumi.deobfuscator.core.other.impl.universal.UniversalNumberTransformer;

public class ComposedAidsfuscatorTransformer extends ComposedTransformer {
  public ComposedAidsfuscatorTransformer() {
    super(
        ComposedGeneralRepairTransformer::new,

        AidsfuscatorFlowFlatteningTransformer::new,
        AidsfuscatorReferenceObfuscationTransformer::new,

        AidsfuscatorClassSaltTransformer::new,
        AidsfuscatorStringTransformer::new,

        UniversalNumberTransformer::new,

        AidsfuscatorIntegerTransformer::new,
        UniversalNumberTransformer::new,

        AidsfuscatorConstantsFixTransformer::new,

        StringBuilderTransformer::new,

        () -> new ComposedTransformer(true,
            UniversalNumberTransformer::new,
            ComposedPeepholeCleanTransformer::new)
    );
  }
}
