package uwu.narumi.deobfuscator.core.other.impl.zkm;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.context.Context;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

import java.util.ArrayList;
import java.util.List;

public class ZelixNormalString extends Transformer {

    /* Written by https://github.com/Lampadina17 | 19/07/2024 */
    @Override
    public void transform(ClassWrapper scope, Context context) throws Exception {
        context.classes(scope).forEach(classWrapper -> {
            /* Decryption keys */
            int key1 = 0;
            List<Byte> key2 = new ArrayList();

            /* Ciphered strings */
            List<String> encryptedStrings = new ArrayList<>();

            /* Array Sizes */
            int arraySize = 0;

            /* Offsets */
            List<Integer> offsets = new ArrayList<>();

            for (MethodNode mn : classWrapper.methods()) {
                /* Retrieve first key from first cipher method */
                if (mn.desc.equals("(Ljava/lang/String;)[C")) {
                    for (AbstractInsnNode isn : mn.instructions.toArray()) {
                        if (isn instanceof IntInsnNode && isn.getNext() != null && isn.getNext() instanceof InsnNode && isn.getNext().getOpcode() == Opcodes.IXOR) {
                            IntInsnNode iin = (IntInsnNode) isn;
                            key1 = iin.operand;
                        }
                    }
                }
                /* Retrieve second key from first cipher method */
                if (mn.desc.equals("([C)Ljava/lang/String;")) {
                    for (AbstractInsnNode isn : mn.instructions.toArray()) {
                        if (isn instanceof IntInsnNode && isn.getPrevious() != null && isn.getPrevious() instanceof FrameNode) {
                            IntInsnNode iin = (IntInsnNode) isn;
                            key2.add((byte) iin.operand);
                        }
                    }
                }
                if (mn.name.equals("<clinit>")) {
                    /* Retrieve array length */
                    if (mn.instructions.getFirst() instanceof IntInsnNode && mn.instructions.getFirst().getNext() instanceof TypeInsnNode) {
                        arraySize = ((IntInsnNode) mn.instructions.getFirst()).operand;
                    }

                    /* Retrieve ciphered strings */
                    for (AbstractInsnNode isn : mn.instructions.toArray()) {
                        if (isn instanceof LdcInsnNode && ((LdcInsnNode) isn).cst instanceof String) {
                            String ciphered = (String) ((LdcInsnNode) isn).cst;
                            encryptedStrings.add(ciphered);
                        }
                    }

                    for (AbstractInsnNode isn : mn.instructions.toArray()) {
                        if (isn instanceof MethodInsnNode) {
                            if (((MethodInsnNode) isn).name.equals("length")) {
                                AbstractInsnNode next2 = isn.getNext().getNext();
                                if (next2 instanceof IntInsnNode) {
                                    IntInsnNode iin = (IntInsnNode) next2;
                                    offsets.add(iin.operand);
                                } else if (next2 instanceof InsnNode) {
                                    offsets.add(constToInt((InsnNode) next2));
                                }
                            }
                        }
                    }
                }
            }

            /* Convert arraylist to array (Key) */
            ZKMCipher cipher = new ZKMCipher();
            byte[] key2Array = new byte[key2.size()];
            for (int j = 0; j < key2.size(); j++) {
                key2Array[j] = key2.get(j);
            }

            if (encryptedStrings.size() == 2 && offsets.size() == 2) {
                /* for classes that has big static block */
                try {
                    for (MethodNode mn : classWrapper.methods()) {
                        for (AbstractInsnNode isn : mn.instructions.toArray()) {
                            if (isn.getOpcode() == AALOAD && isn.getPrevious().getPrevious().getOpcode() == ALOAD) {

                                int index = 0;
                                if (isn.getPrevious() instanceof IntInsnNode)
                                    index = ((IntInsnNode) isn.getPrevious()).operand;
                                else if (isn.getPrevious() instanceof InsnNode)
                                    index = constToInt((InsnNode) isn.getPrevious());
                                String[] decryptedStrings = cipher.StaticInit(encryptedStrings.get(0), encryptedStrings.get(1), key1, key2Array, offsets.get(0), offsets.get(1), arraySize);

                                /* Re-insert original string back to its place */
                                mn.instructions.insert(isn, new LdcInsnNode(decryptedStrings[index]));
                                /* Cleanup shits */
                                mn.instructions.remove(isn.getPrevious().getPrevious());
                                mn.instructions.remove(isn.getPrevious());
                                mn.instructions.remove(isn);
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    for (MethodNode mn : classWrapper.methods()) {
                        for (AbstractInsnNode isn : mn.instructions.toArray()) {
                            if (isn instanceof LdcInsnNode) {
                                LdcInsnNode ldc = (LdcInsnNode) isn;
                                if (ldc.cst instanceof String) {
                                    String string = (String) ldc.cst;
                                    String decrypted = cipher.cipher2(cipher.cipher1(string, key1), key2Array);
                                    ldc.cst = decrypted;
                                }
                            } else if (isn.getOpcode() == SWAP && isn.getNext() instanceof MethodInsnNode && isn.getNext().getNext() instanceof MethodInsnNode && isn.getNext().getNext().getNext().getOpcode() == SWAP) {
                                /* Do cleanup */
                                mn.instructions.remove(isn.getNext().getNext().getNext());
                                mn.instructions.remove(isn.getNext().getNext());
                                mn.instructions.remove(isn.getNext());
                                mn.instructions.remove(isn);
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public int constToInt(InsnNode in) {
        int opcode = in.getOpcode();
        switch (opcode) {
            case ICONST_M1:
                return -1;
            case ICONST_0:
                return 0;
            case ICONST_1:
                return 1;
            case ICONST_2:
                return 2;
            case ICONST_3:
                return 3;
            case ICONST_4:
                return 4;
            case ICONST_5:
                return 5;
        }
        return 0;
    }

    public class ZKMCipher {

        public char[] cipher1(final String var0, final int key) { // All old versions
            final char[] input = var0.toCharArray();
            if (input.length < 2) {
                input[0] ^= key;
            }
            return input;
        }

        public String cipher2(final char[] input, final byte[] keys) { // IDK ver, sometimes has more "case"
            for (int i = 0; input.length > i; ++i) {
                input[i] ^= keys[i % 7]; // TODO: dynamic size
            }
            return (new String(input)).intern();
        }

        public String[] StaticInit(String encrypted1, String encrypted2, int key1, byte[] key2, int offset, int offset2, int arraysize) {
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
                        final String a = cipher2(cipher1(s3, key1), key2);
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
