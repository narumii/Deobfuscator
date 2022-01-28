package uwu.narumi.deobfuscator.transformer.impl.radon;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.exception.TransformerException;
import uwu.narumi.deobfuscator.transformer.Transformer;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/*
    TODO: Make it better cuz sometimes doesn't work (and code is messy) :(
 */
public class RadonInvokeDynamicTransformer extends Transformer {

    private static final String FAST_BOOTSTRAP_DESC = "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;";
    private static final String BOOTSTRAP_DESC = "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;";

    private final Map<String, Map<Long, String>> methodNames = new HashMap<>();
    private final Map<String, Map<Long, String>> fieldNames = new HashMap<>();
    private String decryptName;

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        LOGGER.warn("# In order to fully deobfuscate invokedynamic you need provide all dependency that obfuscated jar using. (Not the case when jar is obfuscated with fast invokedynamic)");

        deobfuscator.classes().stream().flatMap(classNode -> classNode.methods.stream())
                .flatMap(methodNode -> Arrays.stream(methodNode.instructions.toArray()))
                .filter(node -> node instanceof InvokeDynamicInsnNode)
                .filter(node -> isLong(node.getPrevious()))
                .filter(node -> isString(node.getPrevious().getPrevious()))
                .map(InvokeDynamicInsnNode.class::cast)
                .filter(node -> node.bsm.getDesc().equals(BOOTSTRAP_DESC))
                .forEach(node -> {
                    String owner = getString(node.getPrevious().getPrevious());
                    if (deobfuscator.getClasses().containsKey(owner.replace('.', '/')))
                        compose(deobfuscator, owner.replace('.', '/'));
                    else
                        compose(owner.replace('/', '.'));
                });

        deobfuscator.classes().stream()
                .flatMap(classNode -> classNode.methods.stream())
                .forEach(methodNode -> Arrays.stream(methodNode.instructions.toArray())
                        .filter(node -> node instanceof InvokeDynamicInsnNode)
                        .map(InvokeDynamicInsnNode.class::cast)
                        .forEach(node -> {
                            if (node.bsm.getDesc().equals(FAST_BOOTSTRAP_DESC)) {
                                if (decryptName == null)
                                    init(deobfuscator, node.bsm);

                                if (decryptName == null)
                                    return;

                                String[] info = decrypt(node.name, node.bsm, decryptName).split("\u0000\u0000");
                                methodNode.instructions.set(node,
                                        new MethodInsnNode(info[3].equals("a") ? INVOKESTATIC : INVOKEVIRTUAL, info[0], info[1], info[2], false));
                            } else if (node.bsm.getDesc().equals(BOOTSTRAP_DESC) && isLong(node.getPrevious()) && isString(node.getPrevious().getPrevious())) {
                                String owner = getString(node.getPrevious().getPrevious()).replace('.', '/');
                                long hash = getLong(node.getPrevious());

                                String[] split = null;
                                if ((node.name.equals("a") || node.name.equals("b")) && methodNames.containsKey(owner) && methodNames.get(owner).containsKey(hash))
                                    split = methodNames.get(owner).get(hash).split("\u0000");
                                else if (node.name.charAt(0) > 'b' && fieldNames.containsKey(owner) && fieldNames.get(owner).containsKey(hash))
                                    split = fieldNames.get(owner).get(hash).split("\u0000");
                                else
                                    LOGGER.warn("Class {} not found, type: {}, hash: {}", owner, (node.name.equals("a") || node.name.equals("b")) ? "METHOD" : "FIELD", hash);

                                if (split != null) {
                                    methodNode.instructions.remove(node.getPrevious().getPrevious());
                                    methodNode.instructions.remove(node.getPrevious());

                                    set(methodNode, node, owner, split[0], split[1]);
                                }
                            }
                        }));

        methodNames.clear();
        fieldNames.clear();
    }

    private void set(MethodNode methodNode, InvokeDynamicInsnNode node, String owner, String name, String desc) {
        switch (node.name) {
            case "a": {
                methodNode.instructions.set(node, new MethodInsnNode(INVOKESTATIC, owner, name, desc, false));
                break;
            }
            case "b": {
                methodNode.instructions.set(node, new MethodInsnNode(INVOKEVIRTUAL, owner, name, desc, false));
                break;
            }
            case "d": {
                methodNode.instructions.set(node, new FieldInsnNode(GETSTATIC, owner, name, desc));
                break;
            }
            case "e": {
                methodNode.instructions.set(node, new FieldInsnNode(GETFIELD, owner, name, desc));
                break;
            }
            case "f": {
                methodNode.instructions.set(node, new FieldInsnNode(PUTSTATIC, owner, name, desc));
                break;
            }
            case "g": {
                methodNode.instructions.set(node, new FieldInsnNode(PUTFIELD, owner, name, desc));
                break;
            }
        }
    }

    private void compose(String owner) {
        try {
            compose0(Class.forName(owner.replace('/', '.'), false, getClassLoader()));
        } catch (Throwable throwable) {
            LOGGER.error("Cloud not find {} class, you need add libraries", owner);
        }
    }

    private void compose0(Class<?> clazz) {
        if (clazz == null)
            return;

        for (Method method : clazz.getMethods()) {
            long hash = hashMethod(method);
            String info = method.getName() + "\u0000" + Type.getType(method).getDescriptor();

            methodNames.computeIfAbsent(clazz.getName().replace('.', '/'), ignored -> new HashMap<>()).put(hash, info);
        }

        for (Field field : clazz.getFields()) {
            long hash = hashField(field);
            String info = field.getName() + "\u0000" + Type.getType(field.getType()).getDescriptor();

            fieldNames.computeIfAbsent(clazz.getName().replace('.', '/'), ignored -> new HashMap<>()).put(hash, info);
        }

        for (Class<?> anInterface : clazz.getInterfaces()) {
            compose0(anInterface);
        }

        compose0(clazz.getSuperclass());
    }

    private void compose(Deobfuscator deobfuscator, String owner) {
        owner = owner.replace('.', '/');
        compose0(deobfuscator, deobfuscator.getClasses().get(owner));
    }

    private void compose0(Deobfuscator deobfuscator, ClassNode classNode) {
        if (classNode == null)
            return;

        classNode.methods.forEach(methodNode -> {
            long hash = hashMethod(methodNode.name, methodNode.desc);
            String info = methodNode.name + "\u0000" + methodNode.desc;

            methodNames.computeIfAbsent(classNode.name, ignored -> new HashMap<>()).put(hash, info);
        });

        classNode.fields.forEach(fieldNode -> {
            long hash = hashField(fieldNode.name, fieldNode.desc);
            String info = fieldNode.name + "\u0000" + fieldNode.desc;

            fieldNames.computeIfAbsent(classNode.name, ignored -> new HashMap<>()).put(hash, info);
        });

        classNode.interfaces.forEach(anInterface -> compose0(deobfuscator, deobfuscator.getClasses().get(anInterface)));
        compose0(deobfuscator, deobfuscator.getClasses().get(classNode.superName));
    }

    private void init(Deobfuscator deobfuscator, Handle handle) {
        ClassNode classNode = deobfuscator.getClasses().get(handle.getOwner());
        if (classNode == null)
            throw new TransformerException();

        decryptName = classNode.methods.stream()
                .filter(methodNode -> methodNode.desc.equals("(Ljava/lang/String;)Ljava/lang/String;"))
                .filter(methodNode -> isAccess(methodNode.access, ACC_PRIVATE))
                .filter(methodNode -> isAccess(methodNode.access, ACC_STATIC))
                .map(methodNode -> methodNode.name)
                .findFirst().orElseThrow();
    }

    private String decrypt(String encrypted, Handle handle, String decryptName) {
        char[] encryptedChars = encrypted.toCharArray();
        char[] decryptedChars = new char[encryptedChars.length];

        for (int i = 0; i < encryptedChars.length; i++) {
            switch (i % 3) {
                case 0:
                    decryptedChars[i] = (char) (encryptedChars[i] ^ handle.getOwner().replace('/', '.').hashCode());
                    break;
                case 1:
                    decryptedChars[i] = (char) (encryptedChars[i] ^ handle.getName().hashCode());
                    break;
                default:
                    decryptedChars[i] = (char) (encryptedChars[i] ^ decryptName.hashCode());
                    break;
            }
        }

        return new String(decryptedChars);
    }

    private long hashField(Field field) {
        String name = field.getName();
        String desc = Type.getType(field.getType()).getDescriptor();
        return hashField(name, desc);
    }

    private long hashField(String name, String desc) {
        return (((long) hashField(desc) & 0xffffffffL) | (((long) name.hashCode()) << 32));
    }

    private int hashField(String sType) {
        Type type = Type.getType(sType);

        if (type.getSort() == Type.ARRAY)
            return type.getInternalName().replace('/', '.').hashCode();
        else
            return type.getClassName().hashCode();
    }

    private long hashMethod(Method method) {
        String name = method.getName();
        String desc = Type.getType(method).getDescriptor();
        return hashMethod(name, desc);
    }

    private long hashMethod(String name, String desc) {
        return (((long) hashMethod(desc) & 0xffffffffL) | (((long) name.hashCode()) << 32));
    }

    private int hashMethod(String methodDescriptor) {
        int hash = 0;

        Type[] types = Type.getArgumentTypes(methodDescriptor);

        for (Type type : types) {
            if (type.getSort() == Type.ARRAY)
                hash ^= type.getInternalName().replace('/', '.').hashCode();
            else
                hash ^= type.getClassName().hashCode();
        }

        Type returnType = Type.getReturnType(methodDescriptor);
        if (returnType.getSort() == Type.ARRAY)
            hash ^= returnType.getInternalName().replace('/', '.').hashCode();
        else
            hash ^= returnType.getClassName().hashCode();

        return hash;
    }

    private ClassLoader getClassLoader() {
        return getClass().getClassLoader();
    }
}
