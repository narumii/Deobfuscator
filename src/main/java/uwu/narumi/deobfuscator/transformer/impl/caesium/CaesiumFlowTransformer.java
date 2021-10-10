package uwu.narumi.deobfuscator.transformer.impl.caesium;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.transformer.Transformer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CaesiumFlowTransformer extends Transformer {

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        List<FieldInsnNode> toRemove = new ArrayList<>();

        deobfuscator.classes().stream()
                .flatMap(classNode -> classNode.methods.stream())
                .forEach(methodNode -> Arrays.stream(methodNode.instructions.toArray())
                        .filter(node -> check(node, GETSTATIC))
                        .filter(node -> check(node.getNext(), JumpInsnNode.class))
                        .filter(node -> check(node.getNext().getNext(), ACONST_NULL))
                        .filter(node -> check(node.getNext().getNext().getNext(), ATHROW))
                        .forEach(node -> {
                            toRemove.add((FieldInsnNode) node);

                            methodNode.instructions.remove(node.getNext().getNext().getNext());
                            methodNode.instructions.remove(node.getNext().getNext());
                            methodNode.instructions.set(node, new JumpInsnNode(GOTO, ((JumpInsnNode) node.getNext()).label));
                        }));

        toRemove.forEach(fieldInsnNode -> {
            ClassNode classNode = deobfuscator.getClasses().get(fieldInsnNode.owner);
            if (classNode == null)
                return;

            classNode.fields.removeIf(fieldNode -> fieldNode.name.equals(fieldInsnNode.name) && fieldNode.desc.equals(fieldInsnNode.desc));
        });
        toRemove.clear();
    }
}
