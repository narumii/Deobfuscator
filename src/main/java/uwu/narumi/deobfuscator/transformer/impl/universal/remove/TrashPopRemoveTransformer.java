package uwu.narumi.deobfuscator.transformer.impl.universal.remove;

import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.transformer.Transformer;

import java.util.Arrays;

public class TrashPopRemoveTransformer extends Transformer {

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        deobfuscator.classes().stream()
                .flatMap(classNode -> classNode.methods.stream())
                .forEach(methodNode -> Arrays.stream(methodNode.instructions.toArray())
                        .filter(node -> node.getOpcode() == BIPUSH)
                        .filter(node -> node.getNext().getOpcode() == POP)
                        .forEach(node -> {
                            methodNode.instructions.remove(node.getNext());
                            methodNode.instructions.remove(node);
                        })
                );
    }
}
