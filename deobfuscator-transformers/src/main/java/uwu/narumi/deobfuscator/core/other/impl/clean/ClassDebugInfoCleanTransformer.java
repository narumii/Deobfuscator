package uwu.narumi.deobfuscator.core.other.impl.clean;

import uwu.narumi.deobfuscator.api.transformer.Transformer;

public class ClassDebugInfoCleanTransformer extends Transformer {

  @Override
  protected void transform() throws Exception {
    scopedClasses().forEach(classWrapper -> {
      classWrapper.classNode().sourceDebug = null;
      classWrapper.classNode().sourceFile = null;
    });

    // There is always a change
    markChange();
  }
}
