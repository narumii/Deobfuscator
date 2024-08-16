package uwu.narumi.deobfuscator.core.other.impl.universal;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.SourceValue;
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.context.Context;
import uwu.narumi.deobfuscator.api.helper.AsmMathHelper;
import uwu.narumi.deobfuscator.api.helper.StackWalker;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class UniversalFlowTransformer extends Transformer {

  @Override
  public void transform(ClassWrapper scope, Context context) throws Exception {
    context.classes(scope).forEach(classWrapper -> classWrapper.methods().forEach(methodNode -> {
      simplifyCompareInstructions(classWrapper, methodNode);
      simplifyJumpInstructions(classWrapper, methodNode);
    }));
  }

  // TODO: Incomplete. Move to UniversalNumberTransformer during its rewrite.
  private void simplifyCompareInstructions(ClassWrapper classWrapper, MethodNode methodNode) {
    Map<AbstractInsnNode, Frame<SourceValue>> frames = analyzeSource(classWrapper.getClassNode(), methodNode);
    if (frames == null) return;

    // Simplify 'compare' instructions
    for (AbstractInsnNode insn : methodNode.instructions) {
      if (insn.getOpcode() == LCMP) {
        Frame<SourceValue> frame = frames.get(insn);
        if (frame == null) continue;

        // Get instructions from stack that are passed to LCMP
        Optional<StackWalker.StackValue> valueInsn1 = StackWalker.getOnlyOnePossibleProducer(frames, frame.getStack(frame.getStackSize() - 2));
        Optional<StackWalker.StackValue> valueInsn2 = StackWalker.getOnlyOnePossibleProducer(frames, frame.getStack(frame.getStackSize() - 1));
        if (valueInsn1.isEmpty() || valueInsn2.isEmpty()) continue;

        if (valueInsn1.get().value() instanceof LdcInsnNode ldcValue1 && valueInsn2.get().value() instanceof LdcInsnNode ldcValue2) {
          long value1 = (long) ldcValue1.cst;
          long value2 = (long) ldcValue2.cst;
          if (value1 > value2) {
            // Result: 1
            methodNode.instructions.set(insn, new InsnNode(ICONST_1));
          } else if (value1 < value2) {
            // Result: -1
            methodNode.instructions.set(insn, new InsnNode(ICONST_M1));
          } else {
            // Result: 0
            methodNode.instructions.set(insn, new InsnNode(ICONST_0));
          }
          valueInsn1.get().remove(methodNode);
          valueInsn2.get().remove(methodNode);
        }
      }
    }
  }

  // TODO: Add LOOKUPSWITCH and TABLESWITCH
  private void simplifyJumpInstructions(ClassWrapper classWrapper, MethodNode methodNode) {
    Map<AbstractInsnNode, Frame<SourceValue>> frames = analyzeSource(classWrapper.getClassNode(), methodNode);
    if (frames == null) return;

    // Simplify 'jump' instructions
    for (AbstractInsnNode insn : methodNode.instructions) {
      if (AsmMathHelper.isOneValueCondition(insn.getOpcode())) {
        // One-value if statement

        Frame<SourceValue> frame = frames.get(insn);
        if (frame == null) continue;

        JumpInsnNode jumpInsn = (JumpInsnNode) insn;

        // Get instruction from stack that is passed to if statement
        Optional<StackWalker.StackValue> value = StackWalker.getOnlyOnePossibleProducer(frames, frame.getStack(frame.getStackSize() - 1));
        if (value.isEmpty()) continue;

        // Process if statement
        if (value.get().value().isInteger()) {
          boolean ifResult = AsmMathHelper.condition(
              value.get().value().asInteger(), // Value
              jumpInsn.getOpcode() // Opcode
          );

          // Correctly transform redundant if statement
          processRedundantIfStatement(methodNode, jumpInsn, ifResult);

          // Cleanup value
          value.get().remove(methodNode);
        }
      } else if (AsmMathHelper.isTwoValuesCondition(insn.getOpcode())) {
        // Two-value if statements

        Frame<SourceValue> frame = frames.get(insn);
        if (frame == null) continue;

        JumpInsnNode jumpInsn = (JumpInsnNode) insn;

        // Get instructions from stack that are passed to if statement
        Optional<StackWalker.StackValue> value1 = StackWalker.getOnlyOnePossibleProducer(frames, frame.getStack(frame.getStackSize() - 2));
        Optional<StackWalker.StackValue> value2 = StackWalker.getOnlyOnePossibleProducer(frames, frame.getStack(frame.getStackSize() - 1));
        if (value1.isEmpty() || value2.isEmpty()) continue;

        // Process if statement
        if (value1.get().value().isInteger() && value2.get().value().isInteger()) {
          boolean ifResult = AsmMathHelper.condition(
              value1.get().value().asInteger(), // First value
              value2.get().value().asInteger(), // Second value
              jumpInsn.getOpcode() // Opcode
          );

          // Correctly transform redundant if statement
          processRedundantIfStatement(methodNode, jumpInsn, ifResult);

          // Cleanup values
          value1.get().remove(methodNode);
          value2.get().remove(methodNode);
        }
      }
    }
  }

  private void processRedundantIfStatement(MethodNode methodNode, JumpInsnNode ifStatement, boolean ifResult) {
    if (!ifResult) {
      // Remove unreachable if statement
      methodNode.instructions.remove(ifStatement);
    } else {
      // Replace always reachable if statement with GOTO
      ifStatement.setOpcode(GOTO);
    }
  }
}
