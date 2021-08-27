package uwu.narumi.deobfuscator.transformer.impl.universal.remove;

import org.objectweb.asm.tree.AnnotationNode;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.transformer.Transformer;

import java.util.List;
import java.util.stream.Collectors;

public class InvalidAnnotationRemoveTransformer extends Transformer {

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        deobfuscator.classes().forEach(classNode -> {
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

    private List<AnnotationNode> filterAnnotations(List<AnnotationNode> nodes) {
        if (nodes == null) {
            return null;
        }

        return nodes.stream()
                .filter(node -> node.desc.startsWith("L"))
                .filter(node -> node.desc.endsWith(";"))
                .filter(node -> node.desc.length() >= 3)
                .filter(node -> !node.desc.contains("\n"))
                .filter(node -> !node.desc.contains(" "))
                .filter(node -> !node.desc.contains("\u0000"))
                .collect(Collectors.toList());
    }
}
