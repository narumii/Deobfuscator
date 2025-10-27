package uwu.narumi.deobfuscator.core.other.impl.skidfuscator;

import org.objectweb.asm.tree.*;
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.asm.MethodContext;
import uwu.narumi.deobfuscator.api.asm.MethodRef;
import uwu.narumi.deobfuscator.api.asm.matcher.Match;
import uwu.narumi.deobfuscator.api.asm.matcher.MatchContext;
import uwu.narumi.deobfuscator.api.asm.matcher.group.SequenceMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.JumpMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.MethodMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.NumberMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.OpcodeMatch;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

import java.util.HashSet;
import java.util.Set;

public class SkidFlowTransformer extends Transformer {

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


  private static ClassWrapper decryptStaticClass;
  private static String method1Name;

  @Override
  protected void transform() throws Exception {
    if (decryptStaticClass == null) decryptStaticClass = scopedClasses().stream().filter(classWrapper -> classWrapper.name().length() == 33 && (classWrapper.methods().size() == 3 || classWrapper.methods().size() == 2) && classWrapper.methods().stream().map(methodNode -> methodNode.desc).allMatch(desc -> desc.equals("(I)I"))).findFirst().get();
    if (method1Name == null) method1Name = decryptStaticClass.findMethod(methodNode -> NumberMatch.of(31).findFirstMatch(MethodContext.of(decryptStaticClass, methodNode)) != null).get().name;
    scopedClasses().forEach(classWrapper -> classWrapper.methods().forEach(methodNode -> {
      MethodContext methodContext = MethodContext.of(classWrapper, methodNode);

      /* Example: var4 = afglgogiwinhilog(var4, 1845606652); */
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

      /** Example
       *   ldc 952006311
       *   invokestatic ikjlkypytwnvkuzi/erfobcthglyygbhq.jwmyqfvhltjhsidh (I)I
       * */
      staticDecryption.findAllMatches(methodContext).forEach(matchContext -> {
        MethodInsnNode methodInsnNode = matchContext.captures().get("predicate-method").insn().asMethodInsn();
        AbstractInsnNode salt = matchContext.captures().get("salt").insn();
        int decrypted = method1Name.equals(methodInsnNode.name) ? m1(salt.asInteger()) : m2_m3(salt.asInteger());
        methodNode.instructions.set(salt, new LdcInsnNode(decrypted));
        methodNode.instructions.remove(methodInsnNode);
        markChange();
      });

      /* Its moving hash LDC out of context for example, when IF/ELSE ends with same LDC and InlineVariable cant push var to next instructions because it has no context */
      JumpMatch.of().capture("jump").findAllMatches(methodContext).forEach(matchContext -> {
        JumpInsnNode jumpInsnNode = matchContext.captures().get("jump").insn().asJump();
        if (jumpInsnNode.label.getNext() instanceof LdcInsnNode intLdc && jumpInsnNode.label.getNext(2) instanceof VarInsnNode varStore && jumpInsnNode.label.getNext(3) instanceof JumpInsnNode jump1) {
          if (blessedJumpLabels.contains(jump1.label)) return;
          methodNode.instructions.remove(varStore);
          methodNode.instructions.remove(intLdc);
          methodNode.instructions.insert(jump1.label, varStore);
          methodNode.instructions.insert(jump1.label, intLdc);
          blessedJumpLabels.add(jump1.label);
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
              LabelNode blessedLabel = matchContext1.captures().get("jump").insn().asJump().label;
              if (blessedJumpLabels.contains(blessedLabel)) return;
              methodNode.instructions.insert(matchContext1.captures().get("jump").insn().asJump().label, new VarInsnNode(ISTORE, var.var));
              methodNode.instructions.insert(matchContext1.captures().get("jump").insn().asJump().label, new LdcInsnNode(finalLastLdcOfLabel.cst));
              blessedJumpLabels.add(blessedLabel);
            }
          });
        }
      });

      if (!isChanged()) {
        MatchContext matchContext = SequenceMatch.of(NumberMatch.numInteger().capture("hash"), OpcodeMatch.of(ISTORE).capture("param"), Match.of(ctx -> ctx.insn() instanceof LabelNode).capture("label")).doNotSkipLabels().findFirstMatch(methodContext);
        if (matchContext != null) {
          int salt = matchContext.captures().get("hash").insn().asInteger();
          int param = ((VarInsnNode)matchContext.captures().get("param").insn()).var;
          LabelNode label = (LabelNode) matchContext.captures().get("label").insn();
          if (label.getNext() instanceof JumpInsnNode jumpInsnNode) label = jumpInsnNode.label;
          if (blessedLabels.contains(label)) return;
          LabelNode finalLabel = label;
          methodNode.tryCatchBlocks.forEach(tcb -> {
            if (finalLabel.equals(tcb.start) || finalLabel.equals(tcb.handler) || finalLabel.equals(tcb.end)) {
              methodNode.instructions.insert(tcb.start, new VarInsnNode(ISTORE, param));
              methodNode.instructions.insert(tcb.start, new LdcInsnNode(salt));
              methodNode.instructions.insert(tcb.handler, new VarInsnNode(ISTORE, param));
              methodNode.instructions.insert(tcb.handler, new LdcInsnNode(salt));
              methodNode.instructions.insert(tcb.end, new VarInsnNode(ISTORE, param));
              methodNode.instructions.insert(tcb.end, new LdcInsnNode(salt));
              blessedLabels.add(tcb.start);
              blessedLabels.add(tcb.handler);
              blessedLabels.add(tcb.end);
            }
          });
          methodNode.instructions.insert(label, new VarInsnNode(ISTORE, param));
          methodNode.instructions.insert(label, new LdcInsnNode(salt));
          markChange();
          blessedLabels.add(label);
        }
      }
    }));
  }

  private final static Set<LabelNode> blessedLabels = new HashSet<>();
  private final static Set<LabelNode> blessedJumpLabels = new HashSet<>();

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