package uwu.narumi.deobfuscator.transformer.impl.qprotect.b3_0;

import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.helper.ASMHelper;
import uwu.narumi.deobfuscator.transformer.Transformer;

import java.util.Arrays;

/*
    This transformer works only on version: 3.0-b1
 */
public class qProtectStringTransformer extends Transformer {

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        deobfuscator.classes().stream()
                .flatMap(classNode -> classNode.methods.stream())
                .forEach(methodNode -> Arrays.stream(methodNode.instructions.toArray())
                        .filter(ASMHelper::isString)
                        .filter(node -> node.getNext().getOpcode() == INVOKESTATIC)
                        .filter(node -> ((MethodInsnNode) node.getNext()).desc.equals("(Ljava/lang/String;)Ljava/lang/String;"))
                        .forEach(node -> {
                            String original = getString(node);
                            String decode = decode(original);
                            String encode = encode(decode);

                            if (encode.equals(original)) {
                                methodNode.instructions.remove(node.getNext());
                                methodNode.instructions.set(node, new LdcInsnNode(decode));
                            }
                        }));
    }


    private String decode(String string) {
        try {
            char[] chars = string.toCharArray();
            char[] newChars = new char[chars.length];
            char[] firstObjects = new char[]{'\u4832', '\u2385', '\u2386', '\u9813', '\u9125', '\u4582', '\u0913', '\u3422', '\u0853', '\u0724'};
            char[] secondObjects = new char[]{'\u4820', '\u8403', '\u8753', '\u3802', '\u3840', '\u3894', '\u8739', '\u1038', '\u8304', '\u3333'};

            for (int i = 0; i < chars.length; ++i) {
                newChars[i] = (char) (chars[i] ^ firstObjects[i % firstObjects.length]);
            }

            char[] out = new char[newChars.length];
            for (int i = 0; i < chars.length; ++i) {
                out[i] = (char) (newChars[i] ^ secondObjects[i % secondObjects.length]);
            }

            return new String(out);
        } catch (Exception var7) {
            return string;
        }
    }

    private String encode(String string) {
        try {
            char[] chars = string.toCharArray();
            char[] newChars = new char[chars.length];
            char[] firstObjects = new char[]{'\u4832', '\u2385', '\u2386', '\u9813', '\u9125', '\u4582', '\u0913', '\u3422', '\u0853', '\u0724'};
            char[] secondObjects = new char[]{'\u4820', '\u8403', '\u8753', '\u3802', '\u3840', '\u3894', '\u8739', '\u1038', '\u8304', '\u3333'};

            for (int i = 0; i < chars.length; ++i) {
                newChars[i] = (char) (chars[i] ^ secondObjects[i % secondObjects.length]);
            }

            char[] out = new char[newChars.length];
            for (int i = 0; i < chars.length; ++i) {
                out[i] = (char) (newChars[i] ^ firstObjects[i % firstObjects.length]);
            }

            return new String(out);
        } catch (Exception var7) {
            return string;
        }
    }
}
