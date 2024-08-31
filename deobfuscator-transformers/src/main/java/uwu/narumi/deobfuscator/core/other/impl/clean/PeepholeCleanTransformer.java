package uwu.narumi.deobfuscator.core.other.impl.clean;

import uwu.narumi.deobfuscator.api.transformer.ComposedTransformer;
import uwu.narumi.deobfuscator.core.other.impl.clean.peephole.DeadCodeCleanTransformer;
import uwu.narumi.deobfuscator.core.other.impl.clean.peephole.NopCleanTransformer;
import uwu.narumi.deobfuscator.core.other.impl.clean.peephole.UnUsedLabelCleanTransformer;
import uwu.narumi.deobfuscator.core.other.impl.clean.peephole.PopUnUsedLocalVariablesTransformer;
import uwu.narumi.deobfuscator.core.other.impl.clean.peephole.UselessGotosCleanTransformer;
import uwu.narumi.deobfuscator.core.other.impl.clean.peephole.UselessPopCleanTransformer;

/**
 * A transformer that cleans up garbage instructions.
 */
public class PeepholeCleanTransformer extends ComposedTransformer {

  public PeepholeCleanTransformer() {
    super(
        // Remove dead code
        DeadCodeCleanTransformer::new,
        // Some more garbage instructions cleanup
        NopCleanTransformer::new,
        UnUsedLabelCleanTransformer::new,
        UselessGotosCleanTransformer::new,

        // Pop ununsed local variables stores
        PopUnUsedLocalVariablesTransformer::new,
        // Remove useless POP instructions. This also cleans up garbage var stores from the PopUnUsedLocalVariablesTransformer
        UselessPopCleanTransformer::new
    );
  }
}
