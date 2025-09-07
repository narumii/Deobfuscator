package uwu.narumi.deobfuscator.core.other.impl.zkm;

import dev.xdark.ssvm.invoke.Argument;
import dev.xdark.ssvm.mirror.type.InstanceClass;
import dev.xdark.ssvm.value.ObjectValue;
import dev.xdark.ssvm.value.SimpleArrayValue;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.asm.MethodContext;
import uwu.narumi.deobfuscator.api.asm.matcher.Match;
import uwu.narumi.deobfuscator.api.asm.matcher.MatchContext;
import uwu.narumi.deobfuscator.api.asm.matcher.group.SequenceMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.JumpMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.MethodMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.NumberMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.OpcodeMatch;
import uwu.narumi.deobfuscator.api.execution.SandBox;
import uwu.narumi.deobfuscator.api.helper.AsmHelper;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A comprehensive string transformer using temp class to deobfuscate
 * @author toidicakhia
 */
public class ZelixStringTransformer extends Transformer {
    private static final Match CLINIT_DETECTION = SequenceMatch.of(
        OpcodeMatch.of(ILOAD),
        JumpMatch.of(IF_ICMPGE).capture("label"),
        OpcodeMatch.of(ALOAD),
        OpcodeMatch.of(ILOAD),
        MethodMatch.of(INVOKEVIRTUAL).owner("java/lang/String").name("charAt").desc("(I)C"),
        OpcodeMatch.of(ISTORE),
        JumpMatch.of(GOTO)
    );

    private static final Match DECRYPTION_MATCH = SequenceMatch.of(
        NumberMatch.numInteger().capture("key1"),
        NumberMatch.numInteger().capture("key2"),
        MethodMatch.create().desc("(II)Ljava/lang/String;").capture("method-node")
    );

    @Override
    protected void transform() throws Exception {
        Map<ClassWrapper, ClassWrapper> encryptedClassWrapper = new HashMap<>();

        for (ClassWrapper classWrapper : scopedClasses()) {
            if (classWrapper.findClInit().isEmpty())
                continue;

            MethodNode clinitMethod = classWrapper.findClInit().get();
            MethodContext methodContext = MethodContext.of(classWrapper, clinitMethod);
            List<MatchContext> matches = CLINIT_DETECTION.findAllMatches(methodContext);

            if (matches.isEmpty())
                continue;

            MatchContext match = matches.get(matches.size() - 1);

            // copy clinitMethod
            byte[] clonedClass = cloneClassWithClinit(classWrapper, clinitMethod, match);
            byte[] modifiedClass = modifyByteCode(classWrapper, clonedClass);

            ClassWrapper tmpClassWrapper = context().addCompiledClass("tmp/" + classWrapper.name() + ".class", modifiedClass);
            if (tmpClassWrapper == null)
                continue;

            encryptedClassWrapper.put(classWrapper, tmpClassWrapper);
        }

        SandBox sandBox = new SandBox(context());
        encryptedClassWrapper.forEach((classWrapper, tmpClassWrapper) -> {
            AtomicBoolean isDecryptedFully = new AtomicBoolean(true);

            try {
                InstanceClass clazz = sandBox.getHelper().loadClass("tmp." + classWrapper.canonicalName());

                for (MethodNode method : classWrapper.methods()) {
                    MethodContext methodContext = MethodContext.of(classWrapper, method);

                    DECRYPTION_MATCH.findAllMatches(methodContext).forEach(matchContext -> {
                        int key1 = matchContext.captures().get("key1").insn().asInteger();
                        int key2 = matchContext.captures().get("key2").insn().asInteger();
                        MethodInsnNode decryptedMethod = matchContext.captures().get("method-node").insn().asMethodInsn();

                        try {
                            String decryptedString = sandBox.getInvocationUtil().invokeStringReference(
                                clazz.getMethod(decryptedMethod.name, decryptedMethod.desc),
                                Argument.int32(key1),
                                Argument.int32(key2)
                            );

                            matchContext.insnContext().methodNode().instructions.insert(matchContext.insn(), new LdcInsnNode(decryptedString));
                            matchContext.removeAll();
                            markChange();
                        } catch (Exception e) {
                            isDecryptedFully.set(false);
                        }
                    });
                }

                SimpleArrayValue arrayValue = (SimpleArrayValue) sandBox.getInvocationUtil().invokeReference(
                    clazz.getMethod("getArr", "()[Ljava/lang/String;")
                );

                int length = arrayValue.getLength();
                MethodNode clinitMethod = classWrapper.findClInit().get();

                VarInsnNode startArrayInsn = null;

                for (AbstractInsnNode insn : clinitMethod.instructions.toArray()) {
                    if (insn instanceof LabelNode)
                        break;

                    if (insn.getOpcode() == Opcodes.INVOKESTATIC)
                        clinitMethod.instructions.remove(insn);
                    else if (insn instanceof VarInsnNode varInsnNode &&
                        insn.getPrevious() instanceof TypeInsnNode typeInsnNode && typeInsnNode.getOpcode() == Opcodes.ANEWARRAY && typeInsnNode.desc.equals("java/lang/String") &&
                        insn.getPrevious().getPrevious() != null && insn.getPrevious().getPrevious().isInteger()
                    ) {
                        startArrayInsn = varInsnNode;
                        break;
                    }
                }

                if (startArrayInsn != null) {
                    InsnList insnList = new InsnList();

                    for (int i = 0; i < length; i++) {
                        ObjectValue value = arrayValue.getReference(i);
                        String decryptedString = sandBox.vm().getOperations().readUtf8(value);

                        // insert data
                        insnList.add(new VarInsnNode(Opcodes.ALOAD, startArrayInsn.var));
                        insnList.add(AsmHelper.numberInsn(i));
                        insnList.add(new LdcInsnNode(decryptedString));
                        insnList.add(new InsnNode(Opcodes.AASTORE));
                    }

                    clinitMethod.instructions.insert(startArrayInsn, insnList);
                    markChange();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

            context().removeCompiledClass(tmpClassWrapper);

            if (isDecryptedFully.get())
                cleanUpFunction(classWrapper);
        });
    }

    private void cleanUpFunction(ClassWrapper classWrapper) {
        MethodNode clinitMethod = classWrapper.findClInit().get();
        MethodContext methodContext = MethodContext.of(classWrapper, clinitMethod);
        List<MatchContext> matches = CLINIT_DETECTION.findAllMatches(methodContext);
        MatchContext match = matches.get(matches.size() - 1);

        JumpInsnNode jumpInsnNode = match.captures().get("label").insn().asJump();
        AbstractInsnNode currentInsn = jumpInsnNode.label;

        List<AbstractInsnNode> removedInsns = new ArrayList<>();
        LabelNode firstLabel = null;

        for (AbstractInsnNode insn : clinitMethod.instructions.toArray()) {
            if (insn instanceof LabelNode labelNode) {
                firstLabel = labelNode;
                break;
            }
        }

        if (firstLabel == null)
            return;

        while (currentInsn != firstLabel) {
            currentInsn = currentInsn.getPrevious();
            removedInsns.add(currentInsn);
        }

        removedInsns.forEach(insn -> clinitMethod.instructions.remove(insn));
        classWrapper.methods().removeIf(methodNode -> methodNode.desc.equals("(II)Ljava/lang/String;"));
    }

    private byte[] cloneClassWithClinit(ClassWrapper classWrapper, MethodNode clinit, MatchContext match) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        ClassNode classNode = new ClassNode();

        String tmpClassName = "tmp/" + classWrapper.name();

        classNode.access = ACC_PUBLIC | ACC_STATIC;
        classNode.name = tmpClassName;
        classNode.version = classWrapper.classNode().version;
        classNode.superName = "java/lang/Object";

        classNode.methods.add(clinit);

        // create a tmp array for getting data
        classNode.fields.add(new FieldNode(ACC_PUBLIC | ACC_STATIC, "arr", "[Ljava/lang/String;", null, null));

        MethodNode getMethodNode = new MethodNode(ACC_PUBLIC | ACC_STATIC, "getArr", "()[Ljava/lang/String;", null, new String[0]);
        getMethodNode.visitCode();
        getMethodNode.visitFieldInsn(Opcodes.GETSTATIC, tmpClassName, "arr", "[Ljava/lang/String;");
        getMethodNode.visitInsn(Opcodes.ARETURN);
        getMethodNode.visitMaxs(1, 0);
        getMethodNode.visitEnd();

        classNode.methods.add(getMethodNode);

        // copy array
        JumpInsnNode jumpInsnNode = match.captures().get("label").insn().asJump();
        AbstractInsnNode node = jumpInsnNode.label;

        while (node.getOpcode() != Opcodes.GOTO) {
            node = node.getNext();

            if (node instanceof FieldInsnNode fieldInsnNode && fieldInsnNode.owner.equals(classWrapper.name())) {
                FieldNode fieldNode = classWrapper.findField(fieldInsnNode.name, fieldInsnNode.desc).get();
                classNode.fields.add(fieldNode);
            }
        }

        classWrapper.findMethod(methodNode -> methodNode.desc.equals("(II)Ljava/lang/String;")).ifPresent(method -> {
            if (!classNode.methods.contains(method))
                classNode.methods.add(method);
        });

        classNode.accept(cw);
        return cw.toByteArray();
    }


    private byte[] modifyByteCode(ClassWrapper classWrapper, byte[] classByte) {
        ClassReader cr = new ClassReader(classByte);
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        ClassNode classNode = new ClassNode();

        cr.accept(classNode, 0);

        MethodNode clinitMethod = classNode.methods.stream()
            .filter(methodNode -> methodNode.name.equals("<clinit>"))
            .findFirst()
            .get();

        MethodContext methodContext = MethodContext.of(classNode, clinitMethod);
        List<MatchContext> matches = CLINIT_DETECTION.findAllMatches(methodContext);
        MatchContext match = matches.get(matches.size() - 1);

        JumpInsnNode jumpInsnNode = match.captures().get("label").insn().asJump();
        AbstractInsnNode currentInsn = jumpInsnNode.label;

        // mark a return opcode
        while (currentInsn != null) {
            if (currentInsn.getOpcode() == Opcodes.GOTO) {
                MethodNode methodNode = match.insnContext().methodNode();
                methodNode.instructions.insert(currentInsn, new InsnNode(Opcodes.RETURN));
                methodNode.instructions.remove(currentInsn);
            }

            currentInsn = currentInsn.getNext();
        }

        for (AbstractInsnNode insn : clinitMethod.instructions.toArray()) {
            if (insn instanceof LabelNode)
                break;

            if (insn.getOpcode() == Opcodes.INVOKESTATIC)
                clinitMethod.instructions.remove(insn);
            else if (insn instanceof VarInsnNode varInsnNode &&
                insn.getPrevious() instanceof TypeInsnNode typeInsnNode && typeInsnNode.getOpcode() == Opcodes.ANEWARRAY && typeInsnNode.desc.equals("java/lang/String") &&
                insn.getPrevious().getPrevious() != null && insn.getPrevious().getPrevious().isInteger()
            ) {
                InsnList list = new InsnList();
                list.add(new VarInsnNode(Opcodes.ALOAD, varInsnNode.var));
                list.add(new FieldInsnNode(Opcodes.PUTSTATIC, classNode.name, "arr", "[Ljava/lang/String;"));
                clinitMethod.instructions.insert(insn, list);
            }
        }

        for (MethodNode methodNode : classNode.methods) {
            for (AbstractInsnNode insn : methodNode.instructions) {
                if (insn instanceof MethodInsnNode methodInsnNode && methodInsnNode.owner.equals(classWrapper.name())) {
                    methodInsnNode.owner = "tmp/" + methodInsnNode.owner;
                } else if (insn instanceof FieldInsnNode fieldInsnNode && fieldInsnNode.owner.equals(classWrapper.name())) {
                    fieldInsnNode.owner = "tmp/" + fieldInsnNode.owner;
                }
            }
        }

        classNode.accept(cw);
        return cw.toByteArray();
    }
}
