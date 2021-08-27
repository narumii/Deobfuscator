package uwu.narumi.deobfuscator.transformer.impl.universal.remove;

import org.objectweb.asm.tree.LineNumberNode;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.transformer.Transformer;

import java.util.Arrays;

public class LineNumberRemoveTransformer extends Transformer {

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        deobfuscator.classes().stream()
                .flatMap(classNode -> classNode.methods.stream())
                .forEach(methodNode -> Arrays.stream(methodNode.instructions.toArray())
                        .filter(node -> node instanceof LineNumberNode)
                        .forEach(methodNode.instructions::remove)
                );
    }
}
