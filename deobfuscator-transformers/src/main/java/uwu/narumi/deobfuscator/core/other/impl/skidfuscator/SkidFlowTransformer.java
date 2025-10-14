package uwu.narumi.deobfuscator.core.other.impl.skidfuscator;

import org.objectweb.asm.tree.*;
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.asm.MethodContext;
import uwu.narumi.deobfuscator.api.asm.MethodRef;
import uwu.narumi.deobfuscator.api.asm.matcher.Match;
import uwu.narumi.deobfuscator.api.asm.matcher.group.SequenceMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.JumpMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.MethodMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.NumberMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.OpcodeMatch;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class SkidFlowTransformer extends Transformer {

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

  Match switchJump = SequenceMatch.of(
      NumberMatch.numInteger().capture("salt"),
      MethodMatch.invokeStatic().desc("(I)I").and(Match.of(ctx -> ctx.insn().asMethodInsn().name.length() == 16)).capture("predicate-method"),
      OpcodeMatch.of(LOOKUPSWITCH).capture("switch")
  );

  Match ldcPredicateTryCatch = SequenceMatch.of(
      NumberMatch.numInteger().capture("salt"),
      OpcodeMatch.of(ISTORE).capture("store"),
      OpcodeMatch.of(ILOAD).capture("load"),
      MethodMatch.invokeStatic().desc("(I)I").and(Match.of(ctx -> ctx.insn().asMethodInsn().name.length() == 16)).capture("predicate-method"),
      NumberMatch.numInteger().capture("comparator"),
      JumpMatch.of().capture("jump"),
      OpcodeMatch.of(ACONST_NULL),
      OpcodeMatch.of(ATHROW)
  );

  /**
   * Jump conditional loop
   * ldc 574275566
   * invokestatic qvpewqdjuboryazl/wnvkiiljelvpkhzf.jlkfagybberpshgu (I)I
   * ldc 427176201
   * if_icmpeq L
   */
  Match jumpConditionalLoop = SequenceMatch.of(
      NumberMatch.numInteger().capture("start"),
      MethodMatch.invokeStatic().desc("(I)I").and(Match.of(ctx -> ctx.insn().asMethodInsn().name.length() == 16)).capture("predicate-method"),
      NumberMatch.numInteger().capture("comparator"),
      JumpMatch.of().capture("jump-1"),
      JumpMatch.of().capture("jump-2")
  );

  Match staticDecryption = SequenceMatch.of(
      NumberMatch.numInteger().capture("salt"),
      MethodMatch.invokeStatic().desc("(I)I").and(Match.of(ctx -> ctx.insn().asMethodInsn().name.length() == 16)).capture("predicate-method")
  );

  Match selfClassXor = SequenceMatch.of(
      NumberMatch.numInteger().capture("salt1"),
      NumberMatch.numInteger().capture("salt2"),
      MethodMatch.invokeStatic().desc("(II)I").and(Match.of(ctx -> ctx.insn().asMethodInsn().name.length() == 16)).capture("predicate-method")
  );

  Match setVarJump = SequenceMatch.of(
      NumberMatch.numInteger().capture("salt1"),
      OpcodeMatch.of(ISTORE).capture("var"),
      JumpMatch.of().capture("jump")
  );

  /**
  * Trash tcb
  *      P:
            // try-start:   range=[P-Q] handler=Q:java/io/IOException
            new java/io/IOException
            dup
            invokespecial java/io/IOException.<init> ()V
            athrow
        Q:
            // try-end:     range=[P-Q] handler=Q:java/io/IOException
            // try-handler: range=[P-Q] handler=Q:java/io/IOException
            pop
            ldc 982899003
            istore i140
            goto S
            **/
  Match trashTcb = SequenceMatch.of(
      Match.of(ctx -> ctx.insn() instanceof LabelNode).capture("label"),
      OpcodeMatch.of(NEW),
      OpcodeMatch.of(DUP),
      OpcodeMatch.of(INVOKESPECIAL),
      OpcodeMatch.of(ATHROW),
      Match.of(ctx -> ctx.insn() instanceof LabelNode),
      OpcodeMatch.of(POP)
  ).doNotSkipLabels();


  @Override
  protected void transform() throws Exception {
    ClassWrapper decryptStaticClass = scopedClasses().stream().filter(classWrapper -> classWrapper.name().length() == 33 && (classWrapper.methods().size() == 3 || classWrapper.methods().size() == 2) && classWrapper.methods().stream().map(methodNode -> methodNode.desc).allMatch(desc -> desc.equals("(I)I"))).findFirst().get();
    String method1Name = decryptStaticClass.findMethod(methodNode -> NumberMatch.of(31).findFirstMatch(MethodContext.of(decryptStaticClass, methodNode)) != null).get().name;
    scopedClasses().forEach(classWrapper -> classWrapper.methods().forEach(methodNode -> {
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

      switchJump.findAllMatches(methodContext).forEach(matchContext -> {
        int salt = matchContext.captures().get("salt").insn().asInteger();
        MethodInsnNode methodInsnNode = matchContext.captures().get("predicate-method").insn().asMethodInsn();
        methodNode.instructions.remove(methodInsnNode);

        methodNode.instructions.set(matchContext.captures().get("salt").insn(), new LdcInsnNode(method1Name.equals(methodInsnNode.name) ? m1(salt) : m2_m3(salt)));
        markChange();
      });

      Set<AbstractInsnNode> toRemove = new HashSet<>();
      Set<LabelNode> fakeLabels = new HashSet<>();
      ldcPredicateTryCatch.findAllMatches(methodContext).forEach(matchContext -> {
        int salt = m1(matchContext.captures().get("salt").insn().asInteger());
        int comparator = matchContext.captures().get("comparator").insn().asInteger();
        int storeVar = ((VarInsnNode)matchContext.captures().get("store").insn()).var;
        int loadVar = ((VarInsnNode)matchContext.captures().get("load").insn()).var;
        if (salt == comparator && storeVar == loadVar) {
          toRemove.addAll(matchContext.collectedInsns());
          toRemove.remove(matchContext.captures().get("salt").insn());
          toRemove.remove(matchContext.captures().get("store").insn());
          AbstractInsnNode labelNode = matchContext.captures().get("jump").insn().asJump().label;
          fakeLabels.add((LabelNode) labelNode);
          fakeLabels.add((LabelNode) matchContext.captures().get("load").insn().getPrevious());
          while (labelNode.getNext() != null && !(labelNode.getNext() instanceof LabelNode)) {
            toRemove.add(labelNode.getNext());
            labelNode = labelNode.getNext();
          }
          while (labelNode.getNext() != null && labelNode.getOpcode() != POP) {
            labelNode = labelNode.getNext();
          }
          toRemove.add(labelNode);
          markChange();
        }
      });
      trashTcb.findAllMatches(methodContext).forEach(matchContext -> {
        toRemove.addAll(matchContext.collectedInsns());
        toRemove.remove(matchContext.captures().get("label").insn());
        fakeLabels.add((LabelNode) matchContext.captures().get("label").insn());
      });
      toRemove.forEach(methodNode.instructions::remove);
      methodNode.tryCatchBlocks.removeIf(tcb -> fakeLabels.contains(tcb.start) || fakeLabels.contains(tcb.handler) || fakeLabels.contains(tcb.end));
      jumpConditionalLoop.findAllMatches(methodContext).forEach(matchContext -> {
        MethodInsnNode methodInsnNode = matchContext.captures().get("predicate-method").insn().asMethodInsn();
        int start = method1Name.equals(methodInsnNode.name) ? m1(matchContext.captures().get("start").insn().asInteger()) : m2_m3(matchContext.captures().get("start").insn().asInteger());
        int comparator = matchContext.captures().get("comparator").insn().asInteger();
        JumpInsnNode validJump = matchContext.captures().get("jump-1").insn().asJump();
        JumpInsnNode fakeJump = matchContext.captures().get("jump-2").insn().asJump();
        if (start == comparator) {
          validJump = matchContext.captures().get("jump-2").insn().asJump();
          fakeJump = matchContext.captures().get("jump-1").insn().asJump();
        }
        methodNode.instructions.insert(validJump, new JumpInsnNode(GOTO, validJump.label));
        matchContext.removeAll();
        AbstractInsnNode fakeJumpTarget = fakeJump.label;
        toRemove.clear();
        while (fakeJumpTarget.getNext() != null && !(fakeJumpTarget.getNext() instanceof LabelNode)) {
          if (toRemove.contains(fakeJumpTarget)) break;
          toRemove.add(fakeJumpTarget.getNext());
          if (fakeJumpTarget.getNext().isJump()) {
            fakeJumpTarget = fakeJumpTarget.getNext().asJump().label;
          } else {
            fakeJumpTarget = fakeJumpTarget.getNext();
          }
        }
        toRemove.forEach(methodNode.instructions::remove);
        markChange();
      });
      staticDecryption.findAllMatches(methodContext).forEach(matchContext -> {
        MethodInsnNode methodInsnNode = matchContext.captures().get("predicate-method").insn().asMethodInsn();
        AbstractInsnNode salt = matchContext.captures().get("salt").insn();
        int decrypted = method1Name.equals(methodInsnNode.name) ? m1(salt.asInteger()) : m2_m3(salt.asInteger());
        methodNode.instructions.set(salt, new LdcInsnNode(decrypted));
        methodNode.instructions.remove(methodInsnNode);
        markChange();
      });

      selfClassXor.findAllMatches(methodContext).forEach(matchContext -> {
        MethodInsnNode methodInsnNode = matchContext.captures().get("predicate-method").insn().asMethodInsn();
        findMethod(classWrapper.classNode(), MethodRef.of(methodInsnNode)).ifPresent(xorMethod -> {
          AbstractInsnNode salt1 = matchContext.captures().get("salt1").insn();
          AbstractInsnNode salt2 = matchContext.captures().get("salt2").insn();
          methodNode.instructions.set(salt1, new LdcInsnNode(salt2.asInteger() ^ salt1.asInteger()));
          methodNode.instructions.remove(methodInsnNode);
          methodNode.instructions.remove(salt2);
          markChange();
        });
      });

      JumpMatch.of().capture("jump").findAllMatches(methodContext).forEach(matchContext -> {
        JumpInsnNode jumpInsnNode = matchContext.captures().get("jump").insn().asJump();
        if (jumpInsnNode.label.getNext() instanceof LdcInsnNode intLdc && jumpInsnNode.label.getNext(2) instanceof VarInsnNode varStore && jumpInsnNode.label.getNext(3) instanceof JumpInsnNode jump1) {
          methodNode.instructions.remove(varStore);
          methodNode.instructions.remove(intLdc);
          methodNode.instructions.insert(jump1.label, varStore);
          methodNode.instructions.insert(jump1.label, intLdc);
          markChange();
        }
        LdcInsnNode lastLdcOfLabel = null;
        AbstractInsnNode node = jumpInsnNode;
        while (node != null && !(node.getNext() instanceof LabelNode)) {
          if (node instanceof LdcInsnNode ldc) {
            lastLdcOfLabel = ldc;
          }
          node = node.getNext();
        }
        if (lastLdcOfLabel != null) {
          LdcInsnNode finalLastLdcOfLabel = lastLdcOfLabel;
          setVarJump.findAllMatches(methodContext).forEach(matchContext1 -> {
            if (finalLastLdcOfLabel.cst instanceof Integer && matchContext1.captures().get("salt1").insn().asInteger() == (int) finalLastLdcOfLabel.cst) {
              VarInsnNode var = (VarInsnNode) matchContext1.captures().get("var").insn();
              methodNode.instructions.insert(matchContext1.captures().get("jump").insn().asJump().label, new VarInsnNode(ISTORE, var.var));
              methodNode.instructions.insert(matchContext1.captures().get("jump").insn().asJump().label, new LdcInsnNode(finalLastLdcOfLabel.cst));
            }
          });
        }
      });


      methodNode.tryCatchBlocks.forEach(tcb -> {
        try {
          if (tcb.start.getPrevious() instanceof VarInsnNode var && tcb.start.getPrevious(2) instanceof LdcInsnNode ldc) {
            if (tcb.handler.getNext() instanceof VarInsnNode varLoad) {
              if (var.var == varLoad.var) {
                methodNode.instructions.insertBefore(varLoad, new LdcInsnNode(ldc.cst));
                methodNode.instructions.insertBefore(varLoad, new VarInsnNode(ISTORE, var.var));
                markChange();
              }
            }
          }
          if (tcb.end.getNext() instanceof JumpInsnNode jump) {
            if (jump.label.getNext() instanceof LdcInsnNode intLdc && jump.label.getNext(2) instanceof VarInsnNode varStore && jump.label.getNext(3) instanceof JumpInsnNode jump1) {
              jump.label = jump1.label;
              methodNode.instructions.remove(varStore);
              methodNode.instructions.remove(intLdc);
              methodNode.instructions.remove(jump1);
              methodNode.instructions.insert(jump.label, varStore);
              methodNode.instructions.insert(jump.label, intLdc);
              markChange();
            }
          }
          if (tcb.end.getNext() instanceof LdcInsnNode intLdc && tcb.end.getNext(2) instanceof VarInsnNode varStore && tcb.end.getNext(3) instanceof JumpInsnNode jump1) {
            methodNode.instructions.remove(varStore);
            methodNode.instructions.remove(intLdc);
            methodNode.instructions.insert(jump1.label, varStore);
            methodNode.instructions.insert(jump1.label, intLdc);
            markChange();
          }

          } catch (Exception e) {
          e.printStackTrace();
        }
      });
    }));
  }


  public int m1(int n) {
    if (n != 0) {
      return (n * 31 >>> 4) % n ^ n >>> 16;
    }
    return 0;
  }

  public int m2_m3(int n) {
    return (n & 0xE0000000) >> 29 | n << 3;
  }
}
