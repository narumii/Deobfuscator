package uwu.narumi.deobfuscator.transformer.impl.caesium;

import org.objectweb.asm.tree.*;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.transformer.Transformer;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.IvParameterSpec;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/*
    TODO: Fields clean / <clinit> clean / bootstrap clean
 */
public class CaesiumStringTransformer extends Transformer {

    private static final String BSM_DESC = "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/Object;";

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        deobfuscator.classes().forEach(classNode -> {
            MethodNode stringsMethod = getStringsMethod(classNode);
            if (stringsMethod == null)
                return;

            Pair<Long, FieldInsnNode> keys = getKeyFromStringsMethod(stringsMethod);
            Map<Integer, String> strings = getStrings(stringsMethod);
            List<FieldInsnNode> fieldInsnNodes = new ArrayList<>();
            if (strings.isEmpty() || keys.second() == null)
                return;

            classNode.methods.forEach(methodNode -> Arrays.stream(methodNode.instructions.toArray())
                    .filter(node -> node instanceof InvokeDynamicInsnNode)
                    .map(InvokeDynamicInsnNode.class::cast)
                    .filter(node -> node.bsmArgs == null || node.bsmArgs.length <= 0)
                    .filter(node -> node.desc.equals("(IJ)Ljava/lang/String;"))
                    .filter(node -> node.bsm.getDesc().equals(BSM_DESC))
                    .filter(node -> isLong(node.getPrevious()))
                    .filter(node -> isInteger(node.getPrevious().getPrevious()))
                    .forEach(node -> {
                        int position = getInteger(node.getPrevious().getPrevious());
                        if (!strings.containsKey(position))
                            return;

                        methodNode.instructions.remove(node.getPrevious().getPrevious());
                        methodNode.instructions.remove(node.getPrevious());
                        methodNode.instructions.set(node, new LdcInsnNode(decrypt(strings.get(position), keys.first())));
                    }));

            getDecryptMethod(classNode).ifPresent(methodNode -> fieldInsnNodes.addAll(Arrays.stream(methodNode.instructions.toArray())
                    .filter(node -> node instanceof FieldInsnNode)
                    .map(FieldInsnNode.class::cast)
                    .filter(node -> node.desc.equals("[Ljava/lang/String;"))
                    .collect(Collectors.toList())));

            findClInit(classNode).ifPresent(methodNode -> {
                for (AbstractInsnNode node : methodNode.instructions.toArray()) {
                    if (node instanceof MethodInsnNode) {
                        MethodInsnNode method = (MethodInsnNode) node;
                        if (method.name.equals(stringsMethod.name) && method.desc.equals(stringsMethod.desc))
                            methodNode.instructions.remove(node);
                    } else if (node instanceof FieldInsnNode) {
                        FieldInsnNode field = (FieldInsnNode) node;
                        if (field.getOpcode() == PUTSTATIC && fieldInsnNodes.stream().anyMatch(fieldNode -> field.name.equals(fieldNode.name) && field.desc.equals(fieldNode.desc))) {
                            methodNode.instructions.remove(node.getPrevious().getPrevious());
                            methodNode.instructions.remove(node.getPrevious());
                            methodNode.instructions.remove(node);
                        }
                    }
                }
            });

            classNode.fields.removeIf(field -> fieldInsnNodes.stream().anyMatch(fieldNode -> field.name.equals(fieldNode.name) && field.desc.equals(fieldNode.desc)));
            classNode.fields.removeIf(field -> field.name.equals(keys.second().name) && field.desc.equals(keys.second().desc));
            classNode.methods.removeIf(methodNode -> methodNode.desc.equals("(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/Object;"));
            classNode.methods.removeIf(methodNode -> (methodNode.desc.equals("()V") || methodNode.desc.equals("(IJ)Ljava/lang/String;")) && isAccess(methodNode.access, ACC_STATIC) && Arrays.stream(methodNode.instructions.toArray()).anyMatch(node -> isString(node) && getString(node).equals("DES/CBC/PKCS5Padding")));
        });
    }

    private MethodNode getStringsMethod(ClassNode classNode) {
        return classNode.methods.stream()
                .filter(methodNode -> methodNode.desc.equals("()V"))
                .filter(methodNode -> Arrays.stream(methodNode.instructions.toArray()).anyMatch(node -> isString(node) && getString(node).equals("DES/CBC/PKCS5Padding")))
                .filter(methodNode -> isAccess(methodNode.access, ACC_STATIC))
                .findFirst()
                .orElse(null);
    }

    private Pair<Long, FieldInsnNode> getKeyFromStringsMethod(MethodNode methodNode) {
        AtomicLong firstKey = new AtomicLong();
        AtomicLong secondKey = new AtomicLong();
        FieldInsnNode fieldInsnNode = null;

        for (AbstractInsnNode node : methodNode.instructions.toArray()) {
            if (!(node instanceof FieldInsnNode))
                continue;

            FieldInsnNode fieldNode = (FieldInsnNode) node;
            if (!fieldNode.desc.equals("J"))
                continue;

            if (node.getOpcode() == PUTSTATIC && isLong(node.getPrevious())) {
                firstKey.set(getLong(node.getPrevious()));
            } else if (node.getOpcode() == GETSTATIC && isLong(node.getNext())) {
                secondKey.set(getLong(node.getNext()));
                fieldInsnNode = fieldNode;
            }
        }

        return Pair.of(firstKey.get() ^ secondKey.get(), fieldInsnNode);
    }

    private Map<Integer, String> getStrings(MethodNode methodNode) {
        Map<Integer, String> strings = new HashMap<>();
        Arrays.stream(methodNode.instructions.toArray())
                .filter(node -> node instanceof TableSwitchInsnNode)
                .map(TableSwitchInsnNode.class::cast)
                .findFirst()
                .ifPresent(tableSwitch -> {
                    int start = methodNode.instructions.indexOf(tableSwitch.labels.get(0));
                    int end = methodNode.instructions.indexOf(tableSwitch.labels.get(1));

                    for (int i = start; i < end; i++) {
                        AbstractInsnNode node = methodNode.instructions.get(i);
                        if (isInteger(node) && isString(node.getNext()) && node.getNext().getNext().getOpcode() == AASTORE)
                            strings.put(getInteger(node), getString(node.getNext()));
                    }
                });

        return strings;
    }

    private Optional<MethodNode> getDecryptMethod(ClassNode classNode) {
        return classNode.methods.stream()
                .filter(node -> node.desc.equals("(IJ)Ljava/lang/String;"))
                .filter(methodNode -> Arrays.stream(methodNode.instructions.toArray()).anyMatch(node -> isString(node) && getString(node).equals("DES/CBC/PKCS5Padding")))
                .filter(methodNode -> isAccess(methodNode.access, ACC_STATIC))
                .findFirst();
    }

    private String decrypt(String s, long key) {
        try {
            Cipher var3;
            SecretKeyFactory var4;
            try {
                var3 = Cipher.getInstance("DES/CBC/PKCS5Padding");
                var4 = SecretKeyFactory.getInstance("DES");
            } catch (Exception var7) {
                throw new RuntimeException("dev/sim0n/evaluator/util/Log");
            }

            byte[] var5 = new byte[8];
            var5[0] = (byte) ((int) (key >>> 56));

            for (int var6 = 1; var6 < 8; ++var6) {
                var5[var6] = (byte) ((int) (key << var6 * 8 >>> 56));
            }

            var3.init(2, var4.generateSecret(new DESKeySpec(var5)), new IvParameterSpec(new byte[8]));
            return new String(var3.doFinal(Base64.getDecoder().decode(s)));
        } catch (Exception e) {
            e.printStackTrace();
            return s;
        }
    }

    private interface Pair<A, B> {
        static <A, B> Pair<A, B> of(A first, B second) {
            return new Pair<>() {
                @Override
                public A first() {
                    return first;
                }

                @Override
                public B second() {
                    return second;
                }
            };
        }

        A first();

        B second();
    }
}
