package uwu.narumi.deobfuscator.core.other.composed;

import java.util.Arrays;
import java.util.List;
import uwu.narumi.deobfuscator.api.transformer.ComposedTransformer;
import uwu.narumi.deobfuscator.api.transformer.Transformer;
import uwu.narumi.deobfuscator.core.other.impl.clean.ClassDebugInfoCleanTransformer;
import uwu.narumi.deobfuscator.core.other.impl.clean.MethodDebugInfoCleanTransformer;
import uwu.narumi.deobfuscator.core.other.impl.clean.SignatureCleanTransformer;
import uwu.narumi.deobfuscator.core.other.impl.clean.UnknownAttributeCleanTransformer;
import uwu.narumi.deobfuscator.core.other.impl.universal.AccessRepairTransformer;
import uwu.narumi.deobfuscator.core.other.impl.universal.AnnotationFilterTransformer;
import uwu.narumi.deobfuscator.core.other.impl.universal.TryCatchRepairTransformer;

public class ComposedGeneralRepairTransformer extends ComposedTransformer {

  @Override
  public List<Transformer> transformers() {
    return Arrays.asList(
        new AccessRepairTransformer(),
        new AnnotationFilterTransformer(),
        new TryCatchRepairTransformer(),
        new UnknownAttributeCleanTransformer(),
        new SignatureCleanTransformer(),
        new MethodDebugInfoCleanTransformer(),
        new ClassDebugInfoCleanTransformer());
  }
}
