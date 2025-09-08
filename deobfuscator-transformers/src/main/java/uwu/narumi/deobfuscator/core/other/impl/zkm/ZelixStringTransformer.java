package uwu.narumi.deobfuscator.core.other.impl.zkm;

import dev.xdark.ssvm.invoke.Argument;
import dev.xdark.ssvm.mirror.type.InstanceClass;
import dev.xdark.ssvm.value.ObjectValue;
import dev.xdark.ssvm.value.SimpleArrayValue;
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
import uwu.narumi.deobfuscator.api.asm.matcher.impl.OpcodeMatch;
import uwu.narumi.deobfuscator.api.execution.SandBox;
import uwu.narumi.deobfuscator.api.helper.AsmHelper;
import uwu.narumi.deobfuscator.api.transformer.Transformer;
import uwu.narumi.deobfuscator.core.other.impl.zkm.helper.ZelixAsmHelper;
import uwu.narumi.deobfuscator.core.other.impl.zkm.helper.ZelixStringDecryptionMatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

    private static final Match DECRYPTION_MATCH = new ZelixStringDecryptionMatch();

    @Override
    protected void transform() throws Exception {
        List<ClassData> encryptedClassWrapper = new ArrayList<>();

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
            byte[] clonedClass = copyToTempClass(classWrapper, clinitMethod, match);

            ClassWrapper tmpClassWrapper = context().addCompiledClass("tmp/" + classWrapper.name() + ".class", clonedClass);
            if (tmpClassWrapper == null)
                continue;

            encryptedClassWrapper.add(new ClassData(classWrapper, clinitMethod, tmpClassWrapper, match));
        }

        SandBox sandBox = new SandBox(context());

        for (ClassData classData : encryptedClassWrapper) {
            AtomicBoolean shouldCleanArray = new AtomicBoolean(false);

            try {
                InstanceClass clazz = sandBox.getHelper().loadClass("tmp." + classData.classWrapper.canonicalName());

                for (MethodNode method : classData.classWrapper.methods()) {
                    MethodContext methodContext = MethodContext.of(classData.classWrapper, method);

                    List<MatchContext> matches = DECRYPTION_MATCH.findAllMatches(methodContext);

                    if (!matches.isEmpty()) {
                        shouldCleanArray.set(true);
                        matches.forEach(matchContext -> {
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
                                shouldCleanArray.set(false);
                            }
                        });
                    }

                }

                // TODO: Access directly array without getter
                SimpleArrayValue arrayValue = (SimpleArrayValue) sandBox.getInvocationUtil().invokeReference(
                    clazz.getMethod("getArr", "()[Ljava/lang/String;")
                );

                int length = arrayValue.getLength();

                VarInsnNode startArrayInsn = null;

                for (AbstractInsnNode insn : classData.clinitMethod.instructions.toArray()) {
                    if (insn instanceof LabelNode)
                        break;

                    if (insn.getOpcode() == Opcodes.INVOKESTATIC)
                        classData.clinitMethod.instructions.remove(insn);
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

                    classData.clinitMethod.instructions.insert(startArrayInsn, insnList);
                    markChange();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

            context().removeCompiledClass(classData.tempClassWrapper);

            if (shouldCleanArray.get())
                cleanUpFunction(classData);
        }
    }

    private void cleanUpFunction(ClassData classData) {
        JumpInsnNode jumpInsnNode = classData.match.captures().get("label").insn().asJump();

        Arrays.stream(classData.clinitMethod.instructions.toArray()).filter(insn -> insn instanceof LabelNode).map(insn -> (LabelNode) insn).findFirst().ifPresent(firstLabel -> {
            AbstractInsnNode currentInsn = jumpInsnNode.label;

            List<AbstractInsnNode> removedInsns = new ArrayList<>();
            while (currentInsn != firstLabel) {
                currentInsn = currentInsn.getPrevious();
                removedInsns.add(currentInsn);
            }

            removedInsns.forEach(insn -> classData.clinitMethod.instructions.remove(insn));
        });

        classData.classWrapper.methods().removeIf(ZelixAsmHelper::isStringDecryptMethod);
    }

    private byte[] copyToTempClass(ClassWrapper classWrapper, MethodNode clinit, MatchContext match) {
        String tmpClassName = "tmp/" + classWrapper.name();

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        ClassNode classNode = new ClassNode();
        classNode.visit(
            classWrapper.classNode().version,
            ACC_PUBLIC | ACC_SUPER,
            "tmp/" + classWrapper.name(),
            "",
            "java/lang/Object",
            new String[0]
        );

        MethodNode copiedClinit = AsmHelper.copyMethod(clinit);
        classNode.methods.add(copiedClinit);
        ZelixAsmHelper.createTempFieldForSSVM(classNode, tmpClassName, "arr", "getArr", "[Ljava/lang/String;");

        // copy array
        JumpInsnNode jumpInsnNode = match.captures().get("label").insn().asJump();
        AbstractInsnNode node = jumpInsnNode.label;

        while (node.getOpcode() != Opcodes.GOTO) { // is it safe?
            node = node.getNext();

            if (node instanceof FieldInsnNode fieldInsnNode && fieldInsnNode.owner.equals(classWrapper.name()))
                classWrapper.findField(fieldInsnNode.name, fieldInsnNode.desc).ifPresent(fieldNode -> classNode.fields.add(fieldNode));
        }

        classWrapper.findMethod(ZelixAsmHelper::isStringDecryptMethod).ifPresent(method -> classNode.methods.add(AsmHelper.copyMethod(method)));

        MethodContext methodContext = MethodContext.of(classNode, copiedClinit);
        List<MatchContext> matches = CLINIT_DETECTION.findAllMatches(methodContext);
        MatchContext match2 = matches.get(matches.size() - 1);

        JumpInsnNode jumpInsnNode2 = match2.captures().get("label").insn().asJump();
        AbstractInsnNode currentInsn = jumpInsnNode2.label;

        // mark a return opcode
        while (currentInsn != null) {
            if (currentInsn.getOpcode() == Opcodes.GOTO) {
                MethodNode methodNode = match.insnContext().methodNode();
                methodNode.instructions.insert(currentInsn, new InsnNode(Opcodes.RETURN));
                methodNode.instructions.remove(currentInsn);
            }

            currentInsn = currentInsn.getNext();
        }

        for (AbstractInsnNode insn : copiedClinit.instructions.toArray()) {
            if (insn instanceof LabelNode)
                break;

            if (insn.getOpcode() == Opcodes.INVOKESTATIC) {
                copiedClinit.instructions.remove(insn);
            } else if (insn instanceof TypeInsnNode typeInsnNode && typeInsnNode.getOpcode() == Opcodes.ANEWARRAY && !typeInsnNode.desc.equals("java/lang/String")) {
                typeInsnNode.desc = "java/lang/String";
            } else if (insn instanceof VarInsnNode varInsnNode &&
                insn.getPrevious() instanceof TypeInsnNode typeInsnNode && typeInsnNode.getOpcode() == Opcodes.ANEWARRAY && typeInsnNode.desc.equals("java/lang/String") &&
                insn.getPrevious().getPrevious() != null && insn.getPrevious().getPrevious().isInteger()
            ) {
                InsnList list = new InsnList();
                list.add(new VarInsnNode(Opcodes.ALOAD, varInsnNode.var));
                list.add(new FieldInsnNode(Opcodes.PUTSTATIC, classNode.name, "arr", "[Ljava/lang/String;"));
                copiedClinit.instructions.insert(insn, list);
            }
        }

        ZelixAsmHelper.renameOwner(classNode, classWrapper);

        classNode.accept(cw);
        return cw.toByteArray();
    }

    record ClassData(ClassWrapper classWrapper, MethodNode clinitMethod, ClassWrapper tempClassWrapper, MatchContext match) { }
}
