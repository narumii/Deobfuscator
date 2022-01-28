package uwu.narumi.deobfuscator.transformer.impl.binsecure.old;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.helper.ASMHelper;
import uwu.narumi.deobfuscator.sandbox.Clazz;
import uwu.narumi.deobfuscator.sandbox.SandBox;
import uwu.narumi.deobfuscator.transformer.Transformer;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * TODO: Fix my shitty code also boilerplate and this is hardcoded as fuck
 * TODO: Make it less hardcoded xd
 * TODO: Use StackAnalyzer etc bruh
 *
 * @see uwu.narumi.deobfuscator.transformer.impl.binsecure.latest.BinsecureStringTransformer
 */
@Deprecated
public class BinsecureOldStringTransformer extends Transformer {

    private final String decryptClassName;
    private final String mapClassName;
    private final String decryptMethodName;

    public BinsecureOldStringTransformer() {
        this("a0", "ac", "0");
    }

    public BinsecureOldStringTransformer(String decryptClassName, String mapClassName, String decryptMethodName) {
        this.decryptClassName = decryptClassName;
        this.mapClassName = mapClassName;
        this.decryptMethodName = decryptMethodName;
    }

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        ClassNode mapClass = deobfuscator.getOriginalClasses().get(mapClassName);
        ClassNode decryptClass = deobfuscator.getOriginalClasses().get(decryptClassName);
        if (mapClass == null || decryptClass == null)
            return;

        SandBox sandBox = SandBox.getInstance();
        sandBox.put(mapClass, decryptClass);

        deobfuscator.classes().forEach(classNode -> {
            ClassNode execution = new ClassNode();
            execution.visit(classNode.version, ACC_PUBLIC, classNode.name, null, "java/lang/Object", null);
            visitInitGetters(execution);

            Map<String, List<String>> strings = new HashMap<>();
            classNode.methods.forEach(methodNode -> Arrays.stream(methodNode.instructions.toArray())
                    .filter(ASMHelper::isString)
                    .map(ASMHelper::getString)
                    .forEach(node -> strings.computeIfAbsent(methodNode.name, ignored -> new ArrayList<>()).add(node)));

            if (!strings.containsKey("<init>"))
                visitConstructor(execution);

            visitSpoofMethod(execution, strings);
            sandBox.put(execution);
        });

        deobfuscator.classes().forEach(classNode -> {
            Clazz clazz = sandBox.get(classNode.name);
            if (clazz == null)
                return;

            Clazz finalClazz = clazz;
            try {
                finalClazz = new Clazz(clazz.getClazz().newInstance().getClass());
            } catch (Exception ignored) {
            }

            Clazz finalClazz1 = finalClazz;
            classNode.methods.forEach(methodNode -> {
                String name = methodNode.name;
                if (methodNode.name.startsWith("<"))
                    name = name.substring(1, name.length() - 1);

                String[] output = (String[]) finalClazz1.invoke(name, "()[Ljava/lang/String;", null, new Object[0]);
                AtomicInteger index = new AtomicInteger();

                Arrays.stream(methodNode.instructions.toArray())
                        .filter(ASMHelper::isString)
                        .filter(node -> node.getNext().getOpcode() == INVOKESTATIC)
                        .filter(node -> ((MethodInsnNode) node.getNext()).owner.equals(decryptClassName))
                        .filter(node -> ((MethodInsnNode) node.getNext()).name.equals(decryptMethodName))
                        .forEach(node -> {
                            methodNode.instructions.remove(node.getNext());
                            methodNode.instructions.set(node, new LdcInsnNode(output[index.getAndIncrement()]));
                        });
            });
        });
    }

    private void visitConstructor(ClassNode classNode) {
        MethodVisitor methodVisitor = classNode.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        methodVisitor.visitCode();
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        methodVisitor.visitInsn(RETURN);
        methodVisitor.visitEnd();
    }

    private void visitSpoofMethod(ClassNode classNode, Map<String, List<String>> strings) {
        strings.forEach((methodNode, cachedStrings) -> {
            if (methodNode.startsWith("<")) {
                visitSpoofInitMethod(classNode, methodNode, cachedStrings);
                return;
            }

            MethodVisitor methodVisitor = classNode.visitMethod(methodNode.equals("<init>") ? ACC_PUBLIC : ACC_PUBLIC | ACC_STATIC, methodNode, "()[Ljava/lang/String;", null, null);
            methodVisitor.visitCode();

            visitNumber(methodVisitor, cachedStrings.size());
            methodVisitor.visitTypeInsn(ANEWARRAY, "java/lang/String");

            {
                for (int i = 0; i < cachedStrings.size(); i++) {
                    String string = cachedStrings.get(i);

                    methodVisitor.visitInsn(DUP);
                    visitNumber(methodVisitor, i);
                    methodVisitor.visitLdcInsn(string);
                    methodVisitor.visitInsn(AASTORE);
                }
            }

            methodVisitor.visitVarInsn(ASTORE, 0);
            methodVisitor.visitInsn(ICONST_0);
            methodVisitor.visitVarInsn(ISTORE, 1);
            Label label0 = new Label();
            methodVisitor.visitLabel(label0);
            methodVisitor.visitVarInsn(ILOAD, 1);
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitInsn(ARRAYLENGTH);
            Label label1 = new Label();
            methodVisitor.visitJumpInsn(IF_ICMPGE, label1);
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitVarInsn(ILOAD, 1);
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitVarInsn(ILOAD, 1);
            methodVisitor.visitInsn(AALOAD);
            methodVisitor.visitMethodInsn(INVOKESTATIC, decryptClassName, decryptMethodName, "(Ljava/lang/String;)Ljava/lang/String;", false);
            methodVisitor.visitInsn(AASTORE);
            methodVisitor.visitIincInsn(1, 1);
            methodVisitor.visitJumpInsn(GOTO, label0);
            methodVisitor.visitLabel(label1);
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitInsn(ARETURN);

            methodVisitor.visitEnd();
        });
    }

    private void visitInitGetters(ClassNode classNode) {
        classNode.visitField(ACC_PUBLIC | ACC_STATIC, "init", "[Ljava/lang/String;", null, null)
                .visitEnd();
        classNode.visitField(ACC_PUBLIC | ACC_STATIC, "clinit", "[Ljava/lang/String;", null, null)
                .visitEnd();

        {
            MethodVisitor methodVisitor = classNode.visitMethod(ACC_PUBLIC | ACC_STATIC, "init", "()[Ljava/lang/String;", null, null);
            methodVisitor.visitCode();
            methodVisitor.visitFieldInsn(GETSTATIC, classNode.name, "init", "[Ljava/lang/String;");
            methodVisitor.visitInsn(ARETURN);
            methodVisitor.visitEnd();
        }
        {
            MethodVisitor methodVisitor = classNode.visitMethod(ACC_PUBLIC | ACC_STATIC, "clinit", "()[Ljava/lang/String;", null, null);
            methodVisitor.visitCode();
            methodVisitor.visitFieldInsn(GETSTATIC, classNode.name, "clinit", "[Ljava/lang/String;");
            methodVisitor.visitInsn(ARETURN);
            methodVisitor.visitEnd();
        }
    }

    private void visitSpoofInitMethod(ClassNode classNode, String methodNode, List<String> strings) {
        int access;
        if (methodNode.equals("<init>"))
            access = ACC_PUBLIC;
        else if (methodNode.equals("<clinit>"))
            access = ACC_STATIC;
        else
            access = ACC_PUBLIC | ACC_STATIC;

        MethodVisitor methodVisitor = classNode.visitMethod(access, methodNode, "()V", null, null);
        methodVisitor.visitCode();

        if (methodNode.equals("<init>")) {
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        }

        visitNumber(methodVisitor, strings.size());
        methodVisitor.visitTypeInsn(ANEWARRAY, "java/lang/String");

        {
            for (int i = 0; i < strings.size(); i++) {
                String string = strings.get(i);

                methodVisitor.visitInsn(DUP);
                visitNumber(methodVisitor, i);
                methodVisitor.visitLdcInsn(string);
                methodVisitor.visitInsn(AASTORE);
            }
        }

        methodVisitor.visitVarInsn(ASTORE, 1);
        methodVisitor.visitInsn(ICONST_0);
        methodVisitor.visitVarInsn(ISTORE, 2);
        Label label0 = new Label();
        methodVisitor.visitLabel(label0);
        methodVisitor.visitVarInsn(ILOAD, 2);
        methodVisitor.visitVarInsn(ALOAD, 1);
        methodVisitor.visitInsn(ARRAYLENGTH);
        Label label1 = new Label();
        methodVisitor.visitJumpInsn(IF_ICMPGE, label1);
        methodVisitor.visitVarInsn(ALOAD, 1);
        methodVisitor.visitVarInsn(ILOAD, 2);
        methodVisitor.visitVarInsn(ALOAD, 1);
        methodVisitor.visitVarInsn(ILOAD, 2);
        methodVisitor.visitInsn(AALOAD);
        methodVisitor.visitMethodInsn(INVOKESTATIC, decryptClassName, decryptMethodName, "(Ljava/lang/String;)Ljava/lang/String;", false);
        methodVisitor.visitInsn(AASTORE);
        methodVisitor.visitIincInsn(2, 1);
        methodVisitor.visitJumpInsn(GOTO, label0);
        methodVisitor.visitLabel(label1);
        methodVisitor.visitVarInsn(ALOAD, 1);
        methodVisitor.visitFieldInsn(PUTSTATIC, classNode.name, methodNode.substring(1, methodNode.length() - 1), "[Ljava/lang/String;");
        methodVisitor.visitInsn(RETURN);

        methodVisitor.visitEnd();
    }
}