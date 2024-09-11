package uwu.narumi.deobfuscator.core.other.composed;

import uwu.narumi.deobfuscator.api.transformer.ComposedTransformer;
import uwu.narumi.deobfuscator.core.other.impl.clean.peephole.JsrInlinerTransformer;
import uwu.narumi.deobfuscator.core.other.impl.clean.peephole.PopUnUsedLocalVariablesTransformer;
import uwu.narumi.deobfuscator.core.other.impl.clean.peephole.UselessPopCleanTransformer;
import uwu.narumi.deobfuscator.core.other.impl.pool.InlineStaticFieldTransformer;
import uwu.narumi.deobfuscator.core.other.impl.universal.RecoverSyntheticsTransformer;
import uwu.narumi.deobfuscator.core.other.impl.universal.UniversalNumberTransformer;
import uwu.narumi.deobfuscator.core.other.impl.zkm.ZelixLongEncryptionTransformer;
import uwu.narumi.deobfuscator.core.other.impl.zkm.ZelixUselessTryCatchRemoverTransformer;

/**
 * Work in progress
 */
public class ComposedZelixTransformer extends ComposedTransformer {
  public ComposedZelixTransformer() {
    super(
        JsrInlinerTransformer::new,
        RecoverSyntheticsTransformer::new,

        // Fixes flow a bit
        ZelixUselessTryCatchRemoverTransformer::new,

        ZelixLongEncryptionTransformer::new,
        InlineStaticFieldTransformer::new,
        UniversalNumberTransformer::new,

        // Cleanup
        PopUnUsedLocalVariablesTransformer::new,
        UselessPopCleanTransformer::new
    );
  }
}
