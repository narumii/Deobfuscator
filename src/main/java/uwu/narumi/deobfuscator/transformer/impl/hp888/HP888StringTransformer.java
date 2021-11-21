package uwu.narumi.deobfuscator.transformer.impl.hp888;

import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.helper.ASMHelper;
import uwu.narumi.deobfuscator.transformer.Transformer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class HP888StringTransformer extends Transformer {

    /*
    Takie potezne stringi ze az sie spocilem
     */
    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        deobfuscator.classes().forEach(classNode -> {
            List<MethodNode> toRemove = new ArrayList<>();

            classNode.methods.forEach(methodNode -> Arrays.stream(methodNode.instructions.toArray())
                    .filter(node -> node.getOpcode() == INVOKESTATIC)
                    .filter(node -> isString(node.getPrevious())) //encrypted
                    .map(MethodInsnNode.class::cast)
                    .filter(node -> node.owner.equals(classNode.name))
                    .filter(node -> node.desc.equals("(Ljava/lang/String;)Ljava/lang/String;"))
                    .forEach(node -> findMethod(classNode, method -> method.name.equals(node.name) && method.desc.equals(node.desc)).ifPresent(method -> {
                        getKey(method).ifPresent(key -> {
                            String string = getString(node.getPrevious());
                            String className = classNode.name;
                            String firstMethod = method.name;
                            String secondMethod = methodNode.name;

                            methodNode.instructions.remove(node.getPrevious());
                            methodNode.instructions.set(node, new LdcInsnNode(decrypt(string, className, firstMethod, secondMethod, key)));
                            toRemove.add(method);
                        });
                    })));

            classNode.methods.removeAll(toRemove);
            toRemove.clear();
        });
    }

    private Optional<Integer> getKey(MethodNode methodNode) {
        return Arrays.stream(methodNode.instructions.toArray())
                .filter(ASMHelper::isInteger)
                .filter(node -> node.getNext().getOpcode() == IXOR)
                .filter(node -> node.getNext().getNext().getOpcode() == I2C)
                .filter(node -> node.getNext().getNext().getNext().getOpcode() == CASTORE)
                .filter(node -> node.getPrevious().getOpcode() == IXOR)
                .map(ASMHelper::getInteger)
                .findFirst();
    }


    private String decrypt(String string, String firstClass, String firstMethod, String zeroMethod, int key) {
        char[] chars = string.toCharArray();

        for (int i = 0; i < chars.length; ++i) {
            chars[i] =
                    (char) (chars[i]
                            ^ firstClass.hashCode()
                            ^ firstMethod.hashCode()
                            ^ zeroMethod.hashCode()
                            ^ key);
        }

        return new String(chars);
    }
}
