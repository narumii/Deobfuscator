package uwu.narumi.deobfuscator.transformer.impl.colonial;

import org.objectweb.asm.tree.LdcInsnNode;
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
                .forEach(classNode ->
                        classNode.methods.stream()
                                .filter(methodNode -> !methodNode.name.startsWith("<"))
                                .forEach(methodNode ->
                                        Arrays.stream(methodNode.instructions.toArray())
                                                .forEach(insnNode -> {
                                                    if (insnNode.getOpcode() == DUP
                                                            || insnNode.getOpcode() == POP
                                                            || insnNode.getOpcode() == SWAP
                                                            || insnNode.getOpcode() == FSUB
                                                            || insnNode.getOpcode() == ISUB
                                                            || insnNode.getOpcode() == DSUB
                                                            || insnNode.getOpcode() == ATHROW) {
                                                        while (insnNode.getPrevious().getOpcode() == POP
                                                                && insnNode.getPrevious().getPrevious().getOpcode() == INVOKEVIRTUAL
                                                                && insnNode.getPrevious().getPrevious().getPrevious() instanceof LdcInsnNode) {
                                                            methodNode.instructions.remove(insnNode.getPrevious().getPrevious().getPrevious());
                                                            methodNode.instructions.remove(insnNode.getPrevious().getPrevious());
                                                            methodNode.instructions.remove(insnNode.getPrevious());
                                                        }
                                                    } else if (insnNode.getOpcode() == DUP //xd
                                                            && insnNode.getNext().getOpcode() == POP2
                                                            && insnNode.getNext().getNext() instanceof LdcInsnNode
                                                            && insnNode.getNext().getNext().getNext().getOpcode() == POP
                                                            && insnNode.getNext().getNext().getNext().getNext().getOpcode() == SWAP
                                                            && insnNode.getNext().getNext().getNext().getNext().getNext().getOpcode() == POP
                                                            && insnNode.getNext().getNext().getNext().getNext().getNext().getNext() instanceof LdcInsnNode
                                                            && insnNode.getNext().getNext().getNext().getNext().getNext().getNext().getNext() instanceof LdcInsnNode
                                                            && insnNode.getNext().getNext().getNext().getNext().getNext().getNext().getNext().getNext() instanceof LdcInsnNode) {
                                                        methodNode.instructions.remove(insnNode.getNext().getNext().getNext().getNext().getNext().getNext().getNext().getNext());
                                                        methodNode.instructions.remove(insnNode.getNext().getNext().getNext().getNext().getNext().getNext().getNext());
                                                        methodNode.instructions.remove(insnNode.getNext().getNext().getNext().getNext().getNext().getNext());
                                                        methodNode.instructions.remove(insnNode.getNext().getNext().getNext().getNext().getNext());
                                                        methodNode.instructions.remove(insnNode.getNext().getNext().getNext().getNext());
                                                        methodNode.instructions.remove(insnNode.getNext().getNext().getNext());
                                                        methodNode.instructions.remove(insnNode.getNext().getNext());
                                                        methodNode.instructions.remove(insnNode.getNext());
                                                    }
                                                })));
    }
}
