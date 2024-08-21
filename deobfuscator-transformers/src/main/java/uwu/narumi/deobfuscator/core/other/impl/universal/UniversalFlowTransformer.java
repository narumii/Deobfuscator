package uwu.narumi.deobfuscator.core.other.impl.universal;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.OriginalSourceValue;
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.context.Context;
import uwu.narumi.deobfuscator.api.helper.AsmMathHelper;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

import java.util.Map;

public class UniversalFlowTransformer extends Transformer {

  private boolean changed = false;

  @Override
  protected boolean transform(ClassWrapper scope, Context context) throws Exception {
    context.classes(scope).forEach(classWrapper -> classWrapper.methods().forEach(methodNode -> {
      simplifyJumpInstructions(classWrapper, methodNode);
    }));

    return changed;
  }

  // TODO: Add LOOKUPSWITCH and TABLESWITCH
  private void simplifyJumpInstructions(ClassWrapper classWrapper, MethodNode methodNode) {
    Map<AbstractInsnNode, Frame<OriginalSourceValue>> frames = analyzeOriginalSource(classWrapper.getClassNode(), methodNode);
    if (frames == null) return;

    // Simplify 'jump' instructions
    for (AbstractInsnNode insn : methodNode.instructions.toArray()) {
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

          changed = true;
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

          changed = true;
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
