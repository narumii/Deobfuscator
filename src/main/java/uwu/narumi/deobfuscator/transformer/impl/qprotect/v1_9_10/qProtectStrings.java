package uwu.narumi.deobfuscator.transformer.impl.qprotect.v1_9_10;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.transformer.Transformer;

/**
 * @author SooStrator1136
 */
public class qProtectStrings extends Transformer {

    private static final String STR_ARR_DESC = Type.getType(String[].class).getDescriptor();

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        for (ClassNode classNode : deobfuscator.classes()) {
            MethodNode arrayInitMethod = findMethod(
                    classNode,
                    (method) -> {
                        if (!method.desc.equals("()V")) return false;
                        if (method.instructions.size() < 5) return false;
                        if (!isInteger(method.instructions.get(0))) return false;
                        if (!(method.instructions.get(1) instanceof TypeInsnNode newArrInsn)) return false;
                        if (!newArrInsn.desc.equals("java/lang/String")) return false;
                        if (!(method.instructions.get(2) instanceof FieldInsnNode putNode)) return false;
                        if (!putNode.owner.equals(classNode.name) || !putNode.desc.equals(STR_ARR_DESC)) return false;
                        if (!(method.instructions.get(3) instanceof FieldInsnNode getNode)) return false;
                        if (!getNode.owner.equals(classNode.name) || !getNode.desc.equals(STR_ARR_DESC)) return false;
                        return isInteger(method.instructions.get(4));
                    }
            ).orElse(null);

            if (arrayInitMethod == null) continue;

            int arraySize = getInteger(arrayInitMethod.instructions.get(0));
            String[] decrypted = new String[arraySize];

            int insnCount = arrayInitMethod.instructions.size();
            for (int i = 4; i < insnCount; i += 11) {
                decrypted[getInteger(arrayInitMethod.instructions.get(i))] = decryptString(
                        getString(arrayInitMethod.instructions.get(i + 1)),
                        getString(arrayInitMethod.instructions.get(i + 2)),
                        getString(arrayInitMethod.instructions.get(i + 3)),
                        getInteger(arrayInitMethod.instructions.get(i + 4)),
                        getInteger(arrayInitMethod.instructions.get(i + 5)),
                        getInteger(arrayInitMethod.instructions.get(i + 6)),
                        getInteger(arrayInitMethod.instructions.get(i + 7))
                );
            }

            FieldInsnNode strArray = (FieldInsnNode) arrayInitMethod.instructions.get(3);

            for (MethodNode methodNode : classNode.methods) {
                if (methodNode == arrayInitMethod) continue;

                insnCount = methodNode.instructions.size();
                AbstractInsnNode[] insnArray = methodNode.instructions.toArray();
                for (int i = 0; i < insnCount; i++) {
                    AbstractInsnNode currNode = insnArray[i];
                    if (!(currNode instanceof FieldInsnNode fieldNode) || !isSameFieldInsnNode(fieldNode, strArray)) continue;

                    String actualStr = decrypted[getInteger(currNode.getNext())];

                    methodNode.instructions.remove(currNode.getNext().getNext());
                    methodNode.instructions.remove(currNode.getNext());
                    methodNode.instructions.set(currNode, new LdcInsnNode(actualStr));
                }
            }

            classNode.methods.remove(arrayInitMethod);
            classNode.methods.remove(findMethod(classNode, (MethodInsnNode) arrayInitMethod.instructions.get(12)).get());
            classNode.fields.remove(findField(classNode, (field) -> field.name.equals(strArray.name) && field.desc.equals(strArray.desc)).get());
            MethodNode clinit = findClInit(classNode).get();
            for (AbstractInsnNode insnNode : clinit.instructions.toArray()) {
                if (!(insnNode instanceof MethodInsnNode invokeInsn) || !invokeInsn.desc.equals("()V")) continue;
                if (!invokeInsn.owner.equals(classNode.name) || !invokeInsn.name.equals(arrayInitMethod.name)) continue;
                clinit.instructions.remove(insnNode);
                break;
            }
        }
    }

    private static boolean isSameFieldInsnNode(FieldInsnNode nodeA, FieldInsnNode nodeB) {
        if (nodeA.getOpcode() != nodeB.getOpcode()) return false;
        return nodeA.desc.equals(nodeB.desc) && nodeA.owner.equals(nodeB.owner) && nodeA.name.equals(nodeB.name);
    }

    private static String decryptString(String str1, String str2, String str3, int int1, int int2, int int3, int int4) {
        char[] charArray = str3.toCharArray();
        int charArrayLength = charArray.length;
        char[] value = new char[charArrayLength];
        char[] charArray2 = str2.toCharArray();
        for (int i = 0; i < charArrayLength; ++i) {
            value[i] = (char) (charArray[i] ^ charArray2[i % charArray2.length]);
        }
        int hashCode = new String(value).hashCode();
        int n5 = int2 - int4 - int1;
        char[] charArray3 = str1.toCharArray();
        int charArray3Length = charArray3.length;
        char[] value2 = new char[charArray3Length];
        for (int i = 0; i < charArray3Length; ++i) {
            switch (i % 3) {
                case 0 -> value2[i] = (char) (n5 ^ hashCode ^ charArray3[i]);
                case 1 -> value2[i] = (char) (int4 ^ n5 ^ charArray3[i]);
                case 2 -> value2[i] = (char) (int3 ^ charArray3[i]);
            }
        }
        return new String(value2);
    }

}
