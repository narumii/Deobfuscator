package uwu.narumi.deobfuscator.transformer.impl.binsecure.latest;

import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.transformer.Transformer;

import java.util.Arrays;

public class BinsecureInvokeDynamicCallTransformer extends Transformer {

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        deobfuscator.classes().forEach(classNode -> classNode.methods.forEach(methodNode -> Arrays.stream(methodNode.instructions.toArray())
                .filter(node -> node instanceof InvokeDynamicInsnNode)
                .map(InvokeDynamicInsnNode.class::cast)
                .filter(node -> node.bsm.getDesc().equals("(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/Object;"))
                .filter(node -> node.bsmArgs.length == 4)
                .forEach(node -> {
                    int classHash = classNode.name.replace('/', '.').hashCode();
                    int methodHash = methodNode.name.hashCode();

                    methodNode.instructions.set(node,
                            new MethodInsnNode(
                                    (int) node.bsmArgs[0],
                                    decrypt((String) node.bsmArgs[1], classHash, methodHash).replace('.', '/'),
                                    decrypt((String) node.bsmArgs[2], classHash, methodHash),
                                    decrypt((String) node.bsmArgs[3], classHash, methodHash)
                            )
                    );
                })));
    }

    /*
        Fuck kotlin
     */
    private String decrypt(String string, int classHash, int methodHash) {
        char[] chars = string.toCharArray();

        for (int i = 0; i < chars.length; i++) {
            switch (i % 5) {
                case 0:
                    chars[i] ^= 2;
                    break;
                case 1:
                    chars[i] ^= classHash;
                    break;
                case 2:
                    chars[i] ^= methodHash;
                    break;
                case 3:
                    chars[i] ^= classHash + methodHash;
                    break;
                case 4:
                    chars[i] ^= i;
                    break;
            }
        }

        return new String(chars);
    }
}
