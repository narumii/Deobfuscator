package uwu.narumi.deobfuscator.core.other.composed;

import uwu.narumi.deobfuscator.api.transformer.ComposedTransformer;
import uwu.narumi.deobfuscator.core.other.composed.general.ComposedPeepholeCleanTransformer;
import uwu.narumi.deobfuscator.core.other.impl.clean.LocalVariableNamesCleanTransformer;
import uwu.narumi.deobfuscator.core.other.impl.pool.InlineStaticFieldTransformer;
import uwu.narumi.deobfuscator.core.other.impl.qprotect.qProtectFieldFlowTransformer;
import uwu.narumi.deobfuscator.core.other.impl.universal.pool.UniversalStringPoolTransformer;
import uwu.narumi.deobfuscator.core.other.impl.qprotect.qProtectStringTransformer;
import uwu.narumi.deobfuscator.core.other.impl.qprotect.qProtectTryCatchTransformer;
import uwu.narumi.deobfuscator.core.other.impl.qprotect.qProtectInvokeDynamicTransformer;
import uwu.narumi.deobfuscator.core.other.impl.universal.pool.UniversalNumberPoolTransformer;
import uwu.narumi.deobfuscator.core.other.impl.universal.TryCatchRepairTransformer;
import uwu.narumi.deobfuscator.core.other.impl.universal.UniversalFlowTransformer;
import uwu.narumi.deobfuscator.core.other.impl.universal.UniversalNumberTransformer;

/**
 * https://qtechnologies.dev/
 */
public class Composed_qProtectTransformer extends ComposedTransformer {
  public Composed_qProtectTransformer() {
    super(
        // This fixes some weird issues where "this" is used as a local variable name.
        LocalVariableNamesCleanTransformer::new,

        // Initial cleaning code from garbage
        UniversalNumberTransformer::new,
        InlineStaticFieldTransformer::new,

        // Inline number pools
        UniversalNumberPoolTransformer::new,
        // Decrypt method invocation
        qProtectInvokeDynamicTransformer::new,

        // Resolve qProtect flow that uses try-catches
        qProtectTryCatchTransformer::new,
        TryCatchRepairTransformer::new,
        UniversalFlowTransformer::new,

        // Decrypt strings
        qProtectStringTransformer::new,
        // Inline string pools
        UniversalStringPoolTransformer::new,

        // Inline fields again
        InlineStaticFieldTransformer::new,

        // Cleanup
        ComposedPeepholeCleanTransformer::new,

        // Remove field flow after cleaning code from garbage, so we can do pattern matching
        qProtectFieldFlowTransformer::new
    );
  }
}
