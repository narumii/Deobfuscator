package uwu.narumi.deobfuscator.transformer.impl.universal.other;

import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.transformer.Transformer;

import java.util.ArrayList;
import java.util.List;

public class TryCatchFixTransformer extends Transformer {

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        deobfuscator.classes().stream()
                .flatMap(classNode -> classNode.methods.stream())
                .forEach(methodNode -> {
                    methodNode.tryCatchBlocks.removeIf(tbce -> tbce.type == null || tbce.type.isBlank() || tbce.type.isEmpty());
                    methodNode.tryCatchBlocks.removeIf(tbce -> tbce.start.equals(tbce.end) || tbce.start.equals(tbce.handler) || tbce.end.equals(tbce.handler));

                    List<TryCatchBlockNode> toRemove = new ArrayList<>();
                    methodNode.tryCatchBlocks.forEach(tbce -> {
                        LabelNode start = tbce.start;
                        LabelNode handler = tbce.handler;
                        LabelNode end = tbce.end;

                        if (methodNode.instructions.indexOf(start) == -1 || methodNode.instructions.indexOf(handler) == -1 || methodNode.instructions.indexOf(end) == -1)
                            toRemove.add(tbce);
                        else if (end.getNext() != null && end.getNext().getNext() != null && end.getNext().getOpcode() == ACONST_NULL && end.getNext().getNext().getOpcode() == ATHROW)
                            toRemove.add(tbce);
                        else if (methodNode.instructions.indexOf(start) >= methodNode.instructions.indexOf(handler) || methodNode.instructions.indexOf(start) >= methodNode.instructions.indexOf(end) || methodNode.instructions.indexOf(handler) <= methodNode.instructions.indexOf(end))
                            toRemove.add(tbce);
                    });

                    methodNode.tryCatchBlocks.removeAll(toRemove);
                });
    }
}
