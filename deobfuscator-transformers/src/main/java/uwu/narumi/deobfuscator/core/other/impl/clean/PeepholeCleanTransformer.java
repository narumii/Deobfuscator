package uwu.narumi.deobfuscator.core.other.impl.clean;

import uwu.narumi.deobfuscator.api.transformer.ComposedTransformer;
import uwu.narumi.deobfuscator.core.other.impl.clean.peephole.DeadCodeCleanTransformer;
import uwu.narumi.deobfuscator.core.other.impl.clean.peephole.NopCleanTransformer;
import uwu.narumi.deobfuscator.core.other.impl.clean.peephole.UnUsedLabelCleanTransformer;
import uwu.narumi.deobfuscator.core.other.impl.clean.peephole.PopUnUsedLocalVariablesTransformer;
import uwu.narumi.deobfuscator.core.other.impl.clean.peephole.UselessJumpsCleanTransformer;
import uwu.narumi.deobfuscator.core.other.impl.clean.peephole.UselessPopCleanTransformer;

/**
 * A transformer that cleans up garbage instructions.
 */
public class PeepholeCleanTransformer extends ComposedTransformer {

  public PeepholeCleanTransformer() {
    super(
        DeadCodeCleanTransformer::new,
        NopCleanTransformer::new,
        UnUsedLabelCleanTransformer::new,
        UselessJumpsCleanTransformer::new,

        PopUnUsedLocalVariablesTransformer::new,
        UselessPopCleanTransformer::new
    );
  }
}
