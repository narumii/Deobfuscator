package uwu.narumi.deobfuscator.transformer.impl.paramorphism;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.*;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.exception.TransformerException;
import uwu.narumi.deobfuscator.transformer.Transformer;

/*
    Idk if it works lol
    "Paramorphism works completely offline."(https://paramorphism.dev/pricing/) so i added some shitty security manager
 */
public class ParamorphismCrackerTransformer extends Transformer {

    private final String mainClass;

    public ParamorphismCrackerTransformer() {
        this("site/hackery/paramorphism/launch/Main");
    }

    public ParamorphismCrackerTransformer(String mainClass) {
        this.mainClass = mainClass.replace('.', '/');
    }

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        if (deobfuscator.getClassReaderFlags() != 0 && deobfuscator.getClassWriterFlags() != 0)
            throw new TransformerException("You need set ClassReader and ClassWriter Flags to 0");

        deobfuscator.getFiles().put("ParamorphismSecurityManager.class", securityManagerClass());

        InsnList insnList = new InsnList();
        insnList.add(new TypeInsnNode(NEW, "ParamorphismSecurityManager"));
        insnList.add(new InsnNode(DUP));
        insnList.add(new MethodInsnNode(INVOKESPECIAL, "ParamorphismSecurityManager", "<init>", "()V", false));
        insnList.add(new MethodInsnNode(INVOKESTATIC, "java/lang/System", "setSecurityManager", "(Ljava/lang/SecurityManager;)V", false));

        ClassNode classNode = deobfuscator.getClasses().get(mainClass);
        if (classNode == null)
            throw new TransformerException("Main class not found");

        findMethod(classNode, methodNode -> methodNode.name.equals("main") && methodNode.desc.equals("([Ljava/lang/String;)"))
                .ifPresent(methodNode -> methodNode.instructions.insert(insnList));
    }

    private byte[] securityManagerClass() {
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        classWriter.visit(V1_8, ACC_PUBLIC | ACC_SUPER, "ParamorphismSecurityManager", null, "java/lang/SecurityManager", null);

        {
            MethodVisitor methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
            methodVisitor.visitCode();
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/lang/SecurityManager", "<init>", "()V", false);
            methodVisitor.visitInsn(RETURN);
            methodVisitor.visitMaxs(1, 1);
            methodVisitor.visitEnd();
        }
        {
            MethodVisitor methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "checkConnect", "(Ljava/lang/String;I)V", null, null);
            methodVisitor.visitCode();
            methodVisitor.visitTypeInsn(NEW, "java/lang/RuntimeException");
            methodVisitor.visitInsn(DUP);
            methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/lang/RuntimeException", "<init>", "()V", false);
            methodVisitor.visitInsn(ATHROW);
            methodVisitor.visitMaxs(2, 3);
            methodVisitor.visitEnd();
        }
        {
            MethodVisitor methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "checkConnect", "(Ljava/lang/String;ILjava/lang/Object;)V", null, null);
            methodVisitor.visitCode();
            methodVisitor.visitTypeInsn(NEW, "java/lang/RuntimeException");
            methodVisitor.visitInsn(DUP);
            methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/lang/RuntimeException", "<init>", "()V", false);
            methodVisitor.visitInsn(ATHROW);
            methodVisitor.visitMaxs(2, 4);
            methodVisitor.visitEnd();
        }
        classWriter.visitEnd();

        return classWriter.toByteArray();
    }
}
