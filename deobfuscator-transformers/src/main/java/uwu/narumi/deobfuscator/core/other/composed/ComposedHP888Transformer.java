package uwu.narumi.deobfuscator.core.other.composed;

import uwu.narumi.deobfuscator.api.transformer.ComposedTransformer;
import uwu.narumi.deobfuscator.core.other.composed.general.ComposedGeneralFlowTransformer;
import uwu.narumi.deobfuscator.core.other.composed.general.ComposedGeneralRepairTransformer;
import uwu.narumi.deobfuscator.core.other.impl.clean.peephole.DeadCodeCleanTransformer;
import uwu.narumi.deobfuscator.core.other.impl.exploit.WebExploitRemoveTransformer;
import uwu.narumi.deobfuscator.core.other.impl.hp888.HP888PackerTransformer;
import uwu.narumi.deobfuscator.core.other.impl.hp888.HP888StringTransformer;
import uwu.narumi.deobfuscator.core.other.impl.universal.UniversalNumberTransformer;

/**
 * Transformers for custom-made obfuscator by HP888. Used in projects like https://safemc.pl/
 */
public class ComposedHP888Transformer extends ComposedTransformer {

  public ComposedHP888Transformer(String packedEndOfFile) {
    super(
        HP888StringTransformer::new,
        () -> new HP888PackerTransformer(packedEndOfFile),
        HP888StringTransformer::new,
        WebExploitRemoveTransformer::new,
        ComposedGeneralRepairTransformer::new,
        UniversalNumberTransformer::new,
        ComposedGeneralFlowTransformer::new,
        DeadCodeCleanTransformer::new
    );
  }

  public ComposedHP888Transformer() {
    super(
        HP888StringTransformer::new,
        WebExploitRemoveTransformer::new,
        ComposedGeneralRepairTransformer::new,
        UniversalNumberTransformer::new,
        ComposedGeneralFlowTransformer::new,
        DeadCodeCleanTransformer::new
    );
  }
}
