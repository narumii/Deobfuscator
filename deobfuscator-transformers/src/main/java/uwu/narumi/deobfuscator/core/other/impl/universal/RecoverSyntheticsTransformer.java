package uwu.narumi.deobfuscator.core.other.impl.universal;

import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.context.Context;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

public class RecoverSyntheticsTransformer extends Transformer {
  @Override
  protected void transform(ClassWrapper scope, Context context) throws Exception {
    context.classes(scope).forEach(classWrapper -> {
      classWrapper.methods().forEach(methodNode -> {
        // Recover by name
        if (methodNode.name.startsWith("lambda$")) {
          // Mark as synthetic
          methodNode.access |= ACC_SYNTHETIC;
          markChange();
        }
      });
    });
  }
}
