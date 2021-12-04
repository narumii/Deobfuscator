package uwu.narumi.deobfuscator.transformer.impl.qprotect.latest;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.transformer.Transformer;

import java.util.ArrayList;
import java.util.List;

public class qProtectFieldFlowTransformer extends Transformer {

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        deobfuscator.classes().forEach(classNode -> {
            List<String> fields = new ArrayList<>();

            classNode.methods.forEach(methodNode -> {
                boolean modified;

                do {
                    modified = false;

                    for (AbstractInsnNode node : methodNode.instructions.toArray()) {
                        if ((node instanceof FieldInsnNode
                                && node.getOpcode() == GETSTATIC
                                && ((FieldInsnNode) node).desc.equals("Z"))
                                && node.getNext() instanceof JumpInsnNode
                                && node.getNext().getNext().getOpcode() == ACONST_NULL
                            //&& node.getNext().getNext().getNext().getOpcode() == ATHROW
                        ) {

                            fields.add(((FieldInsnNode) node).name);
                            methodNode.instructions.set(node.getNext(), new JumpInsnNode(GOTO, ((JumpInsnNode) node.getNext()).label));
                            methodNode.instructions.remove(node);
                            modified = true;
                        } else if ((node instanceof FieldInsnNode
                                && node.getOpcode() == GETSTATIC
                                && ((FieldInsnNode) node).desc.equals("I"))
                                && (node.getNext() instanceof FieldInsnNode
                                && node.getNext().getOpcode() == GETSTATIC
                                && ((FieldInsnNode) node.getNext()).desc.equals("I"))
                                && node.getNext().getNext() instanceof JumpInsnNode
                                && node.getNext().getNext().getNext().getOpcode() == ACONST_NULL
                            //&& node.getNext().getNext().getNext().getNext().getOpcode() == ATHROW
                        ) {

                            fields.add(((FieldInsnNode) node).name);
                            fields.add(((FieldInsnNode) node.getNext()).name);

                            methodNode.instructions.set(node.getNext().getNext(), new JumpInsnNode(GOTO, ((JumpInsnNode) node.getNext().getNext()).label));
                            methodNode.instructions.remove(node.getNext());
                            methodNode.instructions.remove(node);
                            modified = true;
                        }
                    }
                } while (modified);
            });

            classNode.fields.removeIf(fieldNode -> ((fieldNode.name.length() > 500 && fieldNode.name.startsWith("              ")) || fields.contains(fieldNode.name)) && (fieldNode.desc.equals("I") || fieldNode.desc.equals("Z")));
            fields.clear();
        });
    }
}
