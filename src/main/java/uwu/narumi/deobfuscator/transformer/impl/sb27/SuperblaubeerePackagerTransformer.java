package uwu.narumi.deobfuscator.transformer.impl.sb27;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.helper.ClassHelper;
import uwu.narumi.deobfuscator.transformer.Transformer;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/*
    TODO: Main class gathering and META-INF parsing itd...
 */
public class SuperblaubeerePackagerTransformer extends Transformer {

    private static byte[] decryptKey;

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        deobfuscator.classes().stream()
                .filter(classNode -> classNode.superName.equals("java/lang/ClassLoader"))
                .filter(classNode -> classNode.fields != null)
                .filter(classNode -> !classNode.fields.isEmpty())
                .filter(classNode -> classNode.fields.stream().anyMatch(fieldNode -> fieldNode.desc.equals("[B")))
                .forEach(classNode -> {
                    List<FieldNode> fields = classNode.fields.stream().filter(fieldNode -> fieldNode.desc.equals("[B")).collect(Collectors.toList());
                    if (fields.size() != 1) //i don't wanna deal with it now
                        throw new IllegalArgumentException("FUCK XD");


                    FieldNode fieldNode = fields.get(0);
                    findClInit(classNode).ifPresent(clInit -> Arrays.stream(clInit.instructions.toArray())
                            .filter(node -> node.getOpcode() == NEWARRAY) //yeah we can add one more filter but i'm lazy as fuck
                            .filter(node -> isInteger(node.getPrevious()))
                            .forEach(node -> {
                                AbstractInsnNode current = node;
                                byte[] key = new byte[getInteger(node.getPrevious())];

                                for (int i = 0; i < key.length; i++) {
                                    int position = getInteger(current.getNext().getNext());
                                    int value = getInteger(current.getNext().getNext().getNext());

                                    key[position] = (byte) value;
                                    current = current.getNext().getNext().getNext().getNext();
                                }

                                if (!(current.getNext() instanceof FieldInsnNode)) //i don't wanna deal with it now
                                    throw new IllegalArgumentException("FUCK XD V2");

                                FieldInsnNode fieldInsnNode = (FieldInsnNode) current.getNext();
                                if (fieldNode.name.equals(fieldInsnNode.name) && classNode.name.equals(fieldInsnNode.owner)) {
                                    decryptKey = key;
                                }
                            }));

                    if (decryptKey != null)
                        deobfuscator.getClasses().remove(classNode.name);
                });

        if (decryptKey != null) {
            deobfuscator.getFiles().forEach((name, bytes) -> {
                try {
                    bytes = decrypt(bytes, decryptKey);
                    if (!ClassHelper.isClass("ignored.class", bytes))
                        return;

                    ClassNode classNode = ClassHelper.loadClass(bytes, deobfuscator.getClassReaderFlags());
                    deobfuscator.getClasses().put(classNode.name, classNode);
                    deobfuscator.getFiles().remove(name);
                } catch (Exception e) {
                    LOGGER.error("Could not load class: {}", name);
                    LOGGER.debug("Error", e);
                }
            });
        }
    }

    private byte[] decrypt(byte[] toDecrypt, byte[] key) {
        byte[] bytes = new byte[toDecrypt.length];

        for (int i = 0; i < toDecrypt.length; ++i) {
            bytes[i] = (byte) (toDecrypt[i] ^ key[i % key.length]);
        }

        return bytes;
    }
}
