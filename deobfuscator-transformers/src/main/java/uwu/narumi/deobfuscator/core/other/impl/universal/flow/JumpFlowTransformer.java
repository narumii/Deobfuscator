package uwu.narumi.deobfuscator.core.other.impl.universal.flow;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.OriginalSourceValue;
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.helper.AsmMathHelper;
import uwu.narumi.deobfuscator.api.transformer.FramedInstructionsTransformer;

public class JumpFlowTransformer extends FramedInstructionsTransformer {
  @Override
  protected boolean transformInstruction(ClassWrapper classWrapper, MethodNode methodNode, AbstractInsnNode insn, Frame<OriginalSourceValue> frame) {
    if (AsmMathHelper.isOneValueCondition(insn.getOpcode())) {
      // One-value if statement

      JumpInsnNode jumpInsn = (JumpInsnNode) insn;

      // Get instruction from stack that is passed to if statement
      OriginalSourceValue sourceValue = frame.getStack(frame.getStackSize() - 1);
      if (!sourceValue.originalSource.isOneWayProduced()) return false;

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

        return true;
      }
    } else if (AsmMathHelper.isTwoValuesCondition(insn.getOpcode())) {
      // Two-value if statements

      JumpInsnNode jumpInsn = (JumpInsnNode) insn;

      // Get instructions from stack that are passed to if statement
      OriginalSourceValue sourceValue1 = frame.getStack(frame.getStackSize() - 2);
      OriginalSourceValue sourceValue2 = frame.getStack(frame.getStackSize() - 1);
      if (!sourceValue1.originalSource.isOneWayProduced() || !sourceValue2.originalSource.isOneWayProduced()) return false;

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

        return true;
      }
    }

    return false;
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
