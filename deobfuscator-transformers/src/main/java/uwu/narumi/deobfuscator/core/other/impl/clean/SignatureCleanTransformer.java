package uwu.narumi.deobfuscator.core.other.impl.clean;

import uwu.narumi.deobfuscator.api.transformer.Transformer;

public class SignatureCleanTransformer extends Transformer {

  @Override
  protected void transform() throws Exception {
    scopedClasses().forEach(classWrapper -> {
      classWrapper.classNode().signature = null;
      classWrapper.methods().forEach(methodNode -> methodNode.signature = null);
      classWrapper.fields().forEach(fieldNode -> fieldNode.signature = null);
    });

    // There is always a change
    this.markChange();
  }
}
