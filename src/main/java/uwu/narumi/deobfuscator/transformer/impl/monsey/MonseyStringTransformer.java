package uwu.narumi.deobfuscator.transformer.impl.monsey;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.transformer.Transformer;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

/**
 * TODO: Analyzer decrypt like in Binsecre
 *
 * @see uwu.narumi.deobfuscator.transformer.impl.binsecure.latest.BinsecureStringTransformer
 */
public class MonseyStringTransformer extends Transformer {

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        deobfuscator.classes().forEach(classNode -> {
            AtomicReference<MethodInsnNode> decryptMethod = new AtomicReference<>();

            classNode.methods.forEach(methodNode -> decryptNormally(classNode, methodNode, decryptMethod));
            if (decryptMethod.get() != null) {
                classNode.methods.removeIf(method -> method.name.equals(decryptMethod.get().name) && method.desc.equals("(Ljava/lang/String;II)Ljava/lang/String;"));
            }
        });
    }

    private void decryptNormally(ClassNode classNode, MethodNode methodNode, AtomicReference<MethodInsnNode> decryptMethod) {
        Arrays.stream(methodNode.instructions.toArray())
                .filter(node -> node instanceof MethodInsnNode)
                .filter(node -> node.getOpcode() == INVOKESTATIC)
                .map(MethodInsnNode.class::cast)
                .filter(node -> node.desc.equals("(Ljava/lang/String;II)Ljava/lang/String;"))
                .filter(node -> check(node.getPrevious(), POP))
                .filter(node -> check(node.getPrevious().getPrevious(), DUP))
                .filter(node -> check(node.getPrevious().getPrevious().getPrevious(), SWAP))
                .filter(node -> isInteger(node.getPrevious().getPrevious().getPrevious().getPrevious())) //second key
                .filter(node -> isInteger(node.getPrevious().getPrevious().getPrevious().getPrevious().getPrevious())) //first key
                .filter(node -> isString(node.getPrevious().getPrevious().getPrevious().getPrevious().getPrevious().getPrevious())) //string
                .forEach(node -> {
                    String string = getString(node.getPrevious().getPrevious().getPrevious().getPrevious().getPrevious().getPrevious());
                    int firstKey = getInteger(node.getPrevious().getPrevious().getPrevious().getPrevious().getPrevious());
                    int secondKey = getInteger(node.getPrevious().getPrevious().getPrevious().getPrevious());

                    methodNode.instructions.remove(node.getPrevious().getPrevious().getPrevious().getPrevious().getPrevious().getPrevious());
                    methodNode.instructions.remove(node.getPrevious().getPrevious().getPrevious().getPrevious().getPrevious());
                    methodNode.instructions.remove(node.getPrevious().getPrevious().getPrevious().getPrevious());
                    methodNode.instructions.remove(node.getPrevious().getPrevious().getPrevious());
                    methodNode.instructions.remove(node.getPrevious().getPrevious());
                    methodNode.instructions.remove(node.getPrevious());

                    methodNode.instructions.set(node, new LdcInsnNode(decrypt(string, secondKey, firstKey)));
                    decryptMethod.set(node);
                });
    }

    private String decrypt(String string, int firstKey, int secondKey) {
        char[] chars = string.toCharArray();
        for (int i = 0; i < chars.length; ++i) {
            chars[i] = (char) (chars[i] ^ ~(firstKey ^ secondKey));
        }
        return new String(chars);
    }
}
