package uwu.narumi.deobfuscator.transformer.impl.sentinel;

import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.transformer.Transformer;

import java.util.Arrays;

/**
 * A transformer to de-obfuscate sentinel's built in manipulator string encryption.
 *
 * @author Z3R0
 */
public class SentinelStringTransformer extends Transformer {

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        deobfuscator.classes().forEach(classNode -> {

            MethodNode decryptMethod = classNode.methods.stream()
                    .filter(methodNode -> methodNode.desc.equals("(Ljava/lang/String;)Ljava/lang/String;"))
                    .filter(methodNode -> methodNode.access == ACC_PUBLIC + ACC_STATIC)
                    .filter(methodNode -> methodNode.maxLocals == 4)
                    .filter(methodNode -> methodNode.maxStack == 4)
                    .findFirst().orElseThrow();

            classNode.methods.forEach(methodNode -> Arrays.stream(methodNode.instructions.toArray())
                    .filter(node -> node instanceof MethodInsnNode)
                    .map(MethodInsnNode.class::cast)
                    .filter(node -> node.name.equals(decryptMethod.name))
                    .filter(node -> node.desc.equals(decryptMethod.desc))
                    .filter(node -> isString(node.getPrevious()))
                    .forEach(node -> {
                        String decrypted = decrypt(getString(node.getPrevious()));

                        methodNode.instructions.remove(node.getPrevious());
                        methodNode.instructions.set(node, new LdcInsnNode(decrypted));
                    })
            );

            classNode.methods.remove(decryptMethod);
        });
    }

    public String decrypt(String string) {
        char[] cArray = new char[string.length()];
        int n = 0;
        while (n < string.length()) {
            char c = string.charAt(n);
            cArray[n] = (char) (c / 2);
            ++n;
        }
        return new String(cArray);
    }
}
