package uwu.narumi.deobfuscator.transformer.impl.colonial.old;

import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.transformer.Transformer;

import java.util.Arrays;

/**
 * @author Szymon on 02.02.2022
 * @project Deobfuscator
 */
public class ColonialFlowTransformer extends Transformer {

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        deobfuscator.classes()
                .forEach(classNode -> classNode.methods.stream()
                        .filter(methodNode -> !methodNode.name.startsWith("<"))
                        .forEach(methodNode -> Arrays.stream(methodNode.instructions.toArray())
                                .filter(insnNode -> insnNode.getOpcode() == DUP || insnNode.getOpcode() == POP || insnNode.getOpcode() == SWAP || insnNode.getOpcode() == FSUB || insnNode.getOpcode() == ISUB || insnNode.getOpcode() == DSUB || insnNode.getOpcode() == ATHROW)
                                .forEach(insnNode -> {
                                    while (insnNode.getPrevious().getOpcode() == POP
                                            && insnNode.getPrevious().getPrevious().getOpcode() == INVOKEVIRTUAL
                                            && isString(insnNode.getPrevious().getPrevious().getPrevious())) {
                                        methodNode.instructions.remove(insnNode.getPrevious().getPrevious().getPrevious());
                                        methodNode.instructions.remove(insnNode.getPrevious().getPrevious());
                                        methodNode.instructions.remove(insnNode.getPrevious());
                                    }
                                    if (insnNode.getOpcode() == DUP
                                            && insnNode.getNext().getOpcode() == POP2
                                            && isString(insnNode.getNext().getNext())
                                            && insnNode.getNext().getNext().getNext().getOpcode() == POP
                                            && insnNode.getNext().getNext().getNext().getNext().getOpcode() == SWAP
                                            && insnNode.getNext().getNext().getNext().getNext().getNext().getOpcode() == POP
                                            && isString(insnNode.getNext().getNext().getNext().getNext().getNext().getNext())
                                            && isString(insnNode.getNext().getNext().getNext().getNext().getNext().getNext().getNext())
                                            && isString(insnNode.getNext().getNext().getNext().getNext().getNext().getNext().getNext().getNext())) {
                                        getInstructionsBetween(insnNode.getNext().getNext().getNext().getNext().getNext().getNext().getNext().getNext(), insnNode.getNext())
                                                .forEach(methodNode.instructions::remove);
                                    }
                                })));
    }
}
