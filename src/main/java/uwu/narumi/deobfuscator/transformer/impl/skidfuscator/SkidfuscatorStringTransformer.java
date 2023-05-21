package uwu.narumi.deobfuscator.transformer.impl.skidfuscator;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.helper.ASMHelper;
import uwu.narumi.deobfuscator.transformer.Transformer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;

public class SkidfuscatorStringTransformer extends Transformer {
    // Goofiest transformer
    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        deobfuscator.classes().forEach(classNode -> {
            HashMap<ClassNode, byte[]> classToKeys = new HashMap<>();

            new ArrayList<>(classNode.methods).forEach(mn -> Arrays.stream(mn.instructions.toArray())
                    .filter(node -> node.getOpcode() == INVOKESTATIC)
                    .map(MethodInsnNode.class::cast)
                    .filter(node -> node.desc.equals("(Ljava/lang/String;I)Ljava/lang/String;"))
                    .filter(node -> isInteger(node.getPrevious()))
                    .filter(node -> isString(node.getPrevious().getPrevious()))
                    .forEach(node -> {
                        if (!classToKeys.containsKey(classNode)) {
                            MethodNode decryptMethod = classNode.methods.stream().filter(methodNode -> methodNode.name.equals(node.name)).findFirst().get();
                            classNode.methods.remove(decryptMethod);

                            Arrays.stream(decryptMethod.instructions.toArray())
                                    .filter(n -> n.getOpcode() == NEWARRAY)
                                    .forEach(n -> {
                                        int length = ASMHelper.getInteger(n.getPrevious());
                                        byte[] keys = new byte[length];

                                        ASMHelper.getInstructionsBetween(n, decryptMethod.instructions.get(decryptMethod.instructions.indexOf(n) + (length * 4))).stream()
                                                .filter(insn -> insn.getOpcode() == BASTORE)
                                                .forEach(insn -> keys[ASMHelper.getInteger(insn.getPrevious().getPrevious())] = (byte) ASMHelper.getInteger(insn.getPrevious()));
                                        classToKeys.put(classNode, keys);
                                    });
                        }

                        String string = getString(node.getPrevious().getPrevious());
                        int xor = getInteger(node.getPrevious());
                        byte[] keys = classToKeys.get(classNode);

                        mn.instructions.remove(node.getPrevious().getPrevious());
                        mn.instructions.remove(node.getPrevious());
                        mn.instructions.set(node, new LdcInsnNode(decrypt(string, xor, keys)));
                    })
            );
        });
    }

    private String decrypt(String string, int xor, byte[] keys) {
        byte[] encrypted = Base64.getDecoder().decode(string.getBytes());
        byte[] key = Integer.toString(xor).getBytes();

        for (int i = 0; i < encrypted.length; i++) {
            encrypted[i] ^= key[i % key.length];
            encrypted[i] ^= keys[i % keys.length];
        }
        return new String(encrypted);
    }
}
