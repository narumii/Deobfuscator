package uwu.narumi.deobfuscator.transformer.impl.scuti;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.exception.TransformerException;
import uwu.narumi.deobfuscator.helper.ASMHelper;
import uwu.narumi.deobfuscator.helper.ClassHelper;
import uwu.narumi.deobfuscator.transformer.Transformer;

import java.util.Arrays;

public class ScutiLoaderTransformer extends Transformer {

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        deobfuscator.classes().stream()
                .filter(classNode -> classNode.superName.equals("java/lang/ClassLoader"))
                .filter(classNode -> classNode.methods.size() == 6)
                .findFirst()
                .ifPresent(classNode -> {
                    int fileKey = getFileKey(classNode.methods.stream()
                            .filter(methodNode -> methodNode.desc.equals("([B)[B"))
                            .findFirst()
                            .orElseThrow());

                    deobfuscator.classes().remove(classNode);
                    deobfuscator.getFiles().forEach((name, bytes) -> {
                        try {
                            bytes = decrypt(bytes, fileKey);
                            if (!ClassHelper.isClass("chuj.class", bytes))
                                return;

                            ClassNode decrypted = ClassHelper.loadClass(bytes, deobfuscator.getClassReaderFlags());
                            deobfuscator.getClasses().put(decrypted.name, decrypted);
                        } catch (Exception ignored) {
                        }
                    });
                });
    }

    private int getFileKey(MethodNode node) {
        return Arrays.stream(node.instructions.toArray())
                .filter(ASMHelper::isInteger)
                .filter(insn -> insn.getNext().getOpcode() == IXOR)
                .filter(insn -> insn.getNext().getNext().getOpcode() == I2B)
                .map(ASMHelper::getInteger)
                .findFirst()
                .orElseThrow(TransformerException::new);
    }

    private byte[] decrypt(byte[] bytes, int key) {
        byte[] out = new byte[bytes.length];
        for (int i = 0; i < bytes.length; ++i) {
            out[i] = (byte) (bytes[i] ^ key);
        }
        return out;
    }
}
