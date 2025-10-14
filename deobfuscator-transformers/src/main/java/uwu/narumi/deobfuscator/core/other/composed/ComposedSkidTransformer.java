package uwu.narumi.deobfuscator.core.other.composed;

import uwu.narumi.deobfuscator.api.transformer.ComposedTransformer;
import uwu.narumi.deobfuscator.core.other.composed.general.ComposedGeneralFlowTransformer;
import uwu.narumi.deobfuscator.core.other.impl.clean.peephole.NopCleanTransformer;
import uwu.narumi.deobfuscator.core.other.impl.skidfuscator.SkidFlowTransformer;
import uwu.narumi.deobfuscator.core.other.impl.skidfuscator.SkidNumberTransformer;
import uwu.narumi.deobfuscator.core.other.impl.skidfuscator.SkidStringTransformer;
import uwu.narumi.deobfuscator.core.other.impl.universal.UniversalNumberTransformer;

public class ComposedSkidTransformer extends ComposedTransformer {

    public ComposedSkidTransformer() {
      super(
          UniversalNumberTransformer::new,
          SkidNumberTransformer::new,
          () -> new ComposedTransformer(true,
              NopCleanTransformer::new,
              SkidFlowTransformer::new,
              ComposedGeneralFlowTransformer::new
          ),
          SkidStringTransformer::new
      );
    }
}
