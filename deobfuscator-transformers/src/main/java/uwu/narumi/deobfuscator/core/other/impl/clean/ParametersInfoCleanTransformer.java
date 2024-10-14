package uwu.narumi.deobfuscator.core.other.impl.clean;

import uwu.narumi.deobfuscator.api.transformer.Transformer;

/**
 * Removes parameter info from methods
 */
public class ParametersInfoCleanTransformer extends Transformer {
  @Override
  protected void transform() throws Exception {
    scopedClasses().stream()
        .flatMap(classWrapper -> classWrapper.methods().stream())
        .forEach(methodNode -> {
          methodNode.parameters = null;
          this.markChange();
        });
  }
}
