package uwu.narumi.deobfuscator.core.other.impl.clean;

import uwu.narumi.deobfuscator.api.transformer.ComposedTransformer;
import uwu.narumi.deobfuscator.core.other.impl.clean.peephole.DeadCodeCleanTransformer;
import uwu.narumi.deobfuscator.core.other.impl.clean.peephole.NopCleanTransformer;
import uwu.narumi.deobfuscator.core.other.impl.clean.peephole.UnUsedLabelCleanTransformer;
import uwu.narumi.deobfuscator.core.other.impl.clean.peephole.UnUsedLocalVariablesCleanTransformer;
import uwu.narumi.deobfuscator.core.other.impl.clean.peephole.UselessJumpsCleanTransformer;
import uwu.narumi.deobfuscator.core.other.impl.clean.peephole.UselessPopCleanTransformer;

public class PeepholeCleanTransformer extends ComposedTransformer {

  public PeepholeCleanTransformer() {
    super(
        DeadCodeCleanTransformer::new,
        NopCleanTransformer::new,
        UnUsedLabelCleanTransformer::new,
        UselessJumpsCleanTransformer::new,

        UnUsedLocalVariablesCleanTransformer::new,
        UselessPopCleanTransformer::new
    );
  }
}
