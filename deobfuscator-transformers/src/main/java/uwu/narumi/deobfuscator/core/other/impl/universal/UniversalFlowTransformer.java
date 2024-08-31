package uwu.narumi.deobfuscator.core.other.impl.universal;

import uwu.narumi.deobfuscator.api.transformer.ComposedTransformer;
import uwu.narumi.deobfuscator.core.other.impl.clean.peephole.DeadCodeCleanTransformer;
import uwu.narumi.deobfuscator.core.other.impl.universal.flow.CleanRedundantJumpsTransformer;
import uwu.narumi.deobfuscator.core.other.impl.universal.flow.CleanRedundantSwitchesTransformer;

public class UniversalFlowTransformer extends ComposedTransformer {
  public UniversalFlowTransformer() {
    super(
        // Resolve all number operations in the first place
        UniversalNumberTransformer::new,

        // JumpPredictingAnalyzer is so smart that predicted jumps can be removed just by DeadCodeCleanTransformer.
        DeadCodeCleanTransformer::new,

        // Just need to clean up those ifs and switches by those transformers below
        CleanRedundantJumpsTransformer::new,
        CleanRedundantSwitchesTransformer::new
    );
  }
}
