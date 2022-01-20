package uwu.narumi.deobfuscator.transformer.impl.cheatbreaker;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.LdcInsnNode;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.transformer.Transformer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class CheatBreakerStaticArrayStringPoolTransformer extends Transformer {

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        ClassNode holderClass = deobfuscator.classes().stream()
                .filter(classNode -> classNode.methods.size() == 1)
                .filter(classNode -> classNode.fields.size() == 1)
                .filter(classNode -> classNode.methods.get(0).name.equals("<clinit>"))
                .filter(classNode -> classNode.fields.get(0).desc.equals("[Ljava/lang/String;"))
                .findFirst()
                .orElseThrow();

        FieldNode fieldNode = holderClass.fields.get(0);
        Map<Integer, String> strings = new HashMap<>();
        findClInit(holderClass).ifPresent(methodNode -> Arrays.stream(methodNode.instructions.toArray())
                .filter(insn -> insn.getOpcode() == AASTORE)
                .filter(insn -> isString(insn.getPrevious()))
                .filter(insn -> isInteger(insn.getPrevious().getPrevious()))
                .forEach(insn -> strings.put(getInteger(insn.getPrevious().getPrevious()), getString(insn.getPrevious()))));

        if (strings.isEmpty())
            return;

        deobfuscator.classes().stream()
                .flatMap(classNode -> classNode.methods.stream())
                .forEach(methodNode -> Arrays.stream(methodNode.instructions.toArray())
                        .filter(node -> node instanceof FieldInsnNode)
                        .filter(node -> isInteger(node.getNext()))
                        .filter(node -> node.getNext().getNext().getOpcode() == AALOAD)
                        .map(FieldInsnNode.class::cast)
                        .filter(node -> node.owner.equals(holderClass.name))
                        .filter(node -> node.desc.equals(fieldNode.desc))
                        .forEach(node -> {
                            int position = getInteger(node.getNext());
                            if (!strings.containsKey(position))
                                return;

                            methodNode.instructions.remove(node.getNext().getNext());
                            methodNode.instructions.remove(node.getNext());
                            methodNode.instructions.set(node, new LdcInsnNode(strings.get(position)));
                        }));

        strings.clear();
        deobfuscator.getClasses().remove(holderClass.name);
    }
}