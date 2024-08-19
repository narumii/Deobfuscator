package uwu.narumi.deobfuscator.core.other.impl.universal;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.OriginalSourceValue;
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.context.Context;
import uwu.narumi.deobfuscator.api.helper.AsmMathHelper;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

import java.util.Map;

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
    Map<AbstractInsnNode, Frame<OriginalSourceValue>> frames = analyzeOriginalSource(classWrapper.getClassNode(), methodNode);
    if (frames == null) return;

    // Simplify 'compare' instructions
    for (AbstractInsnNode insn : methodNode.instructions) {
      if (insn.getOpcode() == LCMP) {
        Frame<OriginalSourceValue> frame = frames.get(insn);
        if (frame == null) continue;

        // Get instructions from stack that are passed to LCMP
        OriginalSourceValue value1SourceValue = frame.getStack(frame.getStackSize() - 2);
        OriginalSourceValue value2SourceValue = frame.getStack(frame.getStackSize() - 1);
        if (!value1SourceValue.originalSource.isOneWayProduced() || !value2SourceValue.originalSource.isOneWayProduced()) continue;

        AbstractInsnNode value1Insn = value1SourceValue.originalSource.getProducer();
        AbstractInsnNode value2Insn = value2SourceValue.originalSource.getProducer();

        if (value1Insn instanceof LdcInsnNode ldcValue1 && value2Insn instanceof LdcInsnNode ldcValue2) {
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
          methodNode.instructions.remove(value1SourceValue.getProducer());
          methodNode.instructions.remove(value2SourceValue.getProducer());
        }
      }
    }
  }

  // TODO: Add LOOKUPSWITCH and TABLESWITCH
  private void simplifyJumpInstructions(ClassWrapper classWrapper, MethodNode methodNode) {
    Map<AbstractInsnNode, Frame<OriginalSourceValue>> frames = analyzeOriginalSource(classWrapper.getClassNode(), methodNode);
    if (frames == null) return;

    // Simplify 'jump' instructions
    for (AbstractInsnNode insn : methodNode.instructions) {
      if (AsmMathHelper.isOneValueCondition(insn.getOpcode())) {
        // One-value if statement

        Frame<OriginalSourceValue> frame = frames.get(insn);
        if (frame == null) continue;

        JumpInsnNode jumpInsn = (JumpInsnNode) insn;

        // Get instruction from stack that is passed to if statement
        OriginalSourceValue sourceValue = frame.getStack(frame.getStackSize() - 1);
        if (!sourceValue.originalSource.isOneWayProduced()) continue;

        AbstractInsnNode valueInsn = sourceValue.originalSource.getProducer();

        // Process if statement
        if (valueInsn.isInteger()) {
          boolean ifResult = AsmMathHelper.condition(
              valueInsn.asInteger(), // Value
              jumpInsn.getOpcode() // Opcode
          );

          // Correctly transform redundant if statement
          processRedundantIfStatement(methodNode, jumpInsn, ifResult);

          // Cleanup value
          methodNode.instructions.remove(sourceValue.getProducer());
        }
      } else if (AsmMathHelper.isTwoValuesCondition(insn.getOpcode())) {
        // Two-value if statements

        Frame<OriginalSourceValue> frame = frames.get(insn);
        if (frame == null) continue;

        JumpInsnNode jumpInsn = (JumpInsnNode) insn;

        // Get instructions from stack that are passed to if statement
        OriginalSourceValue sourceValue1 = frame.getStack(frame.getStackSize() - 2);
        OriginalSourceValue sourceValue2 = frame.getStack(frame.getStackSize() - 1);
        if (!sourceValue1.originalSource.isOneWayProduced() || !sourceValue2.originalSource.isOneWayProduced()) continue;

        AbstractInsnNode valueInsn1 = sourceValue1.originalSource.getProducer();
        AbstractInsnNode valueInsn2 = sourceValue2.originalSource.getProducer();

        // Process if statement
        if (valueInsn1.isInteger() && valueInsn2.isInteger()) {
          boolean ifResult = AsmMathHelper.condition(
              valueInsn1.asInteger(), // First value
              valueInsn2.asInteger(), // Second value
              jumpInsn.getOpcode() // Opcode
          );

          // Correctly transform redundant if statement
          processRedundantIfStatement(methodNode, jumpInsn, ifResult);

          // Cleanup values
          methodNode.instructions.remove(sourceValue1.getProducer());
          methodNode.instructions.remove(sourceValue2.getProducer());
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
