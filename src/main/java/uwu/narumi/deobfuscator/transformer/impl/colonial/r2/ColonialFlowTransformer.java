package uwu.narumi.deobfuscator.transformer.impl.colonial.r2;

import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.transformer.Transformer;

import java.util.Arrays;

/**
 * @author Szymon on 28.03.2022
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
                                    while (insnNode.getPrevious() != null && insnNode.getPrevious().getPrevious() != null
                                            && insnNode.getOpcode() == DUP
                                            && insnNode.getPrevious().getOpcode() == INVOKEVIRTUAL
                                            && insnNode.getPrevious().getPrevious().getOpcode() == LDC) {
                                        methodNode.instructions.remove(insnNode.getPrevious().getPrevious());
                                        methodNode.instructions.remove(insnNode.getPrevious());
                                        if (insnNode.getNext().getOpcode() == POP2) {
                                            methodNode.instructions.remove(insnNode.getNext());
                                        } else if (insnNode.getNext().getOpcode() == POP) {
                                            methodNode.instructions.remove(insnNode.getNext());
                                            methodNode.instructions.remove(insnNode.getNext());
                                        }
                                        methodNode.instructions.remove(insnNode);
                                    }

                                    //without `anti jd-gui`
                                    //you can use CFR

                                })));
    }
}
