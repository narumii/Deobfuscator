package uwu.narumi.deobfuscator.transformer.impl.qprotect.v1_9_10;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.transformer.Transformer;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @author SooStrator1136
 */
public class qProtectEnhancedStrings extends Transformer {

    private final Map<MethodNode, Method> methodCache = new HashMap<>(1);

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        for (ClassNode classNode : deobfuscator.classes()) {
            MethodNode clinit = findClInit(classNode).orElse(null);
            if (clinit == null || clinit.instructions.size() < 11) continue;
            if (!isInteger(clinit.instructions.get(0)) || !(clinit.instructions.get(2) instanceof FieldInsnNode)) continue;

            FieldNode arr1Field = findField(classNode, (FieldInsnNode) clinit.instructions.get(2)).orElse(null);
            if (arr1Field == null) continue;

            int arrSize = getInteger(clinit.instructions.get(0));
            if (arrSize == 0) continue;

            String[] arr1 = new String[arrSize];
            String[] arr2 = new String[arrSize];

            int insnCount = clinit.instructions.size();

            int counter = 0;
            for (int i = 6; i < insnCount; i += 4) {
                AbstractInsnNode indexNode = clinit.instructions.get(i);
                if (!isInteger(indexNode)) break;
                Optional<LdcInsnNode> strHolder = getSafe(clinit.instructions, i + 1, LdcInsnNode.class);
                if (strHolder.isEmpty() || !(strHolder.get().cst instanceof String str)) break;
                arr2[getInteger(indexNode)] = str;

                counter++;
            }

            if (counter != arrSize) continue;

            Optional<FieldInsnNode> arr2Insn = getSafe(clinit.instructions, 6 + (4 * (counter - 1)) + 3, FieldInsnNode.class);
            if (arr2Insn.isEmpty()) continue;
            FieldNode arr2Field = findField(classNode, arr2Insn.get()).orElse(null);
            if (arr2Field == null) continue;

            MethodNode decryptionMethod = getDecryptionMethod(classNode);
            if (decryptionMethod == null) continue;

            for (MethodNode methodNode : classNode.methods) {
                for (AbstractInsnNode insnNode : methodNode.instructions.toArray()) {
                    if (!(insnNode instanceof MethodInsnNode invokeInsn)) continue;

                    Optional<MethodNode> invoked = findMethod(classNode, invokeInsn);
                    if (invoked.isEmpty() || decryptionMethod != invoked.get()) continue;
                    if (!isInteger(insnNode.getPrevious().getPrevious())) continue;
                    if (!isInteger(insnNode.getPrevious())) continue;

                    String decrypted = decrypt(
                            arr1,
                            arr2,
                            getInteger(insnNode.getPrevious().getPrevious()),
                            getInteger(insnNode.getPrevious()),
                            decryptionMethod,
                            arr1Field
                    );
                    methodNode.instructions.remove(insnNode.getPrevious().getPrevious());
                    methodNode.instructions.remove(insnNode.getPrevious());
                    methodNode.instructions.set(insnNode, new LdcInsnNode(decrypted));
                }
            }

            classNode.methods.remove(decryptionMethod);
            classNode.fields.remove(arr1Field);
            classNode.fields.remove(arr2Field);
            int lastClinitInsn = clinit.instructions.indexOf(arr2Insn.get());
            for (int i = -1; i < lastClinitInsn; i++) {
                clinit.instructions.remove(clinit.instructions.get(0));
            }
        }
    }

    private static MethodNode getDecryptionMethod(ClassNode classNode) {
        for (MethodNode methodNode : classNode.methods) {
            if (!methodNode.desc.equals("(II)Ljava/lang/String;")) continue;
            if (methodNode.instructions.size() != 886) continue;
            if (!(methodNode.instructions.get(20) instanceof TableSwitchInsnNode)) continue;
            if (!isInteger(methodNode.instructions.get(1))) continue;

            return methodNode;
        }

        return null;
    }

    //FIXME lazy + ugly + risky
    private String decrypt(
            String[] arr1, String[] arr2, int arg1, int arg2, MethodNode method, FieldNode origField1
    ) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException, NoSuchFieldException {
        Method decrypt = methodCache.get(method);

        if (decrypt == null) {
            ClassNode classNode = new ClassNode();
            classNode.version = V1_8;
            classNode.access = ACC_PUBLIC;
            classNode.name = MethodHandles.lookup().lookupClass().getPackageName().replace('.', '/') + "/LazyQp" + classNode.hashCode();
            classNode.superName = "java/lang/Object";

            FieldNode arr1Field = new FieldNode(ACC_PUBLIC | ACC_STATIC, "honestly", "[Ljava/lang/String;", null, null);
            FieldNode arr2Field = new FieldNode(ACC_PUBLIC | ACC_STATIC, "quite", "[Ljava/lang/String;", null, null);

            classNode.fields.add(arr1Field);
            classNode.fields.add(arr2Field);

            MethodNode lazy = new MethodNode(
                    ACC_STATIC | ACC_PUBLIC,
                    "incredible",
                    method.desc,
                    null,
                    null
            );
            lazy.instructions = method.instructions;

            for (AbstractInsnNode insnNode : lazy.instructions.toArray()) {
                if (!(insnNode instanceof FieldInsnNode fieldInsn)) continue;

                fieldInsn.owner = classNode.name;
                fieldInsn.name = origField1.name.equals(fieldInsn.name) ? arr1Field.name : arr2Field.name;
            }

            classNode.methods.add(lazy);
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            classNode.accept(writer);
            Class<?> clazz = MethodHandles.lookup().defineClass(writer.toByteArray());
            decrypt = clazz.getMethod("incredible", int.class, int.class);
            methodCache.put(method, decrypt);
            clazz.getField("honestly").set(null, arr1);
            clazz.getField("quite").set(null, arr2);
        }

        return (String) decrypt.invoke(null, arg1, arg2);
    }

    private static <T extends AbstractInsnNode> Optional<T> getSafe(InsnList insnList, int index, Class<T> expectedType) {
        if (insnList.size() < index) return Optional.empty();

        AbstractInsnNode insnNode = insnList.get(index);
        if (!insnNode.getClass().isAssignableFrom(expectedType)) return Optional.empty();

        return Optional.of((T) insnNode);
    }

}
