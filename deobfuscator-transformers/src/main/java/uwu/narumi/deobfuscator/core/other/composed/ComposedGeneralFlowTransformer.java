package uwu.narumi.deobfuscator.core.other.composed;

import uwu.narumi.deobfuscator.api.transformer.ComposedTransformer;
import uwu.narumi.deobfuscator.core.other.impl.clean.PeepholeCleanTransformer;
import uwu.narumi.deobfuscator.core.other.impl.clean.LineNumberCleanTransformer;
import uwu.narumi.deobfuscator.core.other.impl.clean.peephole.UnUsedLabelCleanTransformer;
import uwu.narumi.deobfuscator.core.other.impl.pool.InlineLocalVariablesTransformer;
import uwu.narumi.deobfuscator.core.other.impl.pool.InlineStaticFieldTransformer;
import uwu.narumi.deobfuscator.core.other.impl.universal.UniversalNumberTransformer;
import uwu.narumi.deobfuscator.core.other.impl.universal.UniversalFlowTransformer;

public class ComposedGeneralFlowTransformer extends ComposedTransformer {

  public ComposedGeneralFlowTransformer() {
    super(
        // Preparation
        LineNumberCleanTransformer::new,
        UnUsedLabelCleanTransformer::new,
        () -> new InlineStaticFieldTransformer(true, true),
        InlineLocalVariablesTransformer::new,

        () -> new ComposedTransformer(true, // Rerun if changed
            UniversalNumberTransformer::new,
            UniversalFlowTransformer::new
        ),

        PeepholeCleanTransformer::new
    );
  }
}
