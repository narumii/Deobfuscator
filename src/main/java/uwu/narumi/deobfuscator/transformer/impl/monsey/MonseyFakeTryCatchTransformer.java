package uwu.narumi.deobfuscator.transformer.impl.monsey;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.transformer.Transformer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MonseyFakeTryCatchTransformer extends Transformer {

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        deobfuscator.classes().stream()
                .flatMap(classNode -> classNode.methods.stream())
                .forEach(methodNode -> {
                    List<TryCatchBlockNode> toRemove = new ArrayList<>();

                    methodNode.tryCatchBlocks.stream()
                            .filter(node -> node.type != null)
                            .filter(node -> node.type.equals("java/lang/Throwable"))
                            .forEach(tbce -> {
                                int handlerIndex = methodNode.instructions.indexOf(tbce.handler);
                                AbstractInsnNode handler = methodNode.instructions.get(handlerIndex);

                                if (check(handler.getNext(), ATHROW)) {
                                    toRemove.add(tbce);
                                }
                            });

                    methodNode.tryCatchBlocks.removeAll(toRemove);
                    toRemove.clear();

                    Arrays.stream(methodNode.instructions.toArray())
                            .filter(node -> node.getOpcode() == GOTO)
                            .filter(node -> node.getNext() != null)
                            .forEach(node -> {
                                if (node.getNext().getOpcode() == ATHROW) {
                                    methodNode.instructions.remove(node.getNext());
                                    methodNode.instructions.remove(node);
                                } else if (node.getNext() instanceof LabelNode && node.getNext().getNext() != null && node.getNext().getNext().getOpcode() == ATHROW) {
                                    methodNode.instructions.remove(node.getNext().getNext());
                                    methodNode.instructions.remove(node);
                                }
                            });
                });
    }
}
