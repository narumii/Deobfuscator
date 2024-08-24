package uwu.narumi.deobfuscator.core.other.impl.clean.peephole;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.OriginalSourceValue;
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.transformer.FramedInstructionsTransformer;

import java.util.stream.Stream;

public class UselessPopCleanTransformer extends FramedInstructionsTransformer {

  @Override
  protected Stream<AbstractInsnNode> getInstructionsStream(Stream<AbstractInsnNode> stream) {
    return stream
        .filter(insn -> insn.getOpcode() == POP);
  }

  @Override
  protected boolean transformInstruction(ClassWrapper classWrapper, MethodNode methodNode, AbstractInsnNode insn, Frame<OriginalSourceValue> frame) {
    OriginalSourceValue sourceValue = frame.getStack(frame.getStackSize() - 1);
    for (AbstractInsnNode producer : sourceValue.insns) {
      // If the producer is a constant, remove the pop and the constant
      if (producer.isConstant()) {
        methodNode.instructions.remove(producer);
        methodNode.instructions.remove(insn);
        return true;
      }
    }

    return false;
  }
}
