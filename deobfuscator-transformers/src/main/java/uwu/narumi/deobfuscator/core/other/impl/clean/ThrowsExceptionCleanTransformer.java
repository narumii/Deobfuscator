package uwu.narumi.deobfuscator.core.other.impl.clean;

import uwu.narumi.deobfuscator.api.transformer.Transformer;

/**
 * Removes "throws Exception" from methods.
 */
public class ThrowsExceptionCleanTransformer extends Transformer {
  @Override
  protected void transform() throws Exception {
    scopedClasses().stream()
        .flatMap(classWrapper -> classWrapper.methods().stream())
        .forEach(methodNode -> {
          methodNode.exceptions = null;
          this.markChange();
        });
  }
}
