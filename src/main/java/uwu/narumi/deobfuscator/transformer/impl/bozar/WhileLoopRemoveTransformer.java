package uwu.narumi.deobfuscator.transformer.impl.bozar;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.transformer.Transformer;

import java.util.Arrays;

public class WhileLoopRemoveTransformer extends Transformer {

    /*
    Loop remover
     */
    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        deobfuscator.classes().stream()
                .flatMap(classNode -> classNode.methods.stream())
                .forEach(methodNode -> Arrays.stream(methodNode.instructions.toArray())
                        .filter(node -> node.getOpcode() == ICONST_1)
                        .filter(node -> node.getNext().getOpcode() == GOTO)
                        .filter(node -> node.getNext().getNext().getOpcode() == -1)
                        .filter(node -> node.getNext().getNext().getNext().getOpcode() == ICONST_5)
                        .filter(node -> node.getNext().getNext().getNext().getNext().getOpcode() == -1)
                        .filter(node -> node.getNext().getNext().getNext().getNext().getNext().getOpcode() == ICONST_M1)
                        .forEach(node -> {
                            try {
                                LabelNode labelNode = ((LabelNode) node.getNext().getNext());
                                AbstractInsnNode end = node.getNext().getNext().getNext().getNext().getNext();

                                while (!(end instanceof JumpInsnNode && ((JumpInsnNode) end).label.equals(labelNode)))
                                    end = end.getNext();

                                getInstructionsBetween(node, end).forEach(a -> methodNode.instructions.remove(a));
                            } catch (Exception ignored) {
                            }
                        })
                );
    }
}
