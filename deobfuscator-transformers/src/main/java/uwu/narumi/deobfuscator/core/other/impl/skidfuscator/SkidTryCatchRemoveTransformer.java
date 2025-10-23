package uwu.narumi.deobfuscator.core.other.impl.skidfuscator;

import org.objectweb.asm.tree.*;
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.asm.MethodContext;
import uwu.narumi.deobfuscator.api.asm.matcher.Match;
import uwu.narumi.deobfuscator.api.asm.matcher.group.AnyMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.group.SequenceMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.JumpMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.MethodMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.OpcodeMatch;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

import java.util.HashSet;
import java.util.Set;

public class SkidTryCatchRemoveTransformer extends Transformer {
  @Override
  protected void transform() throws Exception {
    scopedClasses().forEach(classWrapper -> classWrapper.methods().forEach(methodNode -> resolveFakeTcb(classWrapper, methodNode)));
  }

  /**
   * Skid's transformer basically does some whacky stuff:
   * Source: https://github.com/skidfuscatordev/skidfuscator-java-obfuscator/blob/master/dev.skidfuscator.obfuscator/src/main/java/dev/skidfuscator/obfuscator/transform/impl/flow/BasicRangeTransformer.java
   *
   * Original flow:       Obfuscated Flow:
   *
   * ┌─────────┐            ┌─────────┐
   * │ Block A │            │ Block A │
   * └────┬────┘            └────┬────┘
   *      │                      │
   * ┌────▼────┐         ┌───────▼────────┐
   * │ Block B │         │ Random If Stmt │
   * └─────────┘         └───────┬────────┘
   *                             │
   *                   ┌─────┐◄──┴───►┌─────┐
   *                   │ Yes │        │ No  │
   *                   └─────┘        └──┬──┘
   *                                     │
   *                               ┌─────▼─────┐
   *                               │ Exception │
   *                               └───────────┘
   *
   *                      ┌─────────────┐
   *                      │  Exception  │
   *                      │   Catcher   │
   *                      └──────┬──────┘
   *                             │
   *                        ┌────▼────┐
   *                        │ Block B │
   *                        └─────────┘
   */
  public void resolveFakeTcb(ClassWrapper classWrapper, MethodNode methodNode) {
    MethodContext methodContext = MethodContext.of(classWrapper, methodNode);
    Set<TryCatchBlockNode> tcbToRemove = new HashSet<>();
    Set<AbstractInsnNode> toRemove = new HashSet<>();
    methodNode.tryCatchBlocks.forEach(tcb -> {
      /* Checking if start block has throw null */
      SequenceMatch.of(
          AnyMatch.of(OpcodeMatch.of(ILOAD), OpcodeMatch.of(LDC)).and(Match.of(ctx -> isInsnInLabelRange(methodNode, tcb.start, ctx.insn()))),
          MethodMatch.invokeStatic(),
          OpcodeMatch.of(LDC),
          JumpMatch.of().capture("throw-label"),
          OpcodeMatch.of(ACONST_NULL),
          OpcodeMatch.of(ATHROW)
      ).or(
          SequenceMatch.of(
              OpcodeMatch.of(NEW).and(Match.of(ctx -> isInsnInLabelRange(methodNode, tcb.start, ctx.insn()))),
              OpcodeMatch.of(DUP),
              MethodMatch.invokeSpecial(),
              OpcodeMatch.of(ATHROW)
          )
      ).findAny(methodContext).ifPresent(matchContext -> {
        /* Clearing "else" block which throws exception */
        toRemove.addAll(matchContext.collectedInsns());
        LabelNode fakeJump;
        if (matchContext.captures().containsKey("throw-label")) {
          fakeJump = matchContext.captures().get("throw-label").insn().asJump().label;
          toRemove.add(fakeJump);
        } else {
          fakeJump = tcb.handler;
        }
        AbstractInsnNode abstractInsnNode = fakeJump;
        while (abstractInsnNode.getNext() != null && abstractInsnNode.getOpcode() != POP) {
          abstractInsnNode = abstractInsnNode.getNext();
        }
        toRemove.add(abstractInsnNode);
        tcbToRemove.add(tcb);
        methodNode.instructions.insert(tcb.start, new JumpInsnNode(GOTO, tcb.handler));
        markChange();
      });
    });
    toRemove.forEach(methodNode.instructions::remove);
    methodNode.tryCatchBlocks.removeAll(tcbToRemove);
  }
}
