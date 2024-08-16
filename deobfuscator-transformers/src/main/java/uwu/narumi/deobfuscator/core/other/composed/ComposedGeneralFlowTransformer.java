package uwu.narumi.deobfuscator.core.other.composed;

import uwu.narumi.deobfuscator.api.transformer.ComposedTransformer;
import uwu.narumi.deobfuscator.api.transformer.Transformer;
import uwu.narumi.deobfuscator.core.other.impl.clean.DeadCodeCleanTransformer;
import uwu.narumi.deobfuscator.core.other.impl.clean.LineNumberCleanTransformer;
import uwu.narumi.deobfuscator.core.other.impl.clean.UnUsedLabelCleanTransformer;
import uwu.narumi.deobfuscator.core.other.impl.pool.InlineLocalVariablesTransformer;
import uwu.narumi.deobfuscator.core.other.impl.pool.InlineStaticFieldTransformer;
import uwu.narumi.deobfuscator.core.other.impl.universal.UniversalFlowTransformer;

import java.util.List;

public class ComposedGeneralFlowTransformer extends ComposedTransformer {
  @Override
  public List<Transformer> transformers() {
    return List.of(
        // Preparation
        new LineNumberCleanTransformer(),
        new UnUsedLabelCleanTransformer(),
        new InlineStaticFieldTransformer(true, true),
        new InlineLocalVariablesTransformer(),

        //new UniversalNumberTransformer(),
        new UniversalFlowTransformer(),
        new DeadCodeCleanTransformer()
    );
  }
}
