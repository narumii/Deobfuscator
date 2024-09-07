package uwu.narumi.deobfuscator.core.other.composed;

import uwu.narumi.deobfuscator.api.transformer.ComposedTransformer;
import uwu.narumi.deobfuscator.core.other.impl.clean.peephole.JsrInlinerTransformer;
import uwu.narumi.deobfuscator.core.other.impl.zkm.ZelixLongEncryptionTransformer;

/**
 * Work in progress
 */
public class ComposedZelixTransformer extends ComposedTransformer {
  public ComposedZelixTransformer() {
    super(
        JsrInlinerTransformer::new,
        ZelixLongEncryptionTransformer::new
    );
  }
}
