package uwu.narumi.deobfuscator.core.other.impl.clean;

import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.context.Context;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

public class ClassDebugInfoCleanTransformer extends Transformer {

  @Override
  protected boolean transform(ClassWrapper scope, Context context) throws Exception {
    context
        .classes(scope)
        .forEach(
            classWrapper -> {
              classWrapper.getClassNode().sourceDebug = null;
              classWrapper.getClassNode().sourceFile = null;
            });

    // There is always a change
    return true;
  }
}
