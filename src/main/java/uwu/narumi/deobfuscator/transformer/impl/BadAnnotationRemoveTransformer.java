package uwu.narumi.deobfuscator.transformer.impl;

import java.util.List;
import java.util.stream.Collectors;
import org.objectweb.asm.tree.AnnotationNode;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.exception.DeobfuscationException;
import uwu.narumi.deobfuscator.transformer.Transformer;

public class BadAnnotationRemoveTransformer implements Transformer {

  @Override
  public void transform(Deobfuscator deobfuscator) throws DeobfuscationException {
    deobfuscator.getClasses().forEach(classNode -> {
      classNode.visibleAnnotations = filterAnnotations(classNode.visibleAnnotations);
      classNode.invisibleAnnotations = filterAnnotations(classNode.invisibleAnnotations);

      classNode.methods.forEach(methodNode -> {
        methodNode.visibleAnnotations = filterAnnotations(methodNode.visibleAnnotations);
        methodNode.invisibleAnnotations = filterAnnotations(methodNode.invisibleAnnotations);
      });

      classNode.fields.forEach(fieldNode -> {
        fieldNode.visibleAnnotations = filterAnnotations(fieldNode.visibleAnnotations);
        fieldNode.invisibleAnnotations = filterAnnotations(fieldNode.invisibleAnnotations);
      });
    });
  }

  //Unix, nie wysilaj sie bo i tak ci sie nie uda :(
  private List<AnnotationNode> filterAnnotations(List<AnnotationNode> nodes) {
    if (nodes == null) {
      return null;
    }

    List<String> invalid = nodes.stream()
        .filter(node -> !node.desc.startsWith("L"))
        .filter(node -> !node.desc.endsWith(";"))
        .map(node -> node.desc)
        .collect(Collectors.toList());

    return nodes.stream()
        .filter(
            node -> !invalid.contains(node.desc.replace(";", "").replace("@", "").replace("L", "")))
        .collect(Collectors.toList());
  }

  @Override
  public int weight() {
    return 0;
  }

  @Override
  public String name() {
    return "Bad Annotation Remover";
  }
}
