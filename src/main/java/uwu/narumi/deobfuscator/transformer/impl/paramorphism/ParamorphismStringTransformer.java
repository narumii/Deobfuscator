package uwu.narumi.deobfuscator.transformer.impl.paramorphism;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.sandbox.Clazz;
import uwu.narumi.deobfuscator.sandbox.SandBox;
import uwu.narumi.deobfuscator.transformer.Transformer;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/*
    I think we can do this without execution
 */
public class ParamorphismStringTransformer extends Transformer {

    //private final Set<String> classesToLoad;

    /*public ParamorphismStringTransformer(String... classesToLoad) {
        this.classesToLoad = new HashSet<>(Arrays.asList(classesToLoad));
    }*/

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        /*List<ClassNode> toLoad = new ArrayList<>();
        for (String className : classesToLoad) {
            ClassNode classNode = deobfuscator.getOriginalClasses().get(className);
            if (classNode == null)
                throw new TransformerException("Class not found: " + className);

            toLoad.add(classNode);
        }*/
        ClassNode dispatcher = searchForDispatcherClass(deobfuscator);
        List<ClassNode> toLoad = searchForStringClasses(deobfuscator);
        if (toLoad.isEmpty() || dispatcher == null)
            return;

        Set<String> names = toLoad.stream().map(classNode -> classNode.name).collect(Collectors.toSet());

        SandBox sandBox = SandBox.getInstance();
        sandBox.put(dispatcher);
        sandBox.put(toLoad.toArray(new ClassNode[0]));

        deobfuscator.classes().stream()
                .filter(classNode -> !names.contains(classNode.name))
                .forEach(classNode -> {
                    ClassNode execution = new ClassNode();
                    execution.visit(classNode.version, ACC_PUBLIC, classNode.name, null, "java/lang/Object", null);
                    visitInitGetters(execution);

                    Map<String, List<InvokeDynamicInsnNode>> strings = new HashMap<>();
                    classNode.methods.forEach(methodNode -> Arrays.stream(methodNode.instructions.toArray())
                            .filter(node -> node instanceof InvokeDynamicInsnNode)
                            .map(InvokeDynamicInsnNode.class::cast)
                            .filter(node -> node.name.equals("get"))
                            .filter(node -> node.desc.equals("()Ljava/lang/String;"))
                            .forEach(node -> strings.computeIfAbsent(methodNode.name, ignored -> new ArrayList<>()).add(node)));

                    if (!strings.containsKey("<init>"))
                        visitConstructor(execution);

                    visitSpoofMethod(execution, strings);
                    sandBox.put(execution);

                    strings.clear();
                    //deobfuscator.getClasses().put(execution.name, execution);
                });

        deobfuscator.classes().stream()
                .filter(classNode -> !names.contains(classNode.name))
                .forEach(classNode -> {
                    Clazz clazz = sandBox.get(classNode.name);
                    if (clazz == null)
                        return;

                    Clazz finalClazz = clazz;
                    try {
                        finalClazz = new Clazz(clazz.getClazz().newInstance().getClass());
                    } catch (Throwable ignored) {
                        //ignored.printStackTrace();
                    }

                    Clazz finalClazz1 = finalClazz;
                    classNode.methods.forEach(methodNode -> {
                        String name = methodNode.name;
                        if (methodNode.name.startsWith("<"))
                            name = name.substring(1, name.length() - 1);

                        try {
                            Object output = finalClazz1.invoke(name, "()[Ljava/lang/String;", null);
                            AtomicInteger index = new AtomicInteger();

                            if (!(output instanceof String[])) {
                                return;
                            }

                            Arrays.stream(methodNode.instructions.toArray())
                                    .filter(node -> node instanceof InvokeDynamicInsnNode)
                                    .map(InvokeDynamicInsnNode.class::cast)
                                    .filter(node -> node.name.equals("get"))
                                    .filter(node -> node.desc.equals("()Ljava/lang/String;"))
                                    .forEach(node -> methodNode.instructions.set(node, new LdcInsnNode(((String[]) output)[index.getAndIncrement()])));
                        } catch (Exception e) {
                            //e.printStackTrace();
                        }
                    });
                });

        deobfuscator.getClasses().keySet().removeIf(names::contains);
        toLoad.clear();
        names.clear();
    }

    private void visitConstructor(ClassNode classNode) {
        MethodVisitor methodVisitor = classNode.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        methodVisitor.visitCode();
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        methodVisitor.visitInsn(RETURN);
        methodVisitor.visitEnd();
    }

    private void visitSpoofMethod(ClassNode classNode, Map<String, List<InvokeDynamicInsnNode>> strings) {
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
                    InvokeDynamicInsnNode node = cachedStrings.get(i);

                    methodVisitor.visitInsn(DUP);
                    visitNumber(methodVisitor, i);
                    methodVisitor.visitInvokeDynamicInsn(node.name, node.desc, node.bsm, node.bsmArgs);
                    methodVisitor.visitInsn(AASTORE);
                }
            }
            methodVisitor.visitVarInsn(ASTORE, 0);
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

    private void visitSpoofInitMethod(ClassNode classNode, String methodNode, List<InvokeDynamicInsnNode> strings) {
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
                InvokeDynamicInsnNode node = strings.get(i);

                methodVisitor.visitInsn(DUP);
                visitNumber(methodVisitor, i);
                methodVisitor.visitInvokeDynamicInsn(node.name, node.desc, node.bsm, node.bsmArgs);
                methodVisitor.visitInsn(AASTORE);
            }
        }

        methodVisitor.visitVarInsn(ASTORE, 1);
        methodVisitor.visitVarInsn(ALOAD, 1);
        methodVisitor.visitFieldInsn(PUTSTATIC, classNode.name, methodNode.substring(1, methodNode.length() - 1), "[Ljava/lang/String;");
        methodVisitor.visitInsn(RETURN);

        methodVisitor.visitEnd();
    }

    private ClassNode searchForDispatcherClass(Deobfuscator deobfuscator) {
        return deobfuscator.classes().stream()
                .filter(classNode -> classNode.name.endsWith("Dispatcher"))
                .findFirst()
                .orElseThrow();
    }

    private List<ClassNode> searchForStringClasses(Deobfuscator deobfuscator) {
        return deobfuscator.classes().stream()
                .filter(classNode -> classNode.methods.stream().anyMatch(methodNode -> methodNode.name.equals("$") && methodNode.desc.equals("([BLjava/util/Map;)V")))
                .collect(Collectors.toList());
    }
}
