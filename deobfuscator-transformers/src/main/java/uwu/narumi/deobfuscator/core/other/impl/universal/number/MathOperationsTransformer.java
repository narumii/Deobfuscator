package uwu.narumi.deobfuscator.core.other.impl.universal.number;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.OriginalSourceValue;
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.helper.AsmHelper;
import uwu.narumi.deobfuscator.api.helper.AsmMathHelper;
import uwu.narumi.deobfuscator.api.transformer.FramedInstructionsTransformer;

import java.util.stream.Stream;

/**
 * Simplifies math operations on two constant values.
 */
public class MathOperationsTransformer extends FramedInstructionsTransformer {

  @Override
  protected Stream<AbstractInsnNode> getInstructionsStream(Stream<AbstractInsnNode> stream) {
    return stream
        .filter(insn -> AsmMathHelper.isMathOperation(insn.getOpcode()));
  }

  @Override
  protected boolean transformInstruction(ClassWrapper classWrapper, MethodNode methodNode, AbstractInsnNode insn, Frame<OriginalSourceValue> frame) {
    // Get instructions from stack that are passed
    OriginalSourceValue value1SourceValue = frame.getStack(frame.getStackSize() - 2);
    OriginalSourceValue value2SourceValue = frame.getStack(frame.getStackSize() - 1);
    if (!value1SourceValue.originalSource.isOneWayProduced() || !value2SourceValue.originalSource.isOneWayProduced()) return false;

    AbstractInsnNode value1Insn = value1SourceValue.originalSource.getProducer();
    AbstractInsnNode value2Insn = value2SourceValue.originalSource.getProducer();

    if (value1Insn.isNumber() && value2Insn.isNumber()) {
      Number value1 = value1Insn.asNumber();
      Number value2 = value2Insn.asNumber();

      methodNode.instructions.set(insn, AsmHelper.getNumber(AsmMathHelper.mathOperation(value1, value2, insn.getOpcode())));
      methodNode.instructions.remove(value1SourceValue.getProducer());
      methodNode.instructions.remove(value2SourceValue.getProducer());

      return true;
    }

    return false;
  }
}
