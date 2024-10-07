package uwu.narumi.deobfuscator.core.other.composed;

import uwu.narumi.deobfuscator.api.transformer.ComposedTransformer;
import uwu.narumi.deobfuscator.core.other.composed.general.ComposedPeepholeCleanTransformer;
import uwu.narumi.deobfuscator.core.other.impl.clean.peephole.JsrInlinerTransformer;
import uwu.narumi.deobfuscator.core.other.impl.pool.InlineStaticFieldTransformer;
import uwu.narumi.deobfuscator.core.other.impl.universal.RecoverSyntheticsTransformer;
import uwu.narumi.deobfuscator.core.other.impl.universal.UniversalNumberTransformer;
import uwu.narumi.deobfuscator.core.other.impl.zkm.ZelixLongEncryptionMPCTransformer;
import uwu.narumi.deobfuscator.core.other.impl.zkm.ZelixParametersTransformer;
import uwu.narumi.deobfuscator.core.other.impl.zkm.ZelixUselessTryCatchRemoverTransformer;

import java.util.HashMap;
import java.util.Map;

/**
 * Work in progress
 */
public class ComposedZelixTransformer extends ComposedTransformer {
  public ComposedZelixTransformer() {
    this(false);
  }

  public ComposedZelixTransformer(boolean experimental) {
    this(experimental, new HashMap<>());
  }

  public ComposedZelixTransformer(boolean experimental, Map<String, String> classInitializationOrder) {
    super(
        // Initial cleanup
        JsrInlinerTransformer::new,
        RecoverSyntheticsTransformer::new,

        // Fixes flow a bit
        ZelixUselessTryCatchRemoverTransformer::new,

        // Decompose method parameters
        () -> experimental ? new ZelixParametersTransformer() : null,

        // Decrypt longs
        () -> new ZelixLongEncryptionMPCTransformer(classInitializationOrder),
        InlineStaticFieldTransformer::new,
        UniversalNumberTransformer::new,

        // Cleanup
        ComposedPeepholeCleanTransformer::new
    );
  }
}
