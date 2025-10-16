package uwu.narumi.deobfuscator.core.other.composed;

import uwu.narumi.deobfuscator.api.transformer.ComposedTransformer;
import uwu.narumi.deobfuscator.core.other.composed.general.ComposedPeepholeCleanTransformer;
import uwu.narumi.deobfuscator.core.other.impl.clean.LocalVariableNamesCleanTransformer;
import uwu.narumi.deobfuscator.core.other.impl.pool.InlineLocalVariablesTransformer;
import uwu.narumi.deobfuscator.core.other.impl.sb27.SuperblaubeereInvokeDynamicTransformer;
import uwu.narumi.deobfuscator.core.other.impl.sb27.SuperblaubeereStringTransformer;
import uwu.narumi.deobfuscator.core.other.impl.universal.InlinePureFunctionsTransformer;
import uwu.narumi.deobfuscator.core.other.impl.universal.UniversalFlowTransformer;
import uwu.narumi.deobfuscator.core.other.impl.universal.UniversalNumberTransformer;
import uwu.narumi.deobfuscator.core.other.impl.universal.pool.UniversalNumberPoolTransformer;
import uwu.narumi.deobfuscator.core.other.impl.universal.pool.UniversalStringPoolTransformer;

/**
 * https://github.com/superblaubeere27/obfuscator
 */
public class ComposedSuperblaubeereTransformer extends ComposedTransformer {
  public ComposedSuperblaubeereTransformer() {
    super(
        // Remove var names as they are obfuscated and names are useless
        LocalVariableNamesCleanTransformer::new,

        UniversalNumberTransformer::new,
        UniversalNumberPoolTransformer::new,

        // Fix flow
        UniversalFlowTransformer::new,

        // Decrypt strings
        SuperblaubeereStringTransformer::new,
        UniversalStringPoolTransformer::new,
        SuperblaubeereInvokeDynamicTransformer::new,

        InlinePureFunctionsTransformer::new,
        InlineLocalVariablesTransformer::new,

        // Cleanup
        ComposedPeepholeCleanTransformer::new
    );
  }
}
