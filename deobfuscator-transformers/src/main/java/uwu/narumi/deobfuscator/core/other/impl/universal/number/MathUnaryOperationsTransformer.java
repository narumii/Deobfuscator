package uwu.narumi.deobfuscator.core.other.impl.universal.number;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.analysis.OriginalSourceValue;
import uwu.narumi.deobfuscator.api.asm.InstructionContext;
import uwu.narumi.deobfuscator.api.context.Context;
import uwu.narumi.deobfuscator.api.helper.AsmHelper;
import uwu.narumi.deobfuscator.api.helper.AsmMathHelper;
import uwu.narumi.deobfuscator.api.transformer.FramedInstructionsTransformer;

import java.util.stream.Stream;

/**
 * Simplifies number casts on constant value.
 */
public class MathUnaryOperationsTransformer extends FramedInstructionsTransformer {
  @Override
  protected Stream<AbstractInsnNode> buildInstructionsStream(Stream<AbstractInsnNode> stream) {
    return stream
        .filter(insn -> AsmMathHelper.isMathUnaryOperation(insn.getOpcode()));
  }

  @Override
  protected boolean transformInstruction(Context context, InstructionContext insnContext) {
    // Get instructions from stack that are passed
    OriginalSourceValue sourceValue = insnContext.frame().getStack(insnContext.frame().getStackSize() - 1);
    OriginalSourceValue originalSource = sourceValue.originalSource;
    if (!originalSource.isOneWayProduced()) return false;

    AbstractInsnNode valueInsn = originalSource.getProducer();

    if (valueInsn.isNumber()) {
      Number castedNumber = AsmMathHelper.mathUnaryOperation(valueInsn.asNumber(), insnContext.insn().getOpcode());

      insnContext.methodNode().instructions.set(insnContext.insn(), AsmHelper.getNumber(castedNumber));
      insnContext.methodNode().instructions.remove(sourceValue.getProducer());

      return true;
    }

    return false;
  }
}
