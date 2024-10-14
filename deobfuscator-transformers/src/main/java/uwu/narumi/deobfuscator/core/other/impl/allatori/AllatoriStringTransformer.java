package uwu.narumi.deobfuscator.core.other.impl.allatori;

import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class AllatoriStringTransformer extends Transformer {

    private HashMap<String, DecryptionMethod> decryptors = new HashMap<>();

    private boolean strong;

    public AllatoriStringTransformer(boolean strong) {
        this.strong = strong;
    }

    /* Written by https://github.com/Lampadina17 | 06/08/2024 */
    /* use UniversalNumberTransformer before this transformer to decrypt keys */
    @Override
    protected void transform() throws Exception {
        scopedClasses().forEach(classWrapper -> {
            classWrapper.methods().forEach(methodNode -> {

                AtomicBoolean isDecryptor = new AtomicBoolean(false);

                /* Find decryption methods */
                if (methodNode.desc.equals("(Ljava/lang/String;)Ljava/lang/String;")) {
                    Arrays.stream(methodNode.instructions.toArray()).forEach(ain -> {
                        if (ain instanceof MethodInsnNode min) {
                            if (min.name.equals("length") && min.owner.equals("java/lang/String") && min.desc.equals("()I")) {
                                isDecryptor.set(true);
                            }
                        }
                    });
                }
                if (isDecryptor.get()) {
                    /* Extract possible keys */
                    List<Integer> possibleKeys = new ArrayList<>();
                    Arrays.stream(methodNode.instructions.toArray())
                            .filter(node -> node instanceof IntInsnNode)
                            .map(IntInsnNode.class::cast)
                            .forEach(node -> possibleKeys.add(node.operand));

                    /* Filter all possible keys */
                    Arrays.stream(methodNode.instructions.toArray())
                            .filter(node -> node instanceof InsnNode)
                            .filter(node -> node.getNext() != null && node.getNext().getOpcode() == ISTORE)
                            .forEach(node -> {
                                if (node.getOpcode() == POP) {
                                    decryptors.put(classWrapper.name(), new DecryptionMethod(classWrapper.name(), new int[]{possibleKeys.get(1), possibleKeys.get(2)}));
                                } else if (node.getOpcode() == POP2) {
                                    decryptors.put(classWrapper.name(), new DecryptionMethod(classWrapper.name(), new int[]{possibleKeys.get(0), possibleKeys.get(2)}));
                                }
                            });
                }
            });
        });

        /* Decrypt all strings */
        scopedClasses().forEach(classWrapper -> {
            classWrapper.methods().forEach(methodNode -> {
                Arrays.stream(methodNode.instructions.toArray()).forEach(node -> {
                    if (node instanceof LdcInsnNode ldc && ldc.cst instanceof String && node.getNext() instanceof MethodInsnNode next && next.getOpcode() == INVOKESTATIC) {
                        DecryptionMethod dec1 = decryptors.get(next.owner);

                        /* Decrypt and remove double encryption (Strong) */
                        if (strong && next.getNext() instanceof MethodInsnNode nextnext) {
                            DecryptionMethod dec2 = decryptors.get(nextnext.owner);
                            ldc.cst = dec2.v4weak(dec1.v4strong((String) ldc.cst, methodNode.name + classWrapper.name().replace("/", ".")));
                            methodNode.instructions.remove(nextnext);
                            methodNode.instructions.remove(next);
                            this.markChange();
                            return;
                        }

                        /* Decrypt and remove encryption (Fast/Strong) */
                        if (dec1 != null) {
                            if (strong) {
                                ldc.cst = dec1.v4strong((String) ldc.cst, methodNode.name + classWrapper.name().replace("/", "."));
                            } else {
                                ldc.cst = dec1.v4weak((String) ldc.cst);
                            }
                            /* Remove invoke */
                            methodNode.instructions.remove(next);
                            this.markChange();
                        }
                    }
                });
            });
        });
        LOGGER.info("Decrypted {} strings in {} classes", this.getChangesCount(), scopedClasses().size());
    }

    public class DecryptionMethod {

        private String owner;
        private int[] keys;

        public DecryptionMethod(String owner, int[] keys) {
            this.owner = owner;
            this.keys = keys;
        }

        /* V4 Weak */
        public String v4weak(String input) {
            int i = input.length();
            char[] a = new char[i];
            int i0 = i - 1;
            while (true) {
                if (i0 >= 0) {
                    int i1 = input.charAt(i0);
                    int i2 = i0 + -1;
                    int i3 = (char) (i1 ^ keys[0]);
                    a[i0] = (char) i3;
                    if (i2 >= 0) {
                        i0 = i2 + -1;
                        int i4 = input.charAt(i2);
                        int i5 = (char) (i4 ^ keys[1]);
                        a[i2] = (char) i5;
                        continue;
                    }
                }
                return new String(a);
            }
        }

        /* V4 Strong */
        public String v4strong(String input, String context) {
            int n;
            int n2 = input.length();
            int n3 = n2 - 1;
            char[] cArray = new char[n2];
            int n6 = n = context.length() - 1;
            int n7 = n3;
            String string2 = context;
            while (n7 >= 0) {
                int n8 = n3--;
                cArray[n8] = (char) (keys[0] ^ (input.charAt(n8) ^ string2.charAt(n)));
                if (n3 < 0) return new String(cArray);
                int n9 = n3--;
                char c = cArray[n9] = (char) (keys[1] ^ (input.charAt(n9) ^ string2.charAt(n)));
                if (--n < 0) {
                    n = n6;
                }
                n7 = n3;
            }
            return new String(cArray);
        }
    }
}
