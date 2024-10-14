package uwu.narumi.deobfuscator.core.other.impl.universal;

import java.util.List;
import java.util.function.Predicate;

import org.objectweb.asm.tree.AnnotationNode;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

public class AnnotationFilterTransformer extends Transformer {

  private static final Predicate<AnnotationNode> ANNOTATION_NODE_PREDICATE =
      annotationNode ->
          annotationNode.desc == null
              || annotationNode.desc.length() <= 3
              || !annotationNode.desc.startsWith("L")
              || !annotationNode.desc.endsWith(";")
              || annotationNode.desc.contains("\n")
              || annotationNode.desc.contains("\u0000")
              || annotationNode.desc.contains(" ");

  private boolean changed = false;

  @Override
  protected void transform() throws Exception {
    scopedClasses().forEach(classWrapper -> {
      if (classWrapper.classNode().invisibleAnnotations != null)
        changed |= classWrapper
            .classNode()
            .invisibleAnnotations
            .removeIf(ANNOTATION_NODE_PREDICATE);

      if (classWrapper.classNode().visibleAnnotations != null)
        changed |= classWrapper.classNode().visibleAnnotations.removeIf(ANNOTATION_NODE_PREDICATE);

      classWrapper.methods().forEach(methodNode -> {
        if (methodNode.invisibleAnnotations != null)
          changed |= methodNode.invisibleAnnotations.removeIf(ANNOTATION_NODE_PREDICATE);

        if (methodNode.visibleAnnotations != null)
          changed |= methodNode.visibleAnnotations.removeIf(ANNOTATION_NODE_PREDICATE);

        if (methodNode.invisibleParameterAnnotations != null)
          for (List<AnnotationNode> invisibleParameterAnnotation : methodNode.invisibleParameterAnnotations) {
            if (invisibleParameterAnnotation == null) continue;

            changed |= invisibleParameterAnnotation.removeIf(ANNOTATION_NODE_PREDICATE);
          }

        if (methodNode.visibleParameterAnnotations != null)
          for (List<AnnotationNode> visibleParameterAnnotation : methodNode.visibleParameterAnnotations) {
            if (visibleParameterAnnotation == null) continue;

            changed |= visibleParameterAnnotation.removeIf(ANNOTATION_NODE_PREDICATE);
          }
      });

      classWrapper.fields().forEach(fieldNode -> {
        if (fieldNode.invisibleAnnotations != null)
          changed |= fieldNode.invisibleAnnotations.removeIf(ANNOTATION_NODE_PREDICATE);

        if (fieldNode.visibleAnnotations != null)
          changed |= fieldNode.visibleAnnotations.removeIf(ANNOTATION_NODE_PREDICATE);
      });
    });

    if (changed) {
      markChange();
    }
  }
}
