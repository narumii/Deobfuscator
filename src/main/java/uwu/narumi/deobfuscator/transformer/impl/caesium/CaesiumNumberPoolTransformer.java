package uwu.narumi.deobfuscator.transformer.impl.caesium;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.SourceValue;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.transformer.Transformer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class CaesiumNumberPoolTransformer extends Transformer {

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        deobfuscator.classes().forEach(classNode -> {
            Map<String, Number> numbers = new HashMap<>();

            findClInit(classNode).ifPresent(methodNode -> {
                Map<AbstractInsnNode, Frame<SourceValue>> frames = analyzeSource(classNode, methodNode);

                Arrays.stream(methodNode.instructions.toArray())
                        .filter(node -> node instanceof FieldInsnNode)
                        .filter(node -> node.getOpcode() == PUTSTATIC)
                        .map(FieldInsnNode.class::cast)
                        .filter(node -> node.desc.equals("I") || node.desc.equals("J") || node.desc.equals("D") || node.desc.equals("F"))
                        .forEach(node -> {
                            AbstractInsnNode numberNode = null;
                            if (isNumber(node.getPrevious())) {
                                numberNode = node.getPrevious();
                            } else if (frames != null) {
                                Frame<SourceValue> frame = frames.get(node);
                                SourceValue value = frame.getStack(frame.getStackSize() - 1);
                                if (value == null || value.insns == null || value.insns.isEmpty())
                                    return;

                                AbstractInsnNode stackInsn = value.insns.iterator().next();
                                if (isNumber(stackInsn))
                                    numberNode = stackInsn;
                            }

                            if (numberNode != null) {
                                numbers.put(node.owner + "\u0000" + node.name + "\u0000" + node.desc, getNumber(numberNode));

                                methodNode.instructions.remove(numberNode);
                                methodNode.instructions.remove(node);
                            }
                        });
            });

            classNode.methods.forEach(methodNode -> Arrays.stream(methodNode.instructions.toArray())
                    .filter(node -> node instanceof FieldInsnNode)
                    .filter(node -> node.getOpcode() == GETSTATIC)
                    .map(FieldInsnNode.class::cast)
                    .filter(node -> node.desc.equals("I") || node.desc.equals("J") || node.desc.equals("D") || node.desc.equals("F"))
                    .forEach(node -> {
                        String key = node.owner + "\u0000" + node.name + "\u0000" + node.desc;
                        if (!numbers.containsKey(key))
                            return;

                        methodNode.instructions.set(node, getNumber(numbers.get(key)));
                    }));


            numbers.keySet().forEach(info -> {
                String[] parts = info.split("\u0000");
                classNode.fields.removeIf(fieldNode -> fieldNode.name.equals(parts[1]) && fieldNode.desc.equals(parts[2]));
            });
            numbers.clear();
        });
    }
}
