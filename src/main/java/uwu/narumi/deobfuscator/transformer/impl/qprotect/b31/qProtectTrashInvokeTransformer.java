package uwu.narumi.deobfuscator.transformer.impl.qprotect.b31;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.transformer.Transformer;

import java.util.Arrays;

public class qProtectTrashInvokeTransformer extends Transformer {

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        deobfuscator.classes().stream()
                .flatMap(classNode -> classNode.methods.stream())
                .forEach(methodNode -> Arrays.stream(methodNode.instructions.toArray())
                        .filter(node -> node instanceof MethodInsnNode)
                        .filter(node -> node.getOpcode() == INVOKESTATIC)
                        .map(MethodInsnNode.class::cast)
                        .filter(node -> node.desc.equals("()V"))
                        .forEach(node -> {
                            ClassNode classNode = deobfuscator.getClasses().get(node.owner);
                            if (classNode != null) {
                                findMethod(classNode, method -> method.name.equals(node.name) && method.desc.equals(node.desc))
                                        .ifPresent(method -> methodNode.instructions.remove(node));
                            }
                        }));
    }
}
