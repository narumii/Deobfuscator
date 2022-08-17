package uwu.narumi.deobfuscator.transformer.impl.branchlock;

import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.transformer.Transformer;

import java.util.Arrays;

public class BranchlockHashCodeTransformer extends Transformer {
    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        deobfuscator.classes().stream()
                .flatMap(classNode -> classNode.methods.stream())
                .forEach(methodNode -> Arrays.stream(methodNode.instructions.toArray())
                        .filter(node -> node.getOpcode() == LDC)
                        .filter(node -> node.getNext() instanceof MethodInsnNode)
                        .filter(node -> ((MethodInsnNode) node.getNext()).name.equals("hashCode"))
                        .filter(node -> ((MethodInsnNode) node.getNext()).owner.equals("java/lang/String"))
                        .filter(node -> ((MethodInsnNode) node.getNext()).desc.equals("()I"))
                        .forEach(node -> {
                            int val = ((LdcInsnNode) node).cst.hashCode();
                            methodNode.instructions.remove(node.getNext());
                            methodNode.instructions.set(node, new LdcInsnNode(val));
                        }));

    }
}
