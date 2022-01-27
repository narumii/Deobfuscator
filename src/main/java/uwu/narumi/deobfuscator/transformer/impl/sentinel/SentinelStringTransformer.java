package uwu.narumi.deobfuscator.transformer.impl.sentinel;

import org.objectweb.asm.tree.*;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.transformer.Transformer;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * A transformer to de-obfuscate sentinel's built in manipulator string encryption.
 *
 *
 * @author Z3R0
 */
public class SentinelStringTransformer extends Transformer {
    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        deobfuscator.classes().forEach(classNode -> {

            MethodNode decryptMethod = classNode.methods.stream()
                    .filter(methodNode -> methodNode.desc.equalsIgnoreCase("(Ljava/lang/String;)Ljava/lang/String;"))
                    .filter(methodNode -> methodNode.access == ACC_PUBLIC + ACC_STATIC)
                    .filter(methodNode -> methodNode.maxLocals == 4 && methodNode.maxStack == 4)
                    .findFirst().orElse(null);

            if (decryptMethod == null) {
                return;
            }


            classNode.methods.stream()
                    .forEach(methodNode -> {
                                Arrays.stream(methodNode.instructions.toArray())
                                        .filter(abstractInsnNode -> abstractInsnNode instanceof MethodInsnNode)
                                        .map(abstractInsnNode -> (MethodInsnNode) abstractInsnNode)
                                        .filter(methodInsnNode -> methodInsnNode.name.equalsIgnoreCase(decryptMethod.name))
                                        .filter(methodInsnNode -> methodInsnNode.desc.equalsIgnoreCase(decryptMethod.desc))
                                        .filter(methodInsnNode -> methodInsnNode.getPrevious() instanceof LdcInsnNode)
                                        .forEach(methodInsnNode -> {
                                            if (((LdcInsnNode) methodInsnNode.getPrevious()).cst instanceof String) {


                                                String decrypted = decrypt(((LdcInsnNode) methodInsnNode.getPrevious()).cst.toString());

                                                methodNode.instructions.insert(methodInsnNode, new LdcInsnNode(decrypted));
                                                methodNode.instructions.remove(methodInsnNode.getPrevious());
                                                methodNode.instructions.remove(methodInsnNode);
                                            }
                                        });
                            }
                    );

            classNode.methods.remove(decryptMethod);


        });


    }

    public String decrypt(java.lang.String string) {
        char[] cArray = new char[string.length()];
        int n = 0;
        while (n < string.length()) {
            char c = string.charAt(n);
            cArray[n] = (char) (c / 2);
            ++n;
        }
        return new java.lang.String(cArray);
    }
}
