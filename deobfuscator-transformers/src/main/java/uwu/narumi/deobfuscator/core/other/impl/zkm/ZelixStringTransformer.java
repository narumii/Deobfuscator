package uwu.narumi.deobfuscator.core.other.impl.zkm;

import org.objectweb.asm.tree.*;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class ZelixStringTransformer extends Transformer {

    HashMap<String, Integer> keyType1 = new HashMap<>();
    HashMap<String, List<Byte>> keyType2 = new HashMap<>();
    HashMap<String, Integer> staticArraySize = new HashMap<>();
    HashMap<String, List<Integer>> offsets = new HashMap<>();

    HashMap<String, List<String>> encryptedStrings = new HashMap<>();

    /* Written by https://github.com/Lampadina17 | OG 19/07/2024, Rewritten 09/08/2024 */
    @Override
    protected void transform() throws Exception {
        scopedClasses().forEach(classWrapper -> {
            /* Extract key type 1 from hardcoded xor encryption */
            classWrapper.methods().stream()
                    .filter(methodNode -> methodNode.desc.equals("(Ljava/lang/String;)[C"))
                    .forEach(methodNode ->
                            Arrays.stream(methodNode.instructions.toArray())
                                    .filter(ain -> ain instanceof IntInsnNode)
                                    .filter(ain -> ain.getNext() instanceof InsnNode)
                                    .filter(ain -> ain.getNext().getOpcode() == IXOR)
                                    .map(IntInsnNode.class::cast)
                                    .forEach(iin -> keyType1.put(classWrapper.name(), iin.operand)));

            /* Temporary variable */
            List<Byte> key2 = new ArrayList<>();

            /* Extract key type 2 from hardcoded switch case xor encryption */
            classWrapper.methods().stream()
                    .filter(methodNode -> methodNode.desc.equals("([C)Ljava/lang/String;"))
                    .forEach(methodNode ->
                            Arrays.stream(methodNode.instructions.toArray())
                                    .filter(ain -> ain instanceof IntInsnNode)
                                    .map(IntInsnNode.class::cast)
                                    .forEach(iin -> key2.add((byte) iin.operand)));

            /* Store the key data by class */
            if (!key2.isEmpty()) keyType2.put(classWrapper.name(), key2);

            /* Retrieve array length (static block) */
            classWrapper.methods().stream()
                    .filter(methodNode -> methodNode.name.equals("<clinit>"))
                    .forEach(methodNode -> {
                        if (methodNode.instructions.getFirst() instanceof IntInsnNode && methodNode.instructions.getFirst().getNext() instanceof TypeInsnNode) {
                            staticArraySize.put(classWrapper.name(), ((IntInsnNode) methodNode.instructions.getFirst()).operand);
                        }
                    });

            /* Temporary variable */
            List<String> strings = new ArrayList<>();

            /* Retrieve ciphered strings (static block) */
            classWrapper.methods().stream()
                    .filter(methodNode -> methodNode.name.equals("<clinit>"))
                    .forEach(methodNode -> Arrays.stream(methodNode.instructions.toArray())
                            .filter(ain -> ain instanceof LdcInsnNode)
                            .map(LdcInsnNode.class::cast)
                            .filter(ldc -> ldc.cst instanceof String)
                            .forEach(ldc -> strings.add((String) ldc.cst)));

            if (!strings.isEmpty()) encryptedStrings.put(classWrapper.name(), strings);

            /* Temporary Variable */
            List<Integer> offsets = new ArrayList<>();

            /* Retrieve weird zkm "offsets" */
            classWrapper.methods().stream()
                    .filter(methodNode -> methodNode.name.equals("<clinit>"))
                    .forEach(methodNode ->
                            Arrays.stream(methodNode.instructions.toArray())
                                    .forEach(ain -> {
                                        AbstractInsnNode prev = ain.getPrevious();
                                        if (prev != null && prev.getPrevious() != null && prev.previous() instanceof MethodInsnNode min && min.name.equals("length")) {
                                            if (ain instanceof IntInsnNode iin) {
                                                offsets.add(iin.operand);
                                            } else if (ain instanceof InsnNode in && in.getOpcode() >= ICONST_M1 && in.getOpcode() <= ICONST_5) {
                                                offsets.add(getValue(in));
                                            }
                                        }
                                    }));

            if (!offsets.isEmpty()) this.offsets.put(classWrapper.name(), offsets);
        });

        /* Decrypt and cleanup */
        scopedClasses().forEach(classWrapper -> {
            classWrapper.methods().stream()
                    .forEach(methodNode -> {
                        List<String> encrypted = encryptedStrings.get(classWrapper.name());
                        List<Integer> offsets = this.offsets.get(classWrapper.name());

                        if (encrypted != null && encrypted.size() == 2 && offsets != null && offsets.size() == 2) {
                            /* for classes that has big static block */
                            Arrays.stream(methodNode.instructions.toArray())
                                    .filter(ain -> ain.getOpcode() == AALOAD)
                                    .filter(ain -> ain.getPrevious().getPrevious().getOpcode() == ALOAD)
                                    .forEach(ain -> {
                                        int index = 0;
                                        if (ain.getPrevious() instanceof IntInsnNode iin) index = getValue(iin);
                                        else if (ain.getPrevious() instanceof InsnNode in) index = getValue(in);

                                        List<Byte> key2 = keyType2.get(classWrapper.name());

                                        if (key2 != null)
                                            try {
                                                String[] decryptedStrings = ZKMCipher.StaticInit(
                                                        encrypted.get(0),
                                                        encrypted.get(1),
                                                        keyType1.get(classWrapper.name()),
                                                        shiftBytes(key2),
                                                        offsets.get(0),
                                                        offsets.get(1),
                                                        staticArraySize.get(classWrapper.name()),
                                                        key2.get(0));

                                                /* Re-insert original string back to its place */
                                                methodNode.instructions.insert(ain, new LdcInsnNode(decryptedStrings[index]));
                                                /* Cleanup */
                                                methodNode.instructions.remove(ain.getPrevious().getPrevious());
                                                methodNode.instructions.remove(ain.getPrevious());
                                                methodNode.instructions.remove(ain);
                                                this.markChange();
                                            } catch (Exception e) {
                                            }
                                    });
                        } else {
                            AtomicBoolean cleanup = new AtomicBoolean(false);

                            List<Byte> key2 = keyType2.get(classWrapper.name());

                            if (key2 != null) {
                                Arrays.stream(methodNode.instructions.toArray())
                                        .filter(ain -> ain instanceof LdcInsnNode)
                                        .map(LdcInsnNode.class::cast)
                                        .filter(ldc -> ldc.cst instanceof String)
                                        .forEach(ldc -> {
                                            try {
                                                ldc.cst = ZKMCipher.cipher2(ZKMCipher.cipher1((String) ldc.cst, keyType1.get(classWrapper.name())), shiftBytes(key2), key2.get(0));
                                                cleanup.set(true);
                                            } catch (Exception e) {
                                            }
                                        });

                                if (cleanup.get())
                                    Arrays.stream(methodNode.instructions.toArray())
                                            .filter(ain -> ain.getOpcode() == SWAP)
                                            .filter(ain -> ain.getNext() != null && ain.getNext() instanceof MethodInsnNode)
                                            .filter(ain -> ain.getNext().getNext() != null && ain.getNext().getNext() instanceof MethodInsnNode)
                                            .filter(ain -> ain.getNext().getNext().getNext() != null && ain.getNext().getNext().getNext().getOpcode() == SWAP)
                                            .forEach(ain -> {
                                                /* Do cleanup */
                                                methodNode.instructions.remove(ain.getNext().getNext().getNext());
                                                methodNode.instructions.remove(ain.getNext().getNext());
                                                methodNode.instructions.remove(ain.getNext());
                                                methodNode.instructions.remove(ain);
                                                this.markChange();
                                            });
                            }
                        }
                    });
        });
        LOGGER.info("Decrypted {} strings in {} classes", this.getChangesCount(), scopedClasses().size());
    }

    /* Convert arraylist to array and shift values, when a bug transform into a feature (Key type 2) */
    private byte[] shiftBytes(List<Byte> input) {
        byte[] keyBytes = new byte[input.size() - 1];

        int j = 1;
        for (int i = 0; i < keyBytes.length; i++) {
            keyBytes[i] = input.get(j);
            j++;
        }
        return keyBytes;
    }

    public int getValue(AbstractInsnNode in) {
        int opcode = in.getOpcode();
        return switch (opcode) {
            case ICONST_M1 -> -1;
            case ICONST_0 -> 0;
            case ICONST_1 -> 1;
            case ICONST_2 -> 2;
            case ICONST_3 -> 3;
            case ICONST_4 -> 4;
            case ICONST_5 -> 5;
            case SIPUSH, BIPUSH -> ((IntInsnNode) in).operand;
            default -> throw new RuntimeException("Unsupported opcode");
        };
    }

    public static class ZKMCipher {

        public static char[] cipher1(final String var0, final int key) { // All old versions
            final char[] input = var0.toCharArray();
            if (input.length < 2) {
                input[0] ^= key;
            }
            return input;
        }

        public static String cipher2(final char[] input, final byte[] keys, final int length) throws Exception {
            if (keys.length != length) throw new Exception("Key is invalid");
            for (int i = 0; input.length > i; ++i) {
                input[i] ^= (char) keys[i % length];
            }
            return (new String(input)).intern();
        }

        public static String[] StaticInit(String encrypted1, String encrypted2, int key1, byte[] key2, int offset, int offset2, int arraysize, int length) throws Exception {
            final String[] h2 = new String[arraysize];
            int n = 0;
            String s;
            int n2 = (s = encrypted1).length();
            int n3 = offset;
            int n4 = -1;

            Label_0023:
            while (true) {
                while (true) {
                    ++n4;
                    final String s2 = s;
                    final int n5 = n4;
                    String s3 = s2.substring(n5, n5 + n3);
                    int n6 = -1;
                    while (true) {
                        final String a = ZKMCipher.cipher2(ZKMCipher.cipher1(s3, key1), key2, length);
                        switch (n6) {
                            default: {
                                h2[n++] = a;
                                if ((n4 += n3) < n2) {
                                    n3 = s.charAt(n4);
                                    continue Label_0023;
                                }
                                n2 = (s = encrypted2).length();
                                n3 = offset2;
                                n4 = -1;
                                break;
                            }
                            case 0: {
                                h2[n++] = a;
                                if ((n4 += n3) < n2) {
                                    n3 = s.charAt(n4);
                                    break;
                                }
                                break Label_0023;
                            }
                        }
                        ++n4;
                        final String s4 = s;
                        final int n7 = n4;
                        s3 = s4.substring(n7, n7 + n3);
                        n6 = 0;
                    }
                }
            }
            return h2;
        }
    }
}
