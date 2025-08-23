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
import uwu.narumi.deobfuscator.api.asm.matcher.impl.FieldMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.MethodMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.NumberMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.OpcodeMatch;
import uwu.narumi.deobfuscator.api.execution.SandBox;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

import java.util.*;

public class ZelixStringTransformer extends Transformer {
    private static final Match ZELIX_STRING_CLINIT_MATCH = SequenceMatch.of(
        OpcodeMatch.of(ALOAD),
        FieldMatch.create().desc("[Ljava/lang/String;").capture("field-1"),
        NumberMatch.of(),
        OpcodeMatch.of(ANEWARRAY),
        FieldMatch.create().desc("[Ljava/lang/String;").capture("field-2"),
        OpcodeMatch.of(GOTO)
    );

    private static final Match INVOKE_NUMBER_MATCH = SequenceMatch.of(
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
            MatchContext match = ZELIX_STRING_CLINIT_MATCH.findFirstMatch(methodContext);

            if (match == null)
                continue;

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
            try {
                InstanceClass clazz = sandBox.getHelper().loadClass("tmp." + classWrapper.canonicalName());

                for (MethodNode method : classWrapper.methods()) {
                    MethodContext methodContext = MethodContext.of(classWrapper, method);
                    INVOKE_NUMBER_MATCH.findAllMatches(methodContext).forEach(matchContext -> {
                        int key1 = matchContext.captures().get("key1").insn().asInteger();
                        int key2 = matchContext.captures().get("key2").insn().asInteger();
                        MethodInsnNode decryptedMethod = matchContext.captures().get("method-node").insn().asMethodInsn();

                        String value = sandBox.getInvocationUtil().invokeStringReference(
                            clazz.getMethod(decryptedMethod.name, "(II)Ljava/lang/String;"),
                            Argument.int32(key1),
                            Argument.int32(key2)
                        );

                        matchContext.insnContext().methodNode().instructions.insert(matchContext.insn(), new LdcInsnNode(value));
                        matchContext.removeAll();
                        markChange();
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            context().removeCompiledClass(tmpClassWrapper);
        });
    }

    private byte[] cloneClassWithClinit(ClassWrapper classWrapper, MethodNode clinit, MatchContext match) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        ClassNode classNode = new ClassNode();

        classNode.access = ACC_PUBLIC | ACC_STATIC;
        classNode.name = "tmp/" + classWrapper.name();
        classNode.version = classWrapper.classNode().version;
        classNode.superName = "java/lang/Object";

        classNode.methods.add(clinit);

        addArrayField(classWrapper, classNode, match, "field-1");
        addArrayField(classWrapper, classNode, match, "field-2");

        classNode.accept(cw);
        return cw.toByteArray();
    }

    private void addArrayField(ClassWrapper classWrapper, ClassNode classNode, MatchContext matchContext, String key) {
        FieldInsnNode field1 = matchContext.captures().get(key).insn().asFieldInsn();
        FieldNode fieldNode1 = classWrapper.findField(field1.name, field1.desc).get();
        classNode.fields.add(fieldNode1);
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

        /**
         * Remove all instructions after executing "putstatic" 2 arrays. They can cause compiling issue as it must load on other classes.
         * TODO: Is it better if we can remove all instructions that embedded on label?
         */

        Iterator<AbstractInsnNode> iterator = clinitMethod.instructions.iterator();

        LabelNode returnLabelnode = null;
        boolean markToRemove = false;

        List<AbstractInsnNode> removedInsn = new ArrayList<>();

        while (iterator.hasNext()) {
            AbstractInsnNode insn = iterator.next();

            if (insn.getOpcode() == Opcodes.ALOAD &&
                insn.getNext() instanceof FieldInsnNode fieldNode1 && fieldNode1.desc.equals("[Ljava/lang/String;") &&
                fieldNode1.getNext().isNumber() &&
                fieldNode1.getNext(2).getOpcode() == Opcodes.ANEWARRAY &&
                fieldNode1.getNext(3) instanceof FieldInsnNode fieldNode2 && fieldNode2.desc.equals("[Ljava/lang/String;") &&
                fieldNode2.getNext() instanceof JumpInsnNode jumpInsnNode) {

                returnLabelnode = jumpInsnNode.label;
            }

            if (returnLabelnode != null && insn instanceof LabelNode labelNode1 && labelNode1.getLabel() == returnLabelnode.getLabel()) {
                markToRemove = true;
                iterator.next();
            } else if (markToRemove && insn.getOpcode() != Opcodes.RETURN)
                removedInsn.add(insn);
        }

        removedInsn.forEach(insn -> clinitMethod.instructions.remove(insn));
        removedInsn.clear();

        for (AbstractInsnNode insn : clinitMethod.instructions) {
            if (insn.getOpcode() == Opcodes.INVOKESTATIC)
                removedInsn.add(insn);
        }

        removedInsn.forEach(insn -> clinitMethod.instructions.remove(insn));

        classWrapper.findMethod(methodNode -> methodNode.desc.equals("(II)Ljava/lang/String;")).ifPresent(method -> {
            if (!classNode.methods.contains(method)) classNode.methods.add(method);
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
