package uwu.narumi.deobfuscator.core.other.impl.clean.peephole;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.OriginalSourceValue;
import uwu.narumi.deobfuscator.api.asm.InsnContext;
import uwu.narumi.deobfuscator.api.asm.MethodContext;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

/**
 * This transformer expands DUP-related instructions (DUP, DUP_X1, DUP_X2, DUP2, DUP2_X1, DUP2_X2) and SWAP
 * by replacing them with clones of the instructions that produced the values on the stack.
 * This simplification is often effective after constant propagation or other optimizations have made
 * the producers constant values (e.g., LDC, BIPUSH).
 *
 * <p>Example 1:
 *
 * <p>Input:
 * <pre>
 * bipush 10
 * dup
 * invokestatic foo (I)V
 * invokestatic bar (I)V
 * </pre>
 *
 * <p>Output:
 * <pre>
 * bipush 10
 * bipush 10 // cloned value
 * invokestatic foo (I)V
 * invokestatic bar (I)V
 * </pre>
 *
 * Example 2 (DUP_X2 - Form 1: value1, value2, value3 -> value1, value3, value2, value1):
 *
 * <p>Input:
 * <pre>
 * bipush 10 // value3
 * bipush 20 // value2
 * bipush 30 // value1
 * dup_x2
 * invokestatic someMethod(III)V // Consumes value1, value2, value3
 * invokestatic anotherMethod(I)V // Consumes original value1
 * </pre>
 *
 * <p>Output:
 * <pre>
 * bipush 10 // value3
 * bipush 20 // value2
 * bipush 30 // value1
 * pop
 * pop
 * pop
 * bipush 30 // cloned value1
 * bipush 10 // cloned value3
 * bipush 20 // cloned value2
 * bipush 30 // cloned value1
 * invokestatic someMethod(III)V
 * invokestatic anotherMethod(I)V
 * </pre>
 *
 */
public class ExpandDupsTransformer extends Transformer {
  @Override
  protected void transform() throws Exception {
    scopedClasses().parallelStream().forEach(classWrapper -> classWrapper.methods().parallelStream().forEach(methodNode -> {
      MethodContext methodContext = MethodContext.of(classWrapper, methodNode);

      for (AbstractInsnNode insn : methodNode.instructions.toArray()) {
        // Try to expand DUP-related instructions
        boolean expanded = expandDup(methodContext, insn);

        if (expanded) {
          methodNode.instructions.remove(insn);
          markChange();
        }
      }
    }));
  }

  /**
   * Expands DUP-related instructions in a method's bytecode.
   *
   * @return Is expanded
   */
  // This is a mess XD
  private boolean expandDup(MethodContext methodContext, AbstractInsnNode insn) {
    MethodNode methodNode = methodContext.methodNode();
    InsnContext insnContext = methodContext.at(insn);
    Frame<OriginalSourceValue> frame = insnContext.frame();
    if (frame == null) return false;

    if (insn.getOpcode() == DUP) {
      OriginalSourceValue value1 = frame.getStack(frame.getStackSize() - 1);
      AbstractInsnNode producer1 = checkAndGetProducer(value1);
      if (producer1 == null) return false;

      // Expand
      methodNode.instructions.insert(insn, producer1.clone(null));

      return true;
    } else if (insn.getOpcode() == DUP_X1) {
      OriginalSourceValue value1 = frame.getStack(frame.getStackSize() - 1);
      AbstractInsnNode producer1 = checkAndGetProducer(value1);
      if (producer1 == null) return false;

      OriginalSourceValue value2 = frame.getStack(frame.getStackSize() - 2);
      AbstractInsnNode producer2 = checkAndGetProducer(value2);
      if (producer2 == null) return false;

      insnContext.placePops();

      // Expand
      methodNode.instructions.insert(insn, producer1.clone(null));
      methodNode.instructions.insert(insn, producer2.clone(null));
      methodNode.instructions.insert(insn, producer1.clone(null));

      return true;
    } else if (insn.getOpcode() == DUP_X2) {
      OriginalSourceValue value1 = frame.getStack(frame.getStackSize() - 1);
      AbstractInsnNode producer1 = checkAndGetProducer(value1);
      if (producer1 == null) return false;

      OriginalSourceValue value2 = frame.getStack(frame.getStackSize() - 2);
      AbstractInsnNode producer2 = checkAndGetProducer(value2);
      if (producer2 == null) return false;

      // Forms
      if (value1.getSize() == 1 && value2.getSize() == 2) {
        insnContext.placePops();

        // Expand
        methodNode.instructions.insert(insn, producer1.clone(null));
        methodNode.instructions.insert(insn, producer2.clone(null));
        methodNode.instructions.insert(insn, producer1.clone(null));

        return true;
      } else {
        OriginalSourceValue value3 = frame.getStack(frame.getStackSize() - 3);
        AbstractInsnNode producer3 = checkAndGetProducer(value3);
        if (producer3 == null) return false;

        if (value1.getSize() == 1 && value2.getSize() == 1 && value3.getSize() == 1) {
          insnContext.placePops();

          // Expand
          methodNode.instructions.insert(insn, producer1.clone(null));
          methodNode.instructions.insert(insn, producer3.clone(null));
          methodNode.instructions.insert(insn, producer2.clone(null));
          methodNode.instructions.insert(insn, producer1.clone(null));

          return true;
        }
      }
    } else if (insn.getOpcode() == DUP2) {
      OriginalSourceValue value1 = frame.getStack(frame.getStackSize() - 1);
      AbstractInsnNode producer1 = checkAndGetProducer(value1);
      if (producer1 == null) return false;

      // Forms
      if (value1.getSize() == 2) {
        // Expand
        methodNode.instructions.insert(insn, producer1.clone(null));

        return true;
      } else if (value1.getSize() == 1) {
        OriginalSourceValue value2 = frame.getStack(frame.getStackSize() - 2);
        AbstractInsnNode producer2 = checkAndGetProducer(value2);
        if (producer2 == null) return false;

        // Expand
        methodNode.instructions.insert(insn, producer2.clone(null));
        methodNode.instructions.insert(insn, producer1.clone(null));

        return true;
      }
    } else if (insn.getOpcode() == DUP2_X1) {
      OriginalSourceValue value1 = frame.getStack(frame.getStackSize() - 1);
      AbstractInsnNode producer1 = checkAndGetProducer(value1);
      if (producer1 == null) return false;

      OriginalSourceValue value2 = frame.getStack(frame.getStackSize() - 2);
      AbstractInsnNode producer2 = checkAndGetProducer(value2);
      if (producer2 == null) return false;

      // Forms
      if (value1.getSize() == 2 && value2.getSize() == 1) {
        insnContext.placePops();

        // Expand
        methodNode.instructions.insert(insn, producer1.clone(null));
        methodNode.instructions.insert(insn, producer2.clone(null));
        methodNode.instructions.insert(insn, producer1.clone(null));

        return true;
      } else {
        OriginalSourceValue value3 = frame.getStack(frame.getStackSize() - 3);
        AbstractInsnNode producer3 = checkAndGetProducer(value3);
        if (producer3 == null) return false;

        if (value1.getSize() == 1 && value2.getSize() == 1 && value3.getSize() == 1) {
          insnContext.placePops();

          // Expand
          methodNode.instructions.insert(insn, producer2.clone(null));
          methodNode.instructions.insert(insn, producer1.clone(null));
          methodNode.instructions.insert(insn, producer3.clone(null));
          methodNode.instructions.insert(insn, producer2.clone(null));
          methodNode.instructions.insert(insn, producer1.clone(null));

          return true;
        }
      }
    } else if (insn.getOpcode() == DUP2_X2) {
      OriginalSourceValue value1 = frame.getStack(frame.getStackSize() - 1);
      AbstractInsnNode producer1 = checkAndGetProducer(value1);
      if (producer1 == null) return false;

      OriginalSourceValue value2 = frame.getStack(frame.getStackSize() - 2);
      AbstractInsnNode producer2 = checkAndGetProducer(value2);
      if (producer2 == null) return false;

      // Forms
      if (value1.getSize() == 2 && value2.getSize() == 2) {
        insnContext.placePops();

        // Expand
        methodNode.instructions.insert(insn, producer1.clone(null));
        methodNode.instructions.insert(insn, producer2.clone(null));
        methodNode.instructions.insert(insn, producer1.clone(null));

        return true;
      } else {
        OriginalSourceValue value3 = frame.getStack(frame.getStackSize() - 3);
        AbstractInsnNode producer3 = checkAndGetProducer(value3);
        if (producer3 == null) return false;

        if (value1.getSize() == 1 && value2.getSize() == 1 && value3.getSize() == 2) {
          insnContext.placePops();

          // Expand
          methodNode.instructions.insert(insn, producer2.clone(null));
          methodNode.instructions.insert(insn, producer1.clone(null));
          methodNode.instructions.insert(insn, producer3.clone(null));
          methodNode.instructions.insert(insn, producer2.clone(null));
          methodNode.instructions.insert(insn, producer1.clone(null));

          return true;
        } else if (value1.getSize() == 2 && value2.getSize() == 1 && value3.getSize() == 1) {
          insnContext.placePops();

          // Expand
          methodNode.instructions.insert(insn, producer1.clone(null));
          methodNode.instructions.insert(insn, producer3.clone(null));
          methodNode.instructions.insert(insn, producer2.clone(null));
          methodNode.instructions.insert(insn, producer1.clone(null));

          return true;
        } else {
          OriginalSourceValue value4 = frame.getStack(frame.getStackSize() - 4);
          AbstractInsnNode producer4 = checkAndGetProducer(value4);
          if (producer4 == null) return false;

          if (value1.getSize() == 1 && value2.getSize() == 2 && value3.getSize() == 1 && value4.getSize() == 1) {
            insnContext.placePops();

            // Expand
            methodNode.instructions.insert(insn, producer2.clone(null));
            methodNode.instructions.insert(insn, producer1.clone(null));
            methodNode.instructions.insert(insn, producer4.clone(null));
            methodNode.instructions.insert(insn, producer3.clone(null));
            methodNode.instructions.insert(insn, producer2.clone(null));
            methodNode.instructions.insert(insn, producer1.clone(null));

            return true;
          }
        }
      }
    } else if (insn.getOpcode() == SWAP) {
      OriginalSourceValue value1 = frame.getStack(frame.getStackSize() - 1);
      AbstractInsnNode producer1 = checkAndGetProducer(value1);
      if (producer1 == null) return false;

      OriginalSourceValue value2 = frame.getStack(frame.getStackSize() - 2);
      AbstractInsnNode producer2 = checkAndGetProducer(value2);
      if (producer2 == null) return false;

      insnContext.placePops();

      // Swap
      methodNode.instructions.insert(insn, producer2.clone(null));
      methodNode.instructions.insert(insn, producer1.clone(null));

      return true;
    }

    return false;
  }

  @Nullable
  private AbstractInsnNode checkAndGetProducer(OriginalSourceValue sourceValue) {
    if (sourceValue.originalSource.isOneWayProduced() && sourceValue.originalSource.getProducer().isConstant()) {
      return sourceValue.originalSource.getProducer();
    }
    return null;
  }
}
