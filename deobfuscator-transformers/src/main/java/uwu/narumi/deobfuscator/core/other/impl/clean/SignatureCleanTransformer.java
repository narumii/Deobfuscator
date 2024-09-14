package uwu.narumi.deobfuscator.core.other.impl.clean;

import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.context.Context;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

public class SignatureCleanTransformer extends Transformer {

  @Override
  protected void transform(ClassWrapper scope, Context context) throws Exception {
    context
        .classes(scope)
        .forEach(
            classWrapper -> {
              classWrapper.classNode().signature = null;
              classWrapper.methods().forEach(methodNode -> methodNode.signature = null);
              classWrapper.fields().forEach(fieldNode -> fieldNode.signature = null);
            });

    // There is always a change
    this.markChange();
  }
}
