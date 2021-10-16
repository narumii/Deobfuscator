package uwu.narumi.deobfuscator.transformer.impl.universal.remove;

import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.transformer.Transformer;

import java.util.Arrays;

//Bruh
public class OkThrowRemoveTransformer extends Transformer {

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        deobfuscator.classes().stream()
                .flatMap(classNode -> classNode.methods.stream())
                .forEach(methodNode -> Arrays.stream(methodNode.instructions.toArray())
                        .filter(node -> node.getOpcode() == ATHROW)
                        .forEach(node -> {
                            if (node.getPrevious().getOpcode() == ATHROW) {
                                methodNode.instructions.remove(node.getPrevious());
                                methodNode.instructions.remove(node);
                            } else if (node.getPrevious().getOpcode() == NOP) {
                                methodNode.instructions.remove(node.getPrevious());
                                methodNode.instructions.remove(node);
                            } else if (node.getOpcode() >= IRETURN && node.getOpcode() <= RETURN) {
                                methodNode.instructions.remove(node);
                            }
                        })
                );
    }
}
