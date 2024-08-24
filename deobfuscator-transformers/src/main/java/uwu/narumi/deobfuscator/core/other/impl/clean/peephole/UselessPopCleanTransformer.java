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
        .filter(insn -> insn.getOpcode() == POP || insn.getOpcode() == POP2);
  }

  @Override
  protected boolean transformInstruction(ClassWrapper classWrapper, MethodNode methodNode, AbstractInsnNode insn, Frame<OriginalSourceValue> frame) {
    boolean changed = false;

    OriginalSourceValue firstValue = frame.getStack(frame.getStackSize() - 1);
    for (AbstractInsnNode producer : firstValue.insns) {
      if (insn.getOpcode() == POP2) {
        // If the producer is a double or long, remove the pop2 and the double/long
        if (producer.isDouble() || producer.isLong()) {
          methodNode.instructions.remove(producer);
          methodNode.instructions.remove(insn);
          changed = true;
        } else {
          // Pop two values
          OriginalSourceValue secondValue = frame.getStack(frame.getStackSize() - 2);
          popSourceValue(insn, secondValue, methodNode);
          popSourceValue(insn, firstValue, methodNode);
        }
      } else if (insn.getOpcode() == POP) {
        changed |= popSourceValue(insn, firstValue, methodNode);
      }
    }

    return changed;
  }

  private boolean popSourceValue(AbstractInsnNode insn, OriginalSourceValue sourceValue, MethodNode methodNode) {
    boolean changed = false;
    for (AbstractInsnNode producer : sourceValue.insns) {
      // If the producer is a constant, remove the pop and the constant
      if (producer.isConstant()) {
        methodNode.instructions.remove(producer);
        methodNode.instructions.remove(insn);
        changed = true;
      }
    }

    return changed;
  }
}
