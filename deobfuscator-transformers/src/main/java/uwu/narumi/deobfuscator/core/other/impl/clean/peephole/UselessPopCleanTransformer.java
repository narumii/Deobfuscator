package uwu.narumi.deobfuscator.core.other.impl.clean.peephole;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.OriginalSourceValue;
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.asm.InsnContext;
import uwu.narumi.deobfuscator.api.context.Context;
import uwu.narumi.deobfuscator.api.helper.FramedInstructionsStream;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

import java.util.ArrayList;
import java.util.List;

public class UselessPopCleanTransformer extends Transformer {
  public UselessPopCleanTransformer() {
    this.rerunOnChange = true;
  }

  private final List<AbstractInsnNode> poppedDups = new ArrayList<>();

  @Override
  protected void transform(ClassWrapper scope, Context context) throws Exception {
    FramedInstructionsStream.of(scope, context)
        .editInstructionsStream(stream -> stream.filter(insn -> insn.getOpcode() == POP || insn.getOpcode() == POP2))
        .forEach(insnContext -> {
          boolean success = tryRemovePop(insnContext);

          if (success) {
            insnContext.methodNode().instructions.remove(insnContext.insn());
            markChange();
          }
        });
  }

  /**
   * Tries to remove pop's source values
   *
   * @param insnContext Instructon context
   * @return If removed
   */
  private boolean tryRemovePop(InsnContext insnContext) {
    AbstractInsnNode insn = insnContext.insn();
    OriginalSourceValue firstValue = insnContext.frame().getStack(insnContext.frame().getStackSize() - 1);

    if (!canPop(firstValue)) return false;

    if (insn.getOpcode() == POP) {
      // Pop the value from the stack
      popSourceValue(firstValue, insnContext.methodNode());
      return true;
    } else if (insn.getOpcode() == POP2) {
      if (firstValue.getSize() == 2) {
        // Pop 2-sized value from the stack
        popSourceValue(firstValue, insnContext.methodNode());
      } else {
        // Pop two values from the stack

        int index = insnContext.frame().getStackSize() - 2;
        OriginalSourceValue secondValue = index >= 0 ? insnContext.frame().getStack(insnContext.frame().getStackSize() - 2) : null;
        // Return if we can't remove the source value
        if (secondValue != null && !canPop(secondValue)) return false;

        // Pop
        popSourceValue(firstValue, insnContext.methodNode());
        if (secondValue != null) {
          popSourceValue(secondValue, insnContext.methodNode());
        }
      }
      return true;
    }

    return false;
  }

  /**
   * Checks if source value can be popped
   */
  private boolean canPop(OriginalSourceValue sourceValue) {
    if (sourceValue.insns.isEmpty()) {
      // Nothing to remove. Probably a local variable
      return false;
    }

    // Check if all producers of the source value are constants
    for (AbstractInsnNode producer : sourceValue.insns) {
      // Can be popped if the value is constant
      if (producer.isConstant()) continue;
      // Can be popped if the value is DUP, and it wasn't popped yet
      if (producer.getOpcode() == DUP && !poppedDups.contains(producer)) continue;

      return false;
    }
    return true;
  }

  private void popSourceValue(OriginalSourceValue value, MethodNode methodNode) {
    for (AbstractInsnNode producer : value.insns) {
      if (producer.getOpcode() == DUP) {
        // Prevent popping DUP twice
        poppedDups.add(producer);
      }
      methodNode.instructions.remove(producer);
    }
  }
}
