package uwu.narumi.deobfuscator.core.other.composed;

import org.jetbrains.annotations.Nullable;
import uwu.narumi.deobfuscator.api.transformer.ComposedTransformer;
import uwu.narumi.deobfuscator.core.other.impl.clean.LocalVariableNamesCleanTransformer;
import uwu.narumi.deobfuscator.core.other.impl.hp888.HP888PackerTransformer;
import uwu.narumi.deobfuscator.core.other.impl.hp888.HP888StringTransformer;
import uwu.narumi.deobfuscator.core.other.impl.universal.AccessRepairTransformer;
import uwu.narumi.deobfuscator.core.other.impl.universal.RecoverSyntheticsTransformer;
import uwu.narumi.deobfuscator.core.other.impl.universal.UniversalNumberTransformer;

/**
 * Transformers for custom-made obfuscator by HP888. Used in projects like https://safemc.pl/
 */
public class ComposedHP888Transformer extends ComposedTransformer {

  public ComposedHP888Transformer() {
    this(null);
  }

  public ComposedHP888Transformer(@Nullable String encryptedClassFilesSuffix) {
    super(
        // Decrypt strings
        HP888StringTransformer::new,

        () -> encryptedClassFilesSuffix != null ? new ComposedTransformer(
            // Unpack encrypted classes
            () -> new HP888PackerTransformer(encryptedClassFilesSuffix),
            // Decrypt strings in unpacked classes
            HP888StringTransformer::new
        ) : null,

        // Cleanup
        UniversalNumberTransformer::new,
        AccessRepairTransformer::new,
        LocalVariableNamesCleanTransformer::new,
        RecoverSyntheticsTransformer::new
    );
  }
}
