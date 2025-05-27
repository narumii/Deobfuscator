package uwu.narumi.deobfuscator.core.other.impl.universal;

import uwu.narumi.deobfuscator.api.transformer.ComposedTransformer;
import uwu.narumi.deobfuscator.core.other.impl.clean.peephole.DeadCodeCleanTransformer;
import uwu.narumi.deobfuscator.core.other.impl.universal.flow.CleanRedundantJumpsTransformer;
import uwu.narumi.deobfuscator.core.other.impl.universal.flow.CleanRedundantSwitchesTransformer;

public class UniversalFlowTransformer extends ComposedTransformer {
  public UniversalFlowTransformer() {
    super(
        // Clean up redundant ifs and switches
        CleanRedundantJumpsTransformer::new,
        CleanRedundantSwitchesTransformer::new,

        // Resolve all number operations
        UniversalNumberTransformer::new,

        // Last thing will be to clean up all dead code that is unreachable
        DeadCodeCleanTransformer::new
    );
  }
}
