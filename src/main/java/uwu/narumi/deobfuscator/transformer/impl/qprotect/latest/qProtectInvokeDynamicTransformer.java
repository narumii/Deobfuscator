package uwu.narumi.deobfuscator.transformer.impl.qprotect.latest;

import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.transformer.Transformer;

import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;

public class qProtectInvokeDynamicTransformer extends Transformer {

    public static MethodInsnNode decrypt(Object info, /*Object methodType,*/ Object access, Object key) {

        String decodedInfo = (String) info;
        decodedInfo = new String(Base64.getDecoder().decode(decodedInfo));
        decodedInfo = new String(Base64.getDecoder().decode(decodedInfo));

        char[] firstChars = ((String) key).toCharArray();
        char[] outChars = new char[decodedInfo.length()];
        char[] secondChars = decodedInfo.toCharArray();

        for (int i = 0; i < decodedInfo.length(); ++i) {
            outChars[i] = (char) (secondChars[i] ^ firstChars[i % firstChars.length]);
        }

        decodedInfo = new String(Base64.getDecoder().decode(new String(outChars)));
        String[] parts = decodedInfo.split("<>");
        String className = parts[0].replace('.', '/');
        String invocationName = parts[1];
        String desc = parts[2];
        String specialCaller = parts[3].replace('.', '/');

        int type = (int) access;
        int opcode = type == -889275714 ? INVOKESTATIC : (type == -559038737 ? INVOKEVIRTUAL : INVOKESPECIAL);

        return new MethodInsnNode(opcode, type == -559038242 ? specialCaller : className, invocationName, desc, false);
    }

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        AtomicReference<String> className = new AtomicReference<>();
        AtomicReference<String> methodName = new AtomicReference<>();

        deobfuscator.classes().stream()
                .flatMap(classNode -> classNode.methods.stream())
                .forEach(methodNode -> Arrays.stream(methodNode.instructions.toArray())
                        .filter(node -> node instanceof InvokeDynamicInsnNode)
                        .map(InvokeDynamicInsnNode.class::cast)
                        .filter(node -> node.bsm.getDesc().equals("(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
                        .filter(node -> node.bsmArgs.length == 2)
                        .forEach(node -> {
                            String name = node.name;
                            //String desc = node.desc;
                            Object type = node.bsmArgs[0];
                            String key = (String) node.bsmArgs[1];

                            methodNode.instructions.set(node, decrypt(name, /*desc,*/ type, key));

                            if (className.get() == null) {
                                className.set(node.bsm.getOwner());
                                methodName.set(node.bsm.getName());
                            }
                        }));

        if (className.get() != null) {
            deobfuscator.getClasses().get(className.get()).methods
                    .removeIf(methodNode -> methodNode.name.equals(methodName.get()) && methodNode.desc.equals("(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"));
        }
    }
}
