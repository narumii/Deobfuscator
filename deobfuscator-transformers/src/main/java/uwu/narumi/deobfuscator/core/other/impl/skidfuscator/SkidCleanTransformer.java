package uwu.narumi.deobfuscator.core.other.impl.skidfuscator;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.asm.FieldRef;
import uwu.narumi.deobfuscator.api.asm.MethodContext;
import uwu.narumi.deobfuscator.api.asm.matcher.Match;
import uwu.narumi.deobfuscator.api.asm.matcher.group.SequenceMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.FieldMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.MethodMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.NumberMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.OpcodeMatch;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class SkidCleanTransformer extends Transformer {

  /**
   * Useless storing vars:
   *  getstatic java/lang/System.out Ljava/io/PrintStream;
   *  astore v29 - useless
   *  invokestatic pack/tests/basics/accu/Digi.hwahaewjduusqug ()[B
   *  iload i39
   *  invokestatic pack/tests/basics/accu/Digi.shmyiqcznk ([BI)Ljava/lang/String;
   *  astore v36 - useless
   *  aload v29 - useless
   *  aload v36 - useless
   *  invokevirtual java/io/PrintStream.println (Ljava/lang/String;)V
   */
  Match uselessStoring = SequenceMatch.of(
      Match.of(ctx -> true),
      OpcodeMatch.of(ASTORE).capture("store-1"),
      OpcodeMatch.of(LDC),
      OpcodeMatch.of(ASTORE).capture("store-2"),
      OpcodeMatch.of(ALOAD).capture("load-1"),
      OpcodeMatch.of(ALOAD).capture("load-2"),
      MethodMatch.create()
  );

  @Override
  protected void transform() throws Exception {
    /* Delete static decryption class */
    ClassWrapper decryptStaticClass = scopedClasses().stream().filter(classWrapper -> classWrapper.name().length() == 33 && (classWrapper.methods().size() == 3 || classWrapper.methods().size() == 2) && classWrapper.methods().stream().map(methodNode -> methodNode.desc).allMatch(desc -> desc.equals("(I)I"))).findFirst().get();
    context().getClassesMap().remove(decryptStaticClass.name());
    scopedClasses().forEach(classWrapper -> {
      /* Clean ASCII aheagos/etc */
      findClInit(classWrapper.classNode()).ifPresent(clinit -> {
        AbstractInsnNode firstInsn = clinit.instructions.getFirst();
        boolean hasASCII = false;
        while (firstInsn.getNext() != null) {
          if (firstInsn.isInteger() && firstInsn.getNext().getOpcode() == ANEWARRAY && firstInsn.getNext(2).asFieldInsn().name.equalsIgnoreCase("nothing_to_see_here")) {
            hasASCII = true;
            break;
          }
          firstInsn = firstInsn.getNext();
        }

        Set<AbstractInsnNode> toRemove = new HashSet<>();
        if (hasASCII) {
          toRemove.add(firstInsn);
          toRemove.add(firstInsn.getNext());
          toRemove.add(firstInsn.getNext(2));
          firstInsn = firstInsn.getNext(3);
          while (firstInsn != null && firstInsn.getOpcode() == GETSTATIC && firstInsn.getNext() != null && firstInsn.getNext().isInteger() && firstInsn.getNext(2) != null && firstInsn.getNext(2).isString() && firstInsn.getNext(3) != null && firstInsn.getNext(3).getOpcode() == AASTORE) {
            toRemove.add(firstInsn);
            toRemove.add(firstInsn.getNext());
            toRemove.add(firstInsn.getNext(2));
            toRemove.add(firstInsn.getNext(3));
            firstInsn = firstInsn.getNext(4);
          }
          toRemove.forEach(clinit.instructions::remove);
          classWrapper.fields().removeIf(fieldNode -> fieldNode.name.equals("nothing_to_see_here") && fieldNode.desc.equals("[Ljava/lang/String;"));
          markChange();
        }
      });

      /* Clear <init> hash-int */
      classWrapper.fields().removeIf(fieldNode -> {
        boolean matchField = isAccess(fieldNode.access, ACC_PRIVATE, ACC_TRANSIENT) && fieldNode.desc.equals("I");
        if (matchField) {
          classWrapper.methods().forEach(methodNode1 -> {
            if (methodNode1.name.equals("<init>")) {
              SequenceMatch.of(
                  NumberMatch.of(),
                  FieldMatch.putField().fieldRef(FieldRef.of(classWrapper.classNode(), fieldNode))
              ).findAny(MethodContext.of(classWrapper, methodNode1)).ifPresent(matchContext -> {
                matchContext.removeAll();
                markChange();
              });
            }
          });
        }
        return matchField;
      });

      /* Clear self-class-xor from flow-obfuscation */
      classWrapper.methods().removeIf(methodNode -> methodNode.name.length() == 16 && isAccess(methodNode.access, ACC_PRIVATE, ACC_STATIC) && methodNode.desc.equals("(II)I") && methodNode.instructions.size() == 4);

      classWrapper.methods().forEach(methodNode -> {
        MethodContext methodContext = MethodContext.of(classWrapper, methodNode);

        uselessStoring.findAllMatches(methodContext).forEach(matchContext -> {
          VarInsnNode store1 = (VarInsnNode) matchContext.captures().get("store-1").insn();
          VarInsnNode store2 = (VarInsnNode) matchContext.captures().get("store-2").insn();
          VarInsnNode load1 = (VarInsnNode) matchContext.captures().get("load-1").insn();
          VarInsnNode load2 = (VarInsnNode) matchContext.captures().get("load-2").insn();
          if (store1.var == load1.var && store2.var == load2.var) {
            if (Arrays.stream(methodNode.instructions.toArray()).filter(insn -> !insn.equals(load1)).noneMatch(insn -> insn.getOpcode() == ALOAD && ((VarInsnNode)insn).var == store1.var)) {
              methodNode.instructions.remove(store1);
              methodNode.instructions.remove(load1);
              methodNode.instructions.remove(store2);
              methodNode.instructions.remove(load2);
              markChange();
            }
          }
        });
      });
    });
  }
}
