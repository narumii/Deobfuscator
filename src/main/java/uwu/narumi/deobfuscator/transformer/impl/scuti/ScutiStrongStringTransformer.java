package uwu.narumi.deobfuscator.transformer.impl.scuti;

import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.transformer.Transformer;

import java.util.Arrays;

// https://github.com/netindev/scuti/blob/467b856b7ea46009608ccdf4db69b4b43e640fa6/scuti-core/src/main/java/tk/netindev/scuti/core/transform/obfuscation/StringEncryptionTransformer.java#L188
public class ScutiStrongStringTransformer extends Transformer {

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        deobfuscator.classes().forEach(classNode -> classNode.methods.forEach(methodNode -> Arrays.stream(methodNode.instructions.toArray())
                .filter(node -> node instanceof MethodInsnNode)
                .filter(node -> node.getOpcode() == INVOKESTATIC)
                .map(MethodInsnNode.class::cast)
                .filter(node -> node.desc.equals("(Ljava/lang/String;I)Ljava/lang/String;"))
                .filter(node -> isInteger(node.getPrevious()))
                .filter(node -> isString(node.getPrevious().getPrevious()))
                .forEach(node -> {
                    int xor = getInteger(node.getPrevious());
                    String string = getString(node.getPrevious().getPrevious());

                    methodNode.instructions.remove(node.getPrevious().getPrevious());
                    methodNode.instructions.remove(node.getPrevious());
                    methodNode.instructions.set(node, new LdcInsnNode(decryptStrong(string, node.name, xor)));
                })));
    }


    private String decryptStrong(String string, String methodName, int random) {
        int xor = random ^ methodName.hashCode();
        char[] array = new char[string.length()];
        for (int i = 0; i < string.length(); i++) {
            array[i] = (char) (string.charAt(i) ^ xor);
        }
        return new String(array);
    }
}