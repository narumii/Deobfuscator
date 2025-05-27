package uwu.narumi.deobfuscator.api.helper;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.analysis.OriginalSourceValue;
import uwu.narumi.deobfuscator.api.asm.matcher.Match;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.MethodMatch;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple interpreter for constant folding
 */
public class SimpleInterpreter {

  public static final List<MethodInterpreter> METHOD_INTERPRETERS = new ArrayList<>();

  public static void registerInterpreter(MethodInterpreter interpreter) {
    METHOD_INTERPRETERS.add(interpreter);
  }

  static {
    // String#length
    registerInterpreter(MethodInterpreter.of(
        MethodMatch.invokeVirtual()
            .owner("java/lang/String")
            .name("length")
            .desc("()I"),
        (insn, stackValues) -> {
          // Get value from stack
          OriginalSourceValue sourceValue = stackValues.get(stackValues.size() - 1);
          if (sourceValue.getConstantValue() != null && sourceValue.getConstantValue().value() instanceof String text) {
            return OriginalSourceValue.ConstantValue.of(text.length());
          }

          return null;
        }
    ));
    // String#hashCode
    registerInterpreter(MethodInterpreter.of(
        MethodMatch.invokeVirtual()
            .owner("java/lang/String")
            .name("hashCode")
            .desc("()I"),
        (insn, stackValues) -> {
          // Get value from stack
          OriginalSourceValue sourceValue = stackValues.get(stackValues.size() - 1);
          if (sourceValue.getConstantValue() != null && sourceValue.getConstantValue().value() instanceof String text) {
            return OriginalSourceValue.ConstantValue.of(text.hashCode());
          }
          return null;
        }
    ));
    // Integer#parseInt and Integer#valueOf
    registerInterpreter(MethodInterpreter.of(
        MethodMatch.invokeStatic()
            .owner("java/lang/Integer")
            .name("parseInt", "valueOf")
            .desc("(Ljava/lang/String;)I"),
        (insn, stackValues) -> {
          // Get value from stack
          OriginalSourceValue sourceValue = stackValues.get(stackValues.size() - 1);
          if (sourceValue.getConstantValue() != null && sourceValue.getConstantValue().value() instanceof String text) {
            try {
              return OriginalSourceValue.ConstantValue.of(Integer.parseInt(text));
            } catch (NumberFormatException e) {
              return null;
            }
          }
          return null;
        }
    ));
    // Integer#parseInt with radix
    registerInterpreter(MethodInterpreter.of(
        MethodMatch.invokeStatic()
            .owner("java/lang/Integer")
            .name("parseInt", "valueOf")
            .desc("(Ljava/lang/String;I)I"),
        (insn, stackValues) -> {
          // Get values from stack
          OriginalSourceValue firstValue = stackValues.get(stackValues.size() - 2);
          OriginalSourceValue secondValue = stackValues.get(stackValues.size() - 1);
          if (firstValue.getConstantValue() != null && firstValue.getConstantValue().value() instanceof String text &&
              secondValue.getConstantValue() != null && secondValue.getConstantValue().value() instanceof Integer radix) {
            try {
              return OriginalSourceValue.ConstantValue.of(Integer.parseInt(text, radix));
            } catch (NumberFormatException e) {
              return null;
            }
          }
          return null;
        }
    ));
    // Integer#reverse
    registerInterpreter(MethodInterpreter.of(
        MethodMatch.invokeStatic()
            .owner("java/lang/Integer")
            .name("reverse")
            .desc("(I)I"),
        (insn, stackValues) -> {
          // Get value from stack
          OriginalSourceValue sourceValue = stackValues.get(stackValues.size() - 1);
          if (sourceValue.getConstantValue() != null && sourceValue.getConstantValue().value() instanceof Integer value) {
            return OriginalSourceValue.ConstantValue.of(Integer.reverse(value));
          }
          return null;
        }
    ));
    // Long#reverse
    registerInterpreter(MethodInterpreter.of(
        MethodMatch.invokeStatic()
            .owner("java/lang/Long")
            .name("reverse")
            .desc("(J)J"),
        (insn, stackValues) -> {
          // Get value from stack
          OriginalSourceValue sourceValue = stackValues.get(stackValues.size() - 1);
          if (sourceValue.getConstantValue() != null && sourceValue.getConstantValue().value() instanceof Long value) {
            return OriginalSourceValue.ConstantValue.of(Long.reverse(value));
          }
          return null;
        }
    ));
    // Float#floatToIntBits
    registerInterpreter(MethodInterpreter.of(
        MethodMatch.invokeStatic()
            .owner("java/lang/Float")
            .name("floatToIntBits")
            .desc("(F)I"),
        (insn, stackValues) -> {
          // Get value from stack
          OriginalSourceValue sourceValue = stackValues.get(stackValues.size() - 1);
          if (sourceValue.getConstantValue() != null && sourceValue.getConstantValue().value() instanceof Float value) {
            return OriginalSourceValue.ConstantValue.of(Float.floatToIntBits(value));
          }
          return null;
        }
    ));
    // Float#intBitsToFloat
    registerInterpreter(MethodInterpreter.of(
        MethodMatch.invokeStatic()
            .owner("java/lang/Float")
            .name("intBitsToFloat")
            .desc("(I)F"),
        (insn, stackValues) -> {
          // Get value from stack
          OriginalSourceValue sourceValue = stackValues.get(stackValues.size() - 1);
          if (sourceValue.getConstantValue() != null && sourceValue.getConstantValue().value() instanceof Integer value) {
            return OriginalSourceValue.ConstantValue.of(Float.intBitsToFloat(value));
          }
          return null;
        }
    ));
    // Double#doubleToLongBits
    registerInterpreter(MethodInterpreter.of(
        MethodMatch.invokeStatic()
            .owner("java/lang/Double")
            .name("doubleToLongBits")
            .desc("(D)J"),
        (insn, stackValues) -> {
          // Get value from stack
          OriginalSourceValue sourceValue = stackValues.get(stackValues.size() - 1);
          if (sourceValue.getConstantValue() != null && sourceValue.getConstantValue().value() instanceof Double value) {
            return OriginalSourceValue.ConstantValue.of(Double.doubleToLongBits(value));
          }
          return null;
        }
    ));
    // Double#longBitsToDouble
    registerInterpreter(MethodInterpreter.of(
        MethodMatch.invokeStatic()
            .owner("java/lang/Double")
            .name("longBitsToDouble")
            .desc("(J)D"),
        (insn, stackValues) -> {
          // Get value from stack
          OriginalSourceValue sourceValue = stackValues.get(stackValues.size() - 1);
          if (sourceValue.getConstantValue() != null && sourceValue.getConstantValue().value() instanceof Long value) {
            return OriginalSourceValue.ConstantValue.of(Double.longBitsToDouble(value));
          }
          return null;
        }
    ));
  }

  public record MethodInterpreter(Match match, MethodComputation methodComputation) {
    public static MethodInterpreter of(Match match, MethodComputation transformation) {
      return new MethodInterpreter(match, transformation);
    }
  }

  @FunctionalInterface
  public interface MethodComputation {
    /**
     * Compute constant value from instruction and stack values
     *
     * @param insn        Current instruction
     * @param stackValues Stack values
     * @return Constant value if it was computed or null
     */
    @Nullable
    OriginalSourceValue.ConstantValue computeConstant(AbstractInsnNode insn, List<? extends OriginalSourceValue> stackValues);
  }
}
