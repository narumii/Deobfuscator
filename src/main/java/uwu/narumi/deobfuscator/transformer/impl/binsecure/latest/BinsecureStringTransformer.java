package uwu.narumi.deobfuscator.transformer.impl.binsecure.latest;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.helper.ASMHelper;
import uwu.narumi.deobfuscator.helper.ClassHelper;
import uwu.narumi.deobfuscator.sandbox.SandBox;
import uwu.narumi.deobfuscator.transformer.Transformer;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

public class BinsecureStringTransformer extends Transformer {

    private final String decryptClassName;
    private final String mapClassName;

    public BinsecureStringTransformer() {
        this("c", "0");
    }

    public BinsecureStringTransformer(String decryptClassName, String mapClassName) {
        this.decryptClassName = decryptClassName;
        this.mapClassName = mapClassName;
    }

    /*
        Speed as fuck boiiii
     */
    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        ClassNode mapClass = deobfuscator.getOriginalClasses().get(mapClassName);
        ClassNode decryptClass = ClassHelper.copy(deobfuscator.getOriginalClasses().get(decryptClassName));
        if (mapClass == null || decryptClass == null)
            return;

        int key = getKey(decryptClass);

        decryptClass.methods.removeIf(methodNode -> !methodNode.name.startsWith("<"));
        SandBox sandBox = SandBox.of(mapClass, decryptClass);

        String fieldName = decryptClass.fields.stream().filter(node -> node.desc.equals("[I")).map(node -> node.name).findFirst().orElse("aiooi1iojionlknzjsdnfdas");
        int[] keys = (int[]) sandBox.get(decryptClass).get(fieldName, null);

        deobfuscator.classes().forEach(classNode -> classNode.methods.forEach(methodNode -> Arrays.stream(methodNode.instructions.toArray())
                .filter(ASMHelper::isString)
                .filter(node -> node.getNext() instanceof MethodInsnNode)
                .map(node -> (MethodInsnNode) node.getNext())
                .filter(node -> node.owner.equals(decryptClass.name))
                .forEach(node -> {
                    String string = getString(node.getPrevious());
                    int classHash = classNode.name.replace('/', '.').hashCode();
                    int methodHash = methodNode.name.replace('/', '.').hashCode();

                    methodNode.instructions.remove(node.getPrevious());
                    methodNode.instructions.set(node, new LdcInsnNode(decrypt(string, key, classHash, methodHash, keys)));
                })));
    }

    private int getKey(ClassNode classNode) {
        AtomicInteger key = new AtomicInteger();
        classNode.methods.stream()
                .filter(methodNode -> methodNode.desc.equals("(Ljava/lang/String;I)Ljava/lang/String;"))
                .findFirst().flatMap(methodNode -> Arrays.stream(methodNode.instructions.toArray())
                .filter(ASMHelper::isInteger)
                .filter(node -> check(node.getNext(), ISTORE))
                .filter(node -> check(node.getNext().getNext(), ISTORE))
                .filter(node -> check(node.getNext().getNext().getNext(), ICONST_M1))
                .filter(node -> check(node.getNext().getNext().getNext().getNext(), ACONST_NULL))
                .findFirst()).ifPresent(node -> key.set(ASMHelper.getInteger(node)));

        return key.get();
    }

    /*
        Fuck kotlin
     */
    private String decrypt(String string, int key, int classHash, int methodHash, int[] keys) {
        char[] chars = string.toCharArray();

        for (int i = 0; i < chars.length; i++) {
            int hash = -1;
            switch (i % 5) {
                case 0:
                    hash = 4 + classHash;
                    break;
                case 1:
                    hash = key;
                    break;
                case 2:
                    hash = classHash;
                    break;
                case 3:
                    hash = methodHash;
                    break;
                case 4:
                    hash = methodHash + classHash;
                    break;
                case 5:
                    hash = i + methodHash;
                    break;
            }

            chars[i] ^= (hash) ^ keys[i % keys.length];
        }

        return new String(chars);
    }
}