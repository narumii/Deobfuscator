package uwu.narumi.deobfuscator.core.other.impl.clean;

import uwu.narumi.deobfuscator.api.transformer.Transformer;

public class AnnotationCleanTransformer extends Transformer {

  @Override
  protected void transform() throws Exception {
    scopedClasses().forEach(classWrapper -> {
      classWrapper.classNode().invisibleAnnotations = null;
      classWrapper.classNode().invisibleAnnotations = null;

      classWrapper.methods().forEach(methodNode -> {
        methodNode.invisibleAnnotations = null;
        methodNode.visibleAnnotations = null;
        methodNode.invisibleParameterAnnotations = null;
        methodNode.visibleParameterAnnotations = null;
      });

      classWrapper.fields().forEach(fieldNode -> {
        fieldNode.invisibleAnnotations = null;
        fieldNode.visibleAnnotations = null;
      });
    });

    // There is always a change
    markChange();
  }
}
