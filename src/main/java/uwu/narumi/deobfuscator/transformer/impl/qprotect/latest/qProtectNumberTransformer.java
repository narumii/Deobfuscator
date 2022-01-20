package uwu.narumi.deobfuscator.transformer.impl.qprotect.latest;

import org.objectweb.asm.tree.VarInsnNode;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.helper.ASMHelper;
import uwu.narumi.deobfuscator.transformer.Transformer;
import uwu.narumi.deobfuscator.transformer.impl.binsecure.BinsecureNumberTransformer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class qProtectNumberTransformer extends Transformer {

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        deobfuscator.classes().stream()
                .flatMap(classNode -> classNode.methods.stream())
                .forEach(methodNode -> {
                    Map<Integer, Integer> values = new HashMap<>();

                    Arrays.stream(methodNode.instructions.toArray())
                            .filter(ASMHelper::isInteger)
                            .filter(node -> node.getNext() instanceof VarInsnNode)
                            .filter(node -> node.getNext().getOpcode() == ISTORE)
                            .forEach(node -> {
                                values.put(((VarInsnNode) node.getNext()).var, getInteger(node));

                                methodNode.instructions.remove(node.getNext());
                                methodNode.instructions.remove(node);
                            });

                    Arrays.stream(methodNode.instructions.toArray())
                            .filter(node -> node instanceof VarInsnNode)
                            .filter(node -> node.getOpcode() == ILOAD)
                            .map(VarInsnNode.class::cast)
                            .filter(node -> values.containsKey(node.var))
                            .forEach(node -> methodNode.instructions.set(node, getNumber(values.get(node.var))));
                });

        new BinsecureNumberTransformer().transform(deobfuscator); //YES
    }
}
