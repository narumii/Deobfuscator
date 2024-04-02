package uwu.narumi.deobfuscator.core.other.composed;

import java.util.Arrays;
import java.util.List;
import uwu.narumi.deobfuscator.api.transformer.ComposedTransformer;
import uwu.narumi.deobfuscator.api.transformer.Transformer;
import uwu.narumi.deobfuscator.core.other.impl.clean.*;
import uwu.narumi.deobfuscator.core.other.impl.universal.StackOperationResolveTransformer;
import uwu.narumi.deobfuscator.core.other.impl.universal.TryCatchRepairTransformer;

public class ComposedGeneralCleanTransformer extends ComposedTransformer {

  @Override
  public List<Transformer> transformers() {
    return Arrays.asList(
        new AnnotationCleanTransformer(),
        new ClassDebugInfoCleanTransformer(),
        new LineNumberCleanTransformer(),
        new MethodDebugInfoCleanTransformer(),
        new NopCleanTransformer(),
        new StackOperationResolveTransformer(),
        new TryCatchRepairTransformer(),
        new UnknownAttributeCleanTransformer(),
        new UnUsedLabelCleanTransformer());
  }
}
