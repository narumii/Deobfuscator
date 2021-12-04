package uwu.narumi.deobfuscator.transformer.impl.scuti;

import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.exception.TransformerException;
import uwu.narumi.deobfuscator.helper.ASMHelper;
import uwu.narumi.deobfuscator.transformer.Transformer;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

// https://github.com/netindev/scuti/blob/467b856b7ea46009608ccdf4db69b4b43e640fa6/scuti-core/src/main/java/tk/netindev/scuti/core/transform/obfuscation/StringEncryptionTransformer.java#L148
public class ScutiNormalStringTransformer extends Transformer {

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        deobfuscator.classes().forEach(classNode -> {
            AtomicInteger xorKey = new AtomicInteger(classNode.hashCode());
            classNode.methods.forEach(methodNode -> Arrays.stream(methodNode.instructions.toArray())
                    .filter(node -> node instanceof MethodInsnNode)
                    .filter(node -> node.getOpcode() == INVOKESTATIC)
                    .map(MethodInsnNode.class::cast)
                    .filter(node -> node.desc.equals("(Ljava/lang/String;)Ljava/lang/String;"))
                    .filter(node -> isString(node.getPrevious()))
                    .forEach(node -> {
                        String string = getString(node.getPrevious());
                        if (xorKey.get() == classNode.hashCode()) {
                            findMethod(classNode, decrypt -> decrypt.name.equals(node.name) && decrypt.desc.equals(node.desc))
                                    .ifPresent(decrypt -> xorKey.set(getKey(decrypt)));
                        }

                        methodNode.instructions.remove(node.getPrevious());
                        methodNode.instructions.set(node, new LdcInsnNode(decryptXor(string, xorKey.get())));
                    }));
        });
    }

    private int getKey(MethodNode node) {
        return Arrays.stream(node.instructions.toArray())
                .filter(ASMHelper::isInteger)
                .filter(insn -> insn.getNext().getOpcode() == IXOR)
                .filter(insn -> insn.getNext().getNext().getOpcode() == I2C)
                .map(ASMHelper::getInteger)
                .findFirst()
                .orElseThrow(TransformerException::new);
    }


    private String decryptXor(String string, int xor) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < string.length(); i++) {
            stringBuilder.append((char) (string.charAt(i) ^ xor));
        }
        return stringBuilder.toString();
    }
}