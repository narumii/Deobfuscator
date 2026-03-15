package uwu.narumi.deobfuscator.core.other.composed;

import uwu.narumi.deobfuscator.api.transformer.ComposedTransformer;
import uwu.narumi.deobfuscator.core.other.composed.general.ComposedGeneralFlowTransformer;
import uwu.narumi.deobfuscator.core.other.composed.general.ComposedPeepholeCleanTransformer;
import uwu.narumi.deobfuscator.core.other.impl.clean.LocalVariableNamesCleanTransformer;
import uwu.narumi.deobfuscator.core.other.impl.pool.InlineLocalVariablesTransformer;
import uwu.narumi.deobfuscator.core.other.impl.pool.InlineStaticFieldTransformer;
import uwu.narumi.deobfuscator.core.other.impl.qprotect.*;
import uwu.narumi.deobfuscator.core.other.impl.universal.*;
import uwu.narumi.deobfuscator.core.other.impl.universal.pool.UniversalStringPoolTransformer;
import uwu.narumi.deobfuscator.core.other.impl.universal.pool.UniversalNumberPoolTransformer;

/**
 * https://qtechnologies.dev/
 */
// TODO: Deobfuscate strong string encryption. Sample: qprotect.StringStrongEncryption
public class Composed_qProtectTransformer extends ComposedTransformer {
  public Composed_qProtectTransformer() {
    super(
        // This fixes some weird issues where "this" is used as a local variable name.
        LocalVariableNamesCleanTransformer::new,

        // Initial cleaning code from garbage
        UniversalNumberTransformer::new,
        InlineStaticFieldTransformer::new,
        InlineLocalVariablesTransformer::new,

        // Inline number pools, but keep byte arrays for StrongStringEncryption
        UniversalNumberPoolTransformer::new,
        // Decrypt method invocation
        qProtectInvokeDynamicTransformer::new,
        qProtectInvokeDynamicStrongTransformer::new,

        // Fix flow
        UniversalFlowTransformer::new,

        // Strings
        qProtectStringTransformer::new,
        qProtectAESStringEncryptionTransformer::new,

        // Inline string pools
        UniversalStringPoolTransformer::new,

        // Inline fields again
        InlineStaticFieldTransformer::new,
        InlineLocalVariablesTransformer::new,

        // Cleanup
        ComposedPeepholeCleanTransformer::new,

        // Resolve qProtect flow that uses try-catches
        qProtectTryCatchTransformer::new,
        TryCatchRepairTransformer::new,

        // Remove field flow after cleaning code from garbage, so we can do pattern matching
        qProtectFieldFlowTransformer::new
    );
  }

  public Composed_qProtectTransformer(boolean strongStrings) {
    super(
        // This fixes some weird issues where "this" is used as a local variable name.
        LocalVariableNamesCleanTransformer::new,

        // Initial cleaning code from garbage
        UniversalNumberTransformer::new,
        InlineStaticFieldTransformer::new,
        InlineLocalVariablesTransformer::new,

        // Inline number pools, but keep byte arrays for StrongStringEncryption
        UniversalNumberPoolTransformer::new,
        // Decrypt method invocation
        qProtectInvokeDynamicTransformer::new,
        qProtectInvokeDynamicStrongTransformer::new,

        // Fix flow
        UniversalFlowTransformer::new,

        // Inline fields again
        InlineStaticFieldTransformer::new,
        InlineLocalVariablesTransformer::new,

        // Cleanup
        ComposedPeepholeCleanTransformer::new,

        // Resolve qProtect flow that uses try-catches
        qProtectTryCatchTransformer::new,
        TryCatchRepairTransformer::new,

        // String Strings
        qProtectStrongStringTransformer::new,
        qProtectAESStringEncryptionTransformer::new,
        StringBuilderTransformer::new,

        // Inline string pools
        UniversalStringPoolTransformer::new,
        StringBuilderTransformer::new,

        // Remove field flow after cleaning code from garbage, so we can do pattern matching
        qProtectFieldFlowTransformer::new
    );
  }
}
