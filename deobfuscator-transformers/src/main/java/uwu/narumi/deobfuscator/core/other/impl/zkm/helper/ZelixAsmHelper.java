package uwu.narumi.deobfuscator.core.other.impl.zkm.helper;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;

import java.util.Arrays;

import static org.objectweb.asm.Opcodes.*;

public class ZelixAsmHelper {

    public static void createTempFieldForSSVM(ClassNode classNode, String tmpClassName, String name, String getterName, String desc) {
        classNode.fields.add(new FieldNode(ACC_PUBLIC | ACC_STATIC, name, desc, null, null));

        MethodNode getMethodNode = new MethodNode(ACC_PUBLIC | ACC_STATIC, getterName, "()" + desc, null, new String[0]);
        getMethodNode.visitCode();
        getMethodNode.visitFieldInsn(Opcodes.GETSTATIC, tmpClassName, name, desc);
        getMethodNode.visitInsn(Opcodes.ARETURN);
        getMethodNode.visitMaxs(1, 0);
        getMethodNode.visitEnd();

        classNode.methods.add(getMethodNode);
    }

    public static boolean isStringDecryptMethod(MethodNode methodNode) {
        if (!methodNode.desc.equals("(II)Ljava/lang/String;"))
            return false;

        if ((methodNode.access & (ACC_PRIVATE | ACC_STATIC)) == 0)
            return false;

        return Arrays.stream(methodNode.instructions.toArray()).anyMatch(abstractInsnNode -> abstractInsnNode.getOpcode() == TABLESWITCH);
    }

    // TODO: Can we find a way to access direcly variable without getter?

    public static void renameOwner(ClassNode classNode, ClassWrapper classWrapper) {
        for (MethodNode methodNode : classNode.methods) {
            for (AbstractInsnNode insn : methodNode.instructions) {
                if (insn instanceof MethodInsnNode methodInsnNode && methodInsnNode.owner.equals(classWrapper.name())) {
                    methodInsnNode.owner = "tmp/" + methodInsnNode.owner;
                } else if (insn instanceof FieldInsnNode fieldInsnNode && fieldInsnNode.owner.equals(classWrapper.name())) {
                    fieldInsnNode.owner = "tmp/" + fieldInsnNode.owner;
                }
            }
        }
    }

}
