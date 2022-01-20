package uwu.narumi.deobfuscator.transformer.impl.paramorphism;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.exception.TransformerException;
import uwu.narumi.deobfuscator.helper.ASMHelper;
import uwu.narumi.deobfuscator.transformer.Transformer;

import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class ParamorphismInvokeDynamicTransformer extends Transformer {

    //private final String bootstrapClassName; //class that contains "lookups" field and has string in constructor
    private final Map<Integer, String[][]> lookups = new HashMap<>();

    /*public ParamorphismInvokeDynamicTransformer(String bootstrapClassName) {
        this.bootstrapClassName = bootstrapClassName;
    }*/

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        //ClassNode bootstrapClass = deobfuscator.getOriginalClasses().get(bootstrapClassName);
        ClassNode bootstrapClass = searchForBootstrapClass(deobfuscator);

        //if (bootstrapClass == null)
        //    throw new TransformerException("Class not found");

        StringBuilder key = new StringBuilder();
        findMethod(bootstrapClass, methodNode -> methodNode.name.equals("<init>")).ifPresent(methodNode -> Arrays.stream(methodNode.instructions.toArray())
                .filter(ASMHelper::isString)
                .map(ASMHelper::getString)
                .forEach(key::append));

        if (key.toString().isBlank())
            throw new TransformerException("Base64 String not found");

        decode(Base64.getDecoder().decode(key.toString()));

        deobfuscator.classes().forEach(classNode -> classNode.methods.forEach(methodNode -> Arrays.stream(methodNode.instructions.toArray())
                .filter(node -> node instanceof InvokeDynamicInsnNode)
                .map(InvokeDynamicInsnNode.class::cast)
                .filter(node -> node.name.equals("call"))
                .forEach(node -> {
                    int hash = classNode.name.replace('/', '.').hashCode() * 31 + methodNode.name.hashCode();
                    int position = (int) ((long) node.bsmArgs[0] & 4294967295L);
                    int type = (int) node.bsmArgs[1];

                    switch (type) {
                        case 1:
                            type = INVOKESTATIC;
                            break;
                        case 2:
                            type = INVOKEVIRTUAL;
                            break;
                        case 3:
                            type = INVOKESPECIAL;
                            break;
                    }

                    if (!lookups.containsKey(hash))
                        return;

                    /*
                    Interface check for runnable deobf? maybe
                     */
                    String[] info = lookups.get(hash)[position];
                    methodNode.instructions.set(node,
                            new MethodInsnNode(
                                    type,
                                    info[0].replace('.', '/'),
                                    info[1],
                                    node.desc,
                                    false
                            ));
                })));

        lookups.clear();
        deobfuscator.getClasses().remove(bootstrapClass.name);
        deobfuscator.getClasses().remove(bootstrapClass.name.substring(0, bootstrapClass.name.indexOf('⛔') + 1));
        deobfuscator.getClasses().remove(bootstrapClass.name.substring(0, bootstrapClass.name.lastIndexOf('/') + 1) + "Dispatcher️");
    }

    private void decode(byte[] byArray) {
        int n = 0;
        int n2 = (byArray[n++] & 0xFF) << 24 | ((byArray[n++] & 0xFF) << 16 | ((byArray[n++] & 0xFF) << 8 | (byArray[n++] & 0xFF)));
        for (int i = 0; i < n2; ++i) {
            int n3 = (byArray[n++] & 0xFF) << 24 | ((byArray[n++] & 0xFF) << 16 | ((byArray[n++] & 0xFF) << 8 | (byArray[n++] & 0xFF)));
            int n4 = (byArray[n++] & 0xFF) << 24 | ((byArray[n++] & 0xFF) << 16 | ((byArray[n++] & 0xFF) << 8 | (byArray[n++] & 0xFF)));
            String[][] stringArrayArray = new String[n4][];
            this.lookups.put(n3, stringArrayArray);
            for (int j = 0; j < n4; ++j) {
                int n5 = (byArray[n++] & 0xFF) << 8 | byArray[n++] & 0xFF;
                int n6 = 0;
                do {
                    byArray[n + n6] = (byte) (byArray[n + n6] ^ 0xAA);
                } while (++n6 < n5);
                String string = new String(byArray, n, n5);
                n += n5;
                int n7 = (byArray[n++] & 0xFF) << 8 | byArray[n++] & 0xFF;
                n6 = 0;
                do {
                    byArray[n + n6] = (byte) (byArray[n + n6] ^ 0xAA);
                } while (++n6 < n7);
                String string2 = new String(byArray, n, n7);
                n += n7;
                stringArrayArray[j] = new String[]{string, string2};
            }
        }
    }

    private ClassNode searchForBootstrapClass(Deobfuscator deobfuscator) {
        return deobfuscator.classes().stream()
                .filter(classNode -> classNode.name.endsWith("⛔$0"))
                .findFirst()
                .orElseThrow();
    }
}