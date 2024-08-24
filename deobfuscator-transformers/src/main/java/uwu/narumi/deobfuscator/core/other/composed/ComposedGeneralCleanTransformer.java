package uwu.narumi.deobfuscator.core.other.composed;

import uwu.narumi.deobfuscator.api.transformer.ComposedTransformer;
import uwu.narumi.deobfuscator.core.other.impl.clean.*;
import uwu.narumi.deobfuscator.core.other.impl.clean.peephole.NopCleanTransformer;
import uwu.narumi.deobfuscator.core.other.impl.clean.peephole.UnUsedLabelCleanTransformer;
import uwu.narumi.deobfuscator.core.other.impl.universal.StackOperationResolveTransformer;
import uwu.narumi.deobfuscator.core.other.impl.universal.TryCatchRepairTransformer;

public class ComposedGeneralCleanTransformer extends ComposedTransformer {

  public ComposedGeneralCleanTransformer() {
    super(
        AnnotationCleanTransformer::new,
        ClassDebugInfoCleanTransformer::new,
        LineNumberCleanTransformer::new,
        MethodDebugInfoCleanTransformer::new,
        NopCleanTransformer::new,
        StackOperationResolveTransformer::new,
        TryCatchRepairTransformer::new,
        UnknownAttributeCleanTransformer::new,
        UnUsedLabelCleanTransformer::new
    );
  }
}
