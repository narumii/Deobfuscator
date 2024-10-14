package uwu.narumi.deobfuscator.core.other.impl.clean;

import uwu.narumi.deobfuscator.api.transformer.Transformer;

public class TryCatchBlockCleanTransformer extends Transformer {

  @Override
  protected void transform() throws Exception {
    scopedClasses().stream()
        .flatMap(classWrapper -> classWrapper.methods().stream())
        .forEach(methodNode -> {
          methodNode.tryCatchBlocks = null;
          this.markChange();
        });
  }
}
