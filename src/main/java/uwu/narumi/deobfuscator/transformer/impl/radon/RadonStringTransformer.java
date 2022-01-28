package uwu.narumi.deobfuscator.transformer.impl.radon;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.transformer.Transformer;

import java.util.Arrays;

public class RadonStringTransformer extends Transformer {

    private String owner;
    private boolean isContextCheckingEnabled;

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        deobfuscator.classes().forEach(classNode -> classNode.methods.forEach(methodNode -> Arrays.stream(methodNode.instructions.toArray())
                .filter(node -> node instanceof MethodInsnNode)
                .filter(node -> node.getOpcode() == INVOKESTATIC)
                .map(MethodInsnNode.class::cast)
                .filter(node -> owner == null || node.owner.equals(owner))
                .filter(node -> node.desc.equals("(Ljava/lang/Object;I)Ljava/lang/String;"))
                .filter(node -> isString(node.getPrevious().getPrevious()))
                .filter(node -> isInteger(node.getPrevious()))
                .forEach(node -> {
                    if (owner == null) {
                        init(deobfuscator, node);
                    }

                    if (owner == null)
                        return;

                    String string = getString(node.getPrevious().getPrevious());
                    int randomKey = getInteger(node.getPrevious());

                    int callerClassHC = classNode.name.replace("/", ".").hashCode();
                    int callerMethodHC = methodNode.name.replace("/", ".").hashCode();
                    int decryptorClassHC = node.owner.replace("/", ".").hashCode();
                    int decryptorMethodHC = node.name.replace("/", ".").hashCode();

                    int key1 = ((isContextCheckingEnabled) ? callerClassHC + decryptorClassHC + callerMethodHC : 0) ^ randomKey;
                    int key2 = ((isContextCheckingEnabled) ? callerMethodHC + decryptorMethodHC + callerClassHC : 0) ^ randomKey;
                    int key3 = ((isContextCheckingEnabled) ? decryptorClassHC + callerClassHC + callerMethodHC : 0) ^ randomKey;
                    int key4 = ((isContextCheckingEnabled) ? decryptorMethodHC + callerClassHC + decryptorClassHC : 0) ^ randomKey;

                    methodNode.instructions.remove(node.getPrevious().getPrevious());
                    methodNode.instructions.remove(node.getPrevious());
                    methodNode.instructions.set(node, new LdcInsnNode(decrypt(string, key1, key2, key3, key4)));
                })));

        deobfuscator.getClasses().remove(owner);
    }

    private void init(Deobfuscator deobfuscator, MethodInsnNode methodInsnNode) {
        ClassNode classNode = deobfuscator.getClasses().get(methodInsnNode.owner);

        if (isDecryptMethod(classNode, methodInsnNode)) {
            owner = methodInsnNode.owner;
            isContextCheckingEnabled = isContextCheckingEnabled(classNode, methodInsnNode);
        }
    }

    private boolean isContextCheckingEnabled(ClassNode classNode, MethodInsnNode methodInsnNode) {
        return Arrays.stream(findMethod(classNode, methodInsnNode).orElseThrow().instructions.toArray())
                .filter(node -> node instanceof MethodInsnNode)
                .filter(node -> node.getOpcode() == INVOKEVIRTUAL)
                .map(MethodInsnNode.class::cast)
                .filter(node -> node.owner.equals("java/lang/String"))
                .filter(node -> node.name.equals("hashCode"))
                .filter(node -> node.desc.equals("()I"))

                .filter(node -> node.getPrevious() instanceof MethodInsnNode)
                .filter(node -> node.getPrevious().getOpcode() == INVOKEVIRTUAL)
                .filter(node -> ((MethodInsnNode) node.getPrevious()).owner.equals("java/lang/StackTraceElement"))
                .filter(node -> ((MethodInsnNode) node.getPrevious()).desc.equals("()Ljava/lang/String;"))

                .count() >= 6;
    }

    private boolean isDecryptMethod(ClassNode classNode, MethodInsnNode methodInsnNode) {
        return Arrays.stream(findMethod(classNode, methodInsnNode).orElseThrow().instructions.toArray())
                .filter(node -> node instanceof MethodInsnNode)
                .filter(node -> node.getOpcode() == INVOKEVIRTUAL)
                .map(MethodInsnNode.class::cast)
                .filter(node -> node.owner.equals("java/lang/String"))
                .filter(node -> node.name.equals("hashCode"))
                .filter(node -> node.desc.equals("()I"))
                .filter(node -> isString(node.getPrevious()))
                .filter(node -> node.getNext().getOpcode() == ISTORE)
                .count() >= 4;
    }

    private String decrypt(String s, int key1, int key2, int key3, int key4) {
        StringBuilder sb = new StringBuilder();
        char[] chars = s.toCharArray();

        for (int i = 0; i < chars.length; i++) {
            switch (i % 4) {
                case 0:
                    sb.append((char) (chars[i] ^ key1));
                    break;
                case 1:
                    sb.append((char) (chars[i] ^ key2));
                    break;
                case 2:
                    sb.append((char) (chars[i] ^ key3));
                    break;
                default:
                    sb.append((char) (chars[i] ^ key4));
                    break;
            }
        }

        return sb.toString();
    }
}
