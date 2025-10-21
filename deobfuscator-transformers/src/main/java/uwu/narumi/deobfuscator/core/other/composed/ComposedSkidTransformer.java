package uwu.narumi.deobfuscator.core.other.composed;

import uwu.narumi.deobfuscator.api.transformer.ComposedTransformer;
import uwu.narumi.deobfuscator.core.other.impl.clean.peephole.*;
import uwu.narumi.deobfuscator.core.other.impl.pool.InlineLocalVariablesTransformer;
import uwu.narumi.deobfuscator.core.other.impl.pool.InlineStaticFieldTransformer;
import uwu.narumi.deobfuscator.core.other.impl.skidfuscator.*;
import uwu.narumi.deobfuscator.core.other.impl.universal.UniversalFlowTransformer;
import uwu.narumi.deobfuscator.core.other.impl.universal.UniversalNumberTransformer;

public class ComposedSkidTransformer extends ComposedTransformer {

    public ComposedSkidTransformer() {
      super(
          UniversalNumberTransformer::new,
          SkidNumberTransformer::new,
          SkidTryCatchRemoveTransformer::new,
          InlineStaticFieldTransformer::new,
          () -> new ComposedTransformer(true,
              InlineLocalVariablesTransformer::new, /* Passthrough Hash */
              SkidFlowTransformer::new, /* Resolve SkidFuscator's Flow */

              UniversalFlowTransformer::new, /* Solve SkidFuscator's Switches/Jumps */

              NopCleanTransformer::new,
              UnUsedLabelCleanTransformer::new,
              UselessGotosCleanTransformer::new,

              () -> new ComposedTransformer(true,
                  PopUnUsedLocalVariablesTransformer::new,
                  UselessPopCleanTransformer::new
              )
          ),
          SkidStringTransformer::new,
          SkidCleanTransformer::new
      );
    }
}
