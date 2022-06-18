package uwu.narumi.deobfuscator.transformer.impl.bozoriusz;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LabelNode;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.helper.ASMHelper;
import uwu.narumi.deobfuscator.transformer.Transformer;

import java.util.Arrays;

public class BozoriuszConstantFlowTransformer extends Transformer {

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        deobfuscator.classes().stream()
                .flatMap(classNode -> classNode.methods.stream())
                .forEach(methodNode -> Arrays.stream(methodNode.instructions.toArray())
                        .filter(ASMHelper::isNumber)
                        .filter(node -> node.getNext() instanceof LabelNode)
                        .filter(node -> node.getPrevious() instanceof LabelNode)
                        .filter(node -> node.getPrevious().getPrevious().getOpcode() == GOTO)
                        .filter(node -> isNumber(node.getPrevious().getPrevious().getPrevious()))
                        .forEach(node -> {
                            int index = methodNode.instructions.indexOf(node);
                            if (index - 10 < 0 || methodNode.instructions.size() < index + 9)
                                return;

                            AbstractInsnNode beforeStart = methodNode.instructions.get(index - 10);
                            AbstractInsnNode beforeEnd = node.getPrevious(); //methodNode.instructions.get(index - 1); //node.getPrevious()

                            AbstractInsnNode afterStart = node.getNext(); //methodNode.instructions.get(index + 1); //node.getNext()
                            AbstractInsnNode afterEnd = methodNode.instructions.get(index + 9);

                            if (isLong(beforeStart) && afterEnd instanceof LabelNode) {
                                getInstructionsBetween(
                                        beforeStart,
                                        beforeEnd,
                                        true,
                                        true
                                ).forEach(methodNode.instructions::remove);

                                getInstructionsBetween(
                                        afterStart,
                                        afterEnd,
                                        true,
                                        false
                                ).forEach(methodNode.instructions::remove);
                            }
                        }));
    }
}
