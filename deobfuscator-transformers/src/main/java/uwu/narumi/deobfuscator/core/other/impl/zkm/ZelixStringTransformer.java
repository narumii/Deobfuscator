package uwu.narumi.deobfuscator.core.other.impl.zkm;

import dev.xdark.ssvm.invoke.Argument;
import dev.xdark.ssvm.mirror.type.InstanceClass;
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
import uwu.narumi.deobfuscator.api.transformer.Transformer;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

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
            byte[] removedInsnByte = removeDependantInsn(classWrapper, clonedClass);
            byte[] modified = renameInvoke(classWrapper, removedInsnByte);

            ClassWrapper tmpClassWrapper = context().addCompiledClass("tmp/" + classWrapper.name() + ".class", modified);
            if (tmpClassWrapper == null)
                continue;

            encryptedClassWrapper.put(classWrapper, tmpClassWrapper);
        }

        SandBox sandBox = new SandBox(context());
        encryptedClassWrapper.forEach((classWrapper, tmpClassWrapper) -> {
            AtomicBoolean isDecryptedFully = new AtomicBoolean(true);

            try {
                InstanceClass clazz = sandBox.getHelper().loadClass("tmp." + classWrapper.canonicalName());

                List<MethodInsnNode> methods = new ArrayList<>();

                for (MethodNode method : classWrapper.methods()) {
                    MethodContext methodContext = MethodContext.of(classWrapper, method);
                    DECRYPTION_MATCH.findAllMatches(methodContext).forEach(matchContext -> {
                        int key1 = matchContext.captures().get("key1").insn().asInteger();
                        int key2 = matchContext.captures().get("key2").insn().asInteger();
                        MethodInsnNode decryptedMethod = matchContext.captures().get("method-node").insn().asMethodInsn();

                        try {
                            String value = sandBox.getInvocationUtil().invokeStringReference(
                                clazz.getMethod(decryptedMethod.name, decryptedMethod.desc),
                                Argument.int32(key1),
                                Argument.int32(key2)
                            );

                            matchContext.insnContext().methodNode().instructions.insert(matchContext.insn(), new LdcInsnNode(value));
                            matchContext.removeAll();
                            methods.add(decryptedMethod);
                            markChange();
                        } catch (Exception e) {
                            isDecryptedFully.set(false);
                        }
                    });
                }

                if (isDecryptedFully.get())
                    methods.forEach(decryptedMethod -> classWrapper.methods().removeIf(methodNode -> methodNode.name.equals(decryptedMethod.name) && methodNode.desc.equals(decryptedMethod.desc)));
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

        for (AbstractInsnNode insn: clinitMethod.instructions.toArray()) {
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

        classNode.access = ACC_PUBLIC | ACC_STATIC;
        classNode.name = "tmp/" + classWrapper.name();
        classNode.version = classWrapper.classNode().version;
        classNode.superName = "java/lang/Object";

        classNode.methods.add(clinit);

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

        classNode.accept(cw);
        return cw.toByteArray();
    }

    private void setReturnOpcode(MatchContext match) {
        JumpInsnNode jumpInsnNode = match.captures().get("label").insn().asJump();
        AbstractInsnNode insn = jumpInsnNode.label;

        while (insn != null) {
            if (insn.getOpcode() == Opcodes.GOTO) {
                match.insnContext().methodNode().instructions.insert(insn, new InsnNode(Opcodes.RETURN));
                match.insnContext().methodNode().instructions.remove(insn);
                break;
            }

            insn = insn.getNext();
        }
    }

    private void removeInvokeOnFirstLabel(InsnList instructions) {
        for (AbstractInsnNode insn: instructions.toArray()) {
            if (insn instanceof LabelNode)
                break;

            if (insn.getOpcode() == Opcodes.INVOKESTATIC)
                instructions.remove(insn);
        }
    }

    private byte[] removeDependantInsn(ClassWrapper classWrapper, byte[] classByte) {
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

        setReturnOpcode(matches.get(matches.size() - 1));
        removeInvokeOnFirstLabel(clinitMethod.instructions);

        classWrapper.findMethod(methodNode -> methodNode.desc.equals("(II)Ljava/lang/String;")).ifPresent(method -> {
            if (!classNode.methods.contains(method))
                classNode.methods.add(method);
        });

        classNode.accept(cw);
        return cw.toByteArray();
    }

    private byte[] renameInvoke(ClassWrapper classWrapper, byte[] classByte) {
        ClassReader cr = new ClassReader(classByte);
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        ClassNode classNode = new ClassNode();

        cr.accept(classNode, 0);

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
