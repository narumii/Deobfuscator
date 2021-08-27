package uwu.narumi.deobfuscator.transformer.impl.bozar;

import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.transformer.Transformer;

import java.util.Arrays;

public class CleanTransformer extends Transformer {

    /*
    Some remains remover
     */
    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        deobfuscator.classes().stream()
                .flatMap(classNode -> classNode.methods.stream())
                .forEach(methodNode -> Arrays.stream(methodNode.instructions.toArray())
                        .filter(node -> node.getOpcode() == ICONST_1)
                        .filter(node -> node.getNext().getOpcode() == GOTO)
                        .filter(node -> node.getNext().getNext().getNext().getOpcode() == ICONST_5)
                        .filter(node -> node.getNext().getNext().getNext().getNext().getOpcode() == -1)
                        .filter(node -> node.getNext().getNext().getNext().getNext().getNext().getOpcode() == ICONST_M1)
                        .forEach(node -> getInstructionsBetween(node,
                                node.getNext().getNext().getNext().getNext().getNext()).forEach(a -> methodNode.instructions.remove(a)))
                );
    }
}
