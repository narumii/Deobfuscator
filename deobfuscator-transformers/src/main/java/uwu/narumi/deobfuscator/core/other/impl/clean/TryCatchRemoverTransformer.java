package uwu.narumi.deobfuscator.core.other.impl.clean;

import uwu.narumi.deobfuscator.api.transformer.Transformer;

/**
 * Removes all try-catch blocks from all methods.
 */
public class TryCatchRemoverTransformer extends Transformer {
  @Override
  protected void transform() throws Exception {
    scopedClasses().forEach(classWrapper -> classWrapper.methods().forEach(methodNode -> {
      methodNode.tryCatchBlocks.clear();
      markChange();
    }));
  }
}
