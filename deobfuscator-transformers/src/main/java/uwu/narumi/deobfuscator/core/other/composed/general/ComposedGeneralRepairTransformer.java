package uwu.narumi.deobfuscator.core.other.composed.general;

import uwu.narumi.deobfuscator.api.transformer.ComposedTransformer;
import uwu.narumi.deobfuscator.core.other.impl.clean.ClassDebugInfoCleanTransformer;
import uwu.narumi.deobfuscator.core.other.impl.clean.MethodDebugInfoCleanTransformer;
import uwu.narumi.deobfuscator.core.other.impl.clean.SignatureCleanTransformer;
import uwu.narumi.deobfuscator.core.other.impl.clean.UnknownAttributeCleanTransformer;
import uwu.narumi.deobfuscator.core.other.impl.universal.AccessRepairTransformer;
import uwu.narumi.deobfuscator.core.other.impl.universal.AnnotationFilterTransformer;
import uwu.narumi.deobfuscator.core.other.impl.universal.TryCatchRepairTransformer;

/**
 * If methods are hidden from decompiler, or something uncommon happen, then this transformer will try to fix it.
 */
public class ComposedGeneralRepairTransformer extends ComposedTransformer {

  public ComposedGeneralRepairTransformer() {
    super(
        AccessRepairTransformer::new,
        AnnotationFilterTransformer::new,
        TryCatchRepairTransformer::new,
        UnknownAttributeCleanTransformer::new,
        SignatureCleanTransformer::new,
        MethodDebugInfoCleanTransformer::new,
        ClassDebugInfoCleanTransformer::new
    );
  }
}
