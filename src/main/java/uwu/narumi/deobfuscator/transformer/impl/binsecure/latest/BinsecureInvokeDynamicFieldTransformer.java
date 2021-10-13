package uwu.narumi.deobfuscator.transformer.impl.binsecure.latest;

import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.transformer.Transformer;

import java.util.Arrays;

public class BinsecureInvokeDynamicFieldTransformer extends Transformer {

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        deobfuscator.classes().forEach(classNode -> classNode.methods.forEach(methodNode -> Arrays.stream(methodNode.instructions.toArray())
                .filter(node -> node instanceof InvokeDynamicInsnNode)
                .map(InvokeDynamicInsnNode.class::cast)
                .filter(node -> node.bsm.getDesc().equals("(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/Class;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/invoke/CallSite;"))
                .filter(node -> node.bsmArgs.length == 4)
                .forEach(node -> {
                    String encryptKey = classNode.name.replace("/", ".") + methodNode.name;
                    String owner = encryptStr(encryptKey, (String) node.bsmArgs[1]).replace('.', '/');
                    String desc = node.desc;
                    String name = encryptStr(encryptKey, (String) node.bsmArgs[3]);
                    boolean virtual = ((String) node.bsmArgs[2]).indexOf('.') != -1;

                    String newDesc = null;
                    int opcode = -1;

                    if (virtual && desc.endsWith(")V")) {
                        opcode = PUTFIELD;
                        newDesc = desc.substring(desc.indexOf(';') + 1, desc.length() - 2);
                    } else if (virtual && !desc.endsWith(")V")) {
                        opcode = GETFIELD;
                        newDesc = desc.substring(desc.indexOf(')') + 1);
                    } else if (!virtual && desc.endsWith(")V")) {
                        opcode = PUTSTATIC;
                        newDesc = desc.substring(1, desc.length() - 2);
                    } else if (!virtual && !desc.endsWith(")V")) {
                        opcode = GETSTATIC;
                        newDesc = desc.substring(2);
                    }

                    if (opcode == -1)
                        return;

                    methodNode.instructions.set(node, new FieldInsnNode(opcode, owner, name, newDesc));
                })));
    }

    /*
        Fuck kotlin
     */
    private String encryptStr(String key, String value) {
        int keyLength = key.length();
        int length = value.length();
        StringBuilder out = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            char newC = (char) ((value.charAt(i) ^ 0x2468ACE1) ^ key.charAt(i % keyLength));
            out.append(newC);
        }
        return out.toString();
    }
}
