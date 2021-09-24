package uwu.narumi.deobfuscator.transformer.impl.binsecure.old;

import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.transformer.Transformer;

import java.util.Arrays;

public class OldBinsecureCrasherRemoveTransformer extends Transformer {

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        deobfuscator.classes().stream()
                .flatMap(classNode -> classNode.methods.stream())
                .forEach(methodNode -> {
                    Arrays.stream(methodNode.instructions.toArray())
                            .filter(node -> node instanceof JumpInsnNode)
                            .filter(node -> node.getPrevious().getOpcode() == GETSTATIC)
                            .filter(node -> ((FieldInsnNode) node.getPrevious()).desc.equals("I"))
                            .filter(node -> node.getNext() instanceof InvokeDynamicInsnNode)
                            .filter(node -> node.getNext().getNext() instanceof InvokeDynamicInsnNode)
                            .forEach(node -> {
                                InvokeDynamicInsnNode first = (InvokeDynamicInsnNode) node.getNext();
                                InvokeDynamicInsnNode second = (InvokeDynamicInsnNode) node.getNext().getNext();

                                if (first.name.equals("while") && second.name.equals("fuck")) {
                                    methodNode.instructions.remove(node.getNext().getNext());
                                    methodNode.instructions.remove(node.getNext());
                                    methodNode.instructions.remove(node.getPrevious());
                                    methodNode.instructions.remove(node);
                                }
                            });
                });
    }
}
