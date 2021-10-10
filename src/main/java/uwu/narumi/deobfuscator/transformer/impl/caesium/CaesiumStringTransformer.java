package uwu.narumi.deobfuscator.transformer.impl.caesium;

import org.objectweb.asm.tree.*;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.exception.TransformerException;
import uwu.narumi.deobfuscator.helper.ASMHelper;
import uwu.narumi.deobfuscator.helper.MathHelper;
import uwu.narumi.deobfuscator.transformer.Transformer;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.IvParameterSpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/*
    TODO: Fields clean / <clinit> clean / bootstrap clean
 */
public class CaesiumStringTransformer extends Transformer {

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        deobfuscator.classes().forEach(classNode -> {
            Map<Integer, String> strings = new HashMap<>();

            AtomicLong firstKey = new AtomicLong();
            AtomicLong secondKey = new AtomicLong();

            findMethod(classNode, methodNode -> MathHelper.INTEGER_PATTERN.matcher(methodNode.name).matches() && methodNode.desc.equals("()V"))
                    .filter(methodNode -> Arrays.stream(methodNode.instructions.toArray()).anyMatch(node -> isString(node) && getString(node).equals("DES/CBC/PKCS5Padding")))
                    .ifPresent(methodNode -> {
                        Arrays.stream(methodNode.instructions.toArray())
                                .filter(node -> node instanceof TableSwitchInsnNode)
                                .map(TableSwitchInsnNode.class::cast)
                                .forEach(node -> {
                                    LabelNode start = node.labels.get(0);
                                    AbstractInsnNode end = node.labels.get(1);

                                    //getInstructionsBetween
                                    AbstractInsnNode current = start;
                                    while (!current.equals(end)) {
                                        if (isString(current) && isInteger(current.getPrevious()) && check(current.getNext(), AASTORE)) {
                                            int position = getInteger(current.getPrevious());
                                            if (!strings.containsKey(position)) { //no computeIfAbsent because java sucks
                                                strings.put(position, getString(current));
                                            }
                                        }

                                        current = current.getNext();
                                    }
                                });

                        classNode.methods.remove(methodNode);
                    });

            if (strings.isEmpty())
                return;

            findMethod(classNode, methodNode -> MathHelper.INTEGER_PATTERN.matcher(methodNode.name).matches() && methodNode.desc.equals("(IJ)Ljava/lang/String;"))
                    .filter(methodNode -> Arrays.stream(methodNode.instructions.toArray()).anyMatch(node -> isString(node) && getString(node).equals("DES/CBC/PKCS5Padding")))
                    .ifPresent(methodNode -> {
                        Long[] keys = Arrays.stream(methodNode.instructions.toArray())
                                .filter(ASMHelper::isLong)
                                .filter(node -> check(node.getNext(), LXOR))
                                .map(ASMHelper::getLong).toArray(Long[]::new);

                        if (keys.length < 2)
                            throw new TransformerException();

                        firstKey.set(keys[0]);
                        secondKey.set(keys[1]);

                        classNode.methods.remove(methodNode);
                    });

            classNode.methods.forEach(methodNode -> Arrays.stream(methodNode.instructions.toArray())
                    .filter(node -> node instanceof InvokeDynamicInsnNode)
                    .filter(node -> isLong(node.getPrevious()))
                    .filter(node -> isInteger(node.getPrevious().getPrevious()))
                    .map(InvokeDynamicInsnNode.class::cast)
                    .filter(node -> node.desc.equals("(IJ)Ljava/lang/String;"))
                    .forEach(node -> {
                        int position = getInteger(node.getPrevious().getPrevious());
                        long key = getLong(node.getPrevious());
                        String string = strings.get(position);
                        if (string == null)
                            return;

                        methodNode.instructions.remove(node.getPrevious().getPrevious());
                        methodNode.instructions.remove(node.getPrevious());
                        methodNode.instructions.set(node, new LdcInsnNode(decrypt(string, key, firstKey.get(), secondKey.get())));
                    }));

            strings.clear();
        });
    }


    private String decrypt(String s, long var1, long a, long b) {
        try {
            var1 ^= a;
            var1 ^= b;

            Cipher var3;
            SecretKeyFactory var4;
            try {
                var3 = Cipher.getInstance("DES/CBC/PKCS5Padding");
                var4 = SecretKeyFactory.getInstance("DES");
            } catch (Exception var7) {
                throw new RuntimeException("dev/sim0n/evaluator/util/Log");
            }

            byte[] var5 = new byte[8];
            var5[0] = (byte) ((int) (var1 >>> 56));

            for (int var6 = 1; var6 < 8; ++var6) {
                var5[var6] = (byte) ((int) (var1 << var6 * 8 >>> 56));
            }

            var3.init(2, var4.generateSecret(new DESKeySpec(var5)), new IvParameterSpec(new byte[8]));
            return new String(var3.doFinal(Base64.getDecoder().decode(s)));
        } catch (Exception e) {
            e.printStackTrace();
            return s;
        }
    }
}
