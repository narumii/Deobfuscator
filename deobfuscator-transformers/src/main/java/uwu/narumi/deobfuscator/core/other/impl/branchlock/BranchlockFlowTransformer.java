package uwu.narumi.deobfuscator.core.other.impl.branchlock;

import org.objectweb.asm.tree.*;
import uwu.narumi.deobfuscator.api.asm.MethodContext;
import uwu.narumi.deobfuscator.api.asm.matcher.Match;
import uwu.narumi.deobfuscator.api.asm.matcher.group.SequenceMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.JumpMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.NumberMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.OpcodeMatch;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BranchlockFlowTransformer extends Transformer {

  Match[] flowDupEquationMatches = new Match[] {
    SequenceMatch.of(
        OpcodeMatch.of(DUP),
        JumpMatch.of().capture("fake-jump"),
        JumpMatch.of().capture("correct-jump"),
        NumberMatch.of()
    ),
    SequenceMatch.of(
        OpcodeMatch.of(DUP),
        OpcodeMatch.of(SWAP),
        JumpMatch.of().capture("fake-jump"),
        JumpMatch.of().capture("correct-jump"),
        NumberMatch.of()
    )
  };

  Match flowDupJumpMatch = SequenceMatch.of(
      OpcodeMatch.of(DUP),
      JumpMatch.of().capture("fake-jump"),
      OpcodeMatch.of(ASTORE).capture("correct-store"),
      OpcodeMatch.of(ALOAD),
      JumpMatch.of()
  );

  Match flowDupJumpMatch2 = SequenceMatch.of(
      OpcodeMatch.of(DUP),
      JumpMatch.of().capture("fake-jump"),
      OpcodeMatch.of(ASTORE).capture("correct-store")
  );

  Match flowFakeLoop = SequenceMatch.of(
      OpcodeMatch.of(ASTORE).capture("correct-store"),
      OpcodeMatch.of(ALOAD).capture("fake-load"),
      JumpMatch.of()
  );

  Match errorJump = SequenceMatch.of(
      OpcodeMatch.of(ALOAD).capture("loaded-var"),
      JumpMatch.of()
  );

  Match trashHandlers = SequenceMatch.of(
      Match.of(ctx -> (ctx.insn().getOpcode() >= IRETURN && ctx.insn().getOpcode() <= RETURN) || ctx.insn().getOpcode() == ATHROW),
      Match.of(ctx -> (ctx.insn().getOpcode() >= ACONST_NULL && ctx.insn().getOpcode() <= DCONST_1) || ctx.insn().getOpcode() == ALOAD || ctx.insn() instanceof LabelNode)
  ).doNotSkipLabels();

  @Override
  protected void transform() throws Exception {
    scopedClasses().forEach(classWrapper -> classWrapper.methods().forEach(methodNode -> {
      MethodContext methodContext = MethodContext.of(classWrapper, methodNode);
      List<TryCatchBlockNode> tryCatchBlocks = methodNode.tryCatchBlocks;
      if (tryCatchBlocks != null && !tryCatchBlocks.isEmpty()) {
        Set<AbstractInsnNode> toRemove = new HashSet<>();
        Set<LabelNode> trashLabels = new HashSet<>();
        for (Match flowDupEquationMatch : flowDupEquationMatches) {
          flowDupEquationMatch.findAllMatches(methodContext).forEach(matchContext -> {
            if (matchContext.captures().containsKey("swap")) System.out.println(matchContext.captures().containsKey("swap"));
            LabelNode labelNode = matchContext.captures().get("fake-jump").insn().asJump().label;
            if (labelNode != null && labelNode.getNext() != null && labelNode.getNext() instanceof FrameNode && labelNode.getNext(2) != null && labelNode.getNext(2).getOpcode() == POP) {
              toRemove.add(labelNode);
              trashLabels.add(labelNode);
              toRemove.add(labelNode.getNext());
              toRemove.add(labelNode.getNext(2));
              markChange();
            } else if (labelNode != null && labelNode.getNext() != null && labelNode.getNext().getOpcode() == POP) {
              toRemove.add(labelNode);
              trashLabels.add(labelNode);
              toRemove.add(labelNode.getNext());
              markChange();
            }
            toRemove.addAll(matchContext.collectedInsns());
            toRemove.remove(matchContext.captures().get("correct-jump").insn());
            markChange();
          });
        }
        flowFakeLoop.findAllMatches(methodContext).forEach(matchContext -> {
          VarInsnNode varInsnNode = (VarInsnNode) matchContext.captures().get("correct-store").insn();
          VarInsnNode varInsnNode1 = (VarInsnNode) matchContext.captures().get("fake-load").insn();
          if (varInsnNode.var != varInsnNode1.var) {
            toRemove.addAll(matchContext.collectedInsns());
            toRemove.remove(matchContext.captures().get("correct-store").insn());
            markChange();
          }
        });
        JumpInsnNode jumpInsnNode = null;
        try {
          if ((methodNode.instructions.get(0) instanceof LabelNode && methodNode.instructions.get(1) instanceof JumpInsnNode)) {
            jumpInsnNode = methodNode.instructions.get(1).asJump();
            trashLabels.add((LabelNode) methodNode.instructions.get(0));
          } else if (methodNode.instructions.get(0) instanceof JumpInsnNode) {
            jumpInsnNode = methodNode.instructions.get(0).asJump();
          }
          if (jumpInsnNode != null) {
            int i = 0;
            if ((jumpInsnNode.label.getNext().isVarLoad() && ((VarInsnNode)jumpInsnNode.label.getNext()).var == 0 || (jumpInsnNode.label.getNext().getOpcode() >= ACONST_NULL && jumpInsnNode.label.getNext().getOpcode() <= DCONST_1))) {
              while (jumpInsnNode.label.getNext(i) != null && !jumpInsnNode.label.getNext(i).isVarStore()) {
                i++;
              }
              VarInsnNode flowVarIndex = (VarInsnNode) jumpInsnNode.label.getNext(i);
              if (flowVarIndex != null) {
                errorJump.findAllMatches(methodContext).forEach(matchContext -> {
                  if (((VarInsnNode) matchContext.captures().get("loaded-var").insn()).var == flowVarIndex.var) {
                    toRemove.addAll(matchContext.collectedInsns());
                  }
                  markChange();
                });
              }
              toRemove.add(jumpInsnNode);
              jumpInsnNode = null;
            }
          }
        } catch (Exception exception) {
          exception.printStackTrace();
        }
        flowDupJumpMatch.findAllMatches(methodContext).forEach(matchContext -> {
          LabelNode labelNode = matchContext.captures().get("fake-jump").insn().asJump().label;
          if (labelNode != null && labelNode.getNext() != null && labelNode.getNext() instanceof FrameNode && labelNode.getNext(2) != null && labelNode.getNext(2).getOpcode() == GOTO) {
            toRemove.add(labelNode);
            trashLabels.add(labelNode);
            toRemove.add(labelNode.getNext());
            toRemove.add(labelNode.getNext(2));
            markChange();
          }
          toRemove.addAll(matchContext.collectedInsns());
          toRemove.remove(matchContext.captures().get("correct-store").insn());
          markChange();
        });
        flowDupJumpMatch2.findAllMatches(methodContext).forEach(matchContext -> {
          LabelNode labelNode = matchContext.captures().get("fake-jump").insn().asJump().label;
          if (labelNode != null && labelNode.getNext() != null && labelNode.getNext().getOpcode() == GOTO) {
            toRemove.add(labelNode);
            trashLabels.add(labelNode);
            toRemove.add(labelNode.getNext());
            markChange();
          }
          toRemove.addAll(matchContext.collectedInsns());
          toRemove.remove(matchContext.captures().get("correct-store").insn());
          markChange();
        });

        try {
          AbstractInsnNode returnNode = trashHandlers.findFirstMatch(methodContext).insn();

          AbstractInsnNode next = returnNode.getNext();
          while (next != null) {
            if (next instanceof LabelNode label) {
              trashLabels.add(label);
            }
            next = next.getNext();
          }
        } catch (Exception ignored) {}

        toRemove.forEach(
            methodNode.instructions::remove
        );

        boolean changed = true;

        if (jumpInsnNode != null) {
          AbstractInsnNode next = jumpInsnNode.label;
          while (next != null) {
            if (next instanceof LabelNode label) {
              trashLabels.add(label);
            }
            next = next.getNext();
          }
          while (jumpInsnNode.label.getNext() != null && !(jumpInsnNode.label.getNext() instanceof JumpInsnNode)) {
            AbstractInsnNode abstractInsnNode = jumpInsnNode.label.getNext();
            if (abstractInsnNode instanceof LabelNode) {
              methodNode.instructions.remove(abstractInsnNode);
              continue;
            }
            methodNode.instructions.remove(abstractInsnNode);
            methodNode.instructions.insertBefore(jumpInsnNode, abstractInsnNode);
            markChange();
          }

          methodNode.instructions.remove(jumpInsnNode);
          markChange();
        } else {
          changed = !toRemove.isEmpty();
        }

        if (!changed) return;

        Set<TryCatchBlockNode> tryCatchBlockNodes = new HashSet<>();

        for (TryCatchBlockNode tryCatchBlock : tryCatchBlocks) {
          if (trashLabels.contains(tryCatchBlock.start) || trashLabels.contains(tryCatchBlock.handler) || trashLabels.contains(tryCatchBlock.end)) {
            tryCatchBlockNodes.add(tryCatchBlock);
            markChange();
          }
        }

        tryCatchBlockNodes.forEach(
            methodNode.tryCatchBlocks::remove
        );
      }
    }));
  }
}
