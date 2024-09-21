package uwu.narumi.deobfuscator.core.other.composed;

import uwu.narumi.deobfuscator.api.transformer.ComposedTransformer;
import uwu.narumi.deobfuscator.core.other.composed.general.ComposedGeneralCleanTransformer;
import uwu.narumi.deobfuscator.core.other.composed.general.ComposedGeneralFlowTransformer;
import uwu.narumi.deobfuscator.core.other.composed.general.ComposedGeneralRepairTransformer;
import uwu.narumi.deobfuscator.core.other.impl.clean.peephole.DeadCodeCleanTransformer;
import uwu.narumi.deobfuscator.core.other.impl.hp888.HP888PackerTransformer;
import uwu.narumi.deobfuscator.core.other.impl.hp888.HP888StringTransformer;
import uwu.narumi.deobfuscator.core.other.impl.universal.UniversalNumberTransformer;

public class ComposedHP888Transformer extends ComposedTransformer {

    public ComposedHP888Transformer(String packedEndOfFile) {
        super(
                HP888StringTransformer::new,
                () -> new HP888PackerTransformer(packedEndOfFile),
                HP888StringTransformer::new,
                ComposedGeneralCleanTransformer::new,
                ComposedGeneralRepairTransformer::new,
                HP888StringTransformer::new,
                UniversalNumberTransformer::new,
                ComposedGeneralFlowTransformer::new,
                DeadCodeCleanTransformer::new
        );
    }

    public ComposedHP888Transformer() {
        super(
                HP888StringTransformer::new,
                ComposedGeneralCleanTransformer::new,
                ComposedGeneralRepairTransformer::new,
                HP888StringTransformer::new,
                UniversalNumberTransformer::new,
                ComposedGeneralFlowTransformer::new,
                DeadCodeCleanTransformer::new
        );
    }
}
