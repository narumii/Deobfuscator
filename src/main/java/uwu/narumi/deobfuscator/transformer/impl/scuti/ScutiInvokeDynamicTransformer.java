package uwu.narumi.deobfuscator.transformer.impl.scuti;

import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.exception.TransformerException;
import uwu.narumi.deobfuscator.helper.ASMHelper;
import uwu.narumi.deobfuscator.transformer.Transformer;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Python 28/oct/2021
 */
public class ScutiInvokeDynamicTransformer extends Transformer {

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        deobfuscator.classes()
                .forEach(classNode -> classNode.methods.stream()
                        .filter(methodNode -> methodNode.name.length() > 50)
                        .filter(methodNode -> methodNode.desc.equals("(Ljava/lang/String;)Ljava/lang/String;"))
                        .filter(methodNode -> methodNode.access == ACC_PRIVATE + ACC_STATIC + ACC_BRIDGE + ACC_SYNTHETIC)
                        .findFirst()
                        .ifPresent(decrypt -> {
                            AtomicInteger key = new AtomicInteger(getKey(decrypt));

                            classNode.methods.forEach(methodNode -> Arrays.stream(methodNode.instructions.toArray()).sequential()
                                    .filter(node -> node instanceof InvokeDynamicInsnNode)
                                    .map(InvokeDynamicInsnNode.class::cast)
                                    .filter(node -> node.bsm.getDesc().equals("(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Integer;)Ljava/lang/invoke/CallSite;"))
                                    .filter(node -> node.bsmArgs.length == 4)
                                    .filter(node -> node.name.length() > 50)
                                    .forEach(node -> {
                                        String owner = decrypt((String) node.bsmArgs[0], key.get()).replace('.', '/');
                                        String name = decrypt((String) node.bsmArgs[1], key.get());
                                        String desc = decrypt((String) node.bsmArgs[2], key.get());
                                        int opcode = (int) node.bsmArgs[3] == 0 ? INVOKESTATIC : INVOKEVIRTUAL;

                                        methodNode.instructions.set(node, new MethodInsnNode(opcode, owner, name, desc, false));
                                    }));

                            //classNode.methods.remove(decrypt);
                        }));
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

    private String decrypt(final String string, final int decryptValue) {
        final StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < string.length(); i++) {
            stringBuilder.append((char) (string.charAt(i) ^ decryptValue));
        }
        return stringBuilder.toString();
    }
}
