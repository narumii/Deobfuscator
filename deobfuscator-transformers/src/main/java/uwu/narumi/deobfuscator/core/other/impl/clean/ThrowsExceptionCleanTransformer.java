package uwu.narumi.deobfuscator.core.other.impl.clean;

import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.context.Context;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

/**
 * Removes "throws Exception" from methods.
 */
public class ThrowsExceptionCleanTransformer extends Transformer {
  @Override
  protected void transform(ClassWrapper scope, Context context) throws Exception {
    context.classes(scope).stream()
        .flatMap(classWrapper -> classWrapper.methods().stream())
        .forEach(methodNode -> {
          methodNode.exceptions = null;
          this.markChange();
        });
  }
}
