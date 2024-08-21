package uwu.narumi.deobfuscator.api.helper;

import static org.objectweb.asm.Opcodes.*;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.analysis.OriginalSourceValue;
import uwu.narumi.deobfuscator.api.asm.matcher.rule.Match;
import uwu.narumi.deobfuscator.api.asm.matcher.rule.impl.MethodMatch;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

public final class AsmMathHelper {
  private static final Map<Integer, Predicate<Integer>> ONE_VALUE_CONDITION_PREDICATES = Map.of(
      IFEQ, value -> value == 0,
      IFNE, value -> value != 0,
      IFLT, value -> value < 0,
      IFGE, value -> value >= 0,
      IFGT, value -> value > 0,
      IFLE, value -> value <= 0
  );

  private static final Map<Integer, BiPredicate<Integer, Integer>> TWO_VALUES_VALUE_CONDITION_PREDICATES = Map.of(
      IF_ICMPEQ, Objects::equals,
      IF_ICMPNE, (first, second) -> !Objects.equals(first, second),
      IF_ICMPLT, (first, second) -> first < second,
      IF_ICMPGE, (first, second) -> first >= second,
      IF_ICMPGT, (first, second) -> first > second,
      IF_ICMPLE, (first, second) -> first <= second
  );

  private static final Map<Integer, BiFunction<Number, Number, Number>> MATH_OPERATIONS = Map.ofEntries(
      // Integer
      Map.entry(IADD, (first, second) -> first.intValue() + second.intValue()),
      Map.entry(ISUB, (first, second) -> first.intValue() - second.intValue()),
      Map.entry(IMUL, (first, second) -> first.intValue() * second.intValue()),
      Map.entry(IDIV, (first, second) -> first.intValue() / second.intValue()),
      Map.entry(IREM, (first, second) -> first.intValue() % second.intValue()),
      Map.entry(IXOR, (first, second) -> first.intValue() ^ second.intValue()),
      Map.entry(IAND, (first, second) -> first.intValue() & second.intValue()),
      Map.entry(IOR, (first, second) -> first.intValue() | second.intValue()),
      Map.entry(ISHL, (first, second) -> first.intValue() << second.intValue()),
      Map.entry(ISHR, (first, second) -> first.intValue() >> second.intValue()),
      Map.entry(IUSHR, (first, second) -> first.intValue() >>> second.intValue()),
      // Long
      Map.entry(LADD, (first, second) -> first.longValue() + second.longValue()),
      Map.entry(LSUB, (first, second) -> first.longValue() - second.longValue()),
      Map.entry(LMUL, (first, second) -> first.longValue() * second.longValue()),
      Map.entry(LDIV, (first, second) -> first.longValue() / second.longValue()),
      Map.entry(LREM, (first, second) -> first.longValue() % second.longValue()),
      Map.entry(LXOR, (first, second) -> first.longValue() ^ second.longValue()),
      Map.entry(LAND, (first, second) -> first.longValue() & second.longValue()),
      Map.entry(LOR, (first, second) -> first.longValue() | second.longValue()),
      Map.entry(LSHL, (first, second) -> first.longValue() << second.longValue()),
      Map.entry(LSHR, (first, second) -> first.longValue() >> second.longValue()),
      Map.entry(LUSHR, (first, second) -> first.longValue() >>> second.longValue()),
      // Float
      Map.entry(FADD, (first, second) -> first.floatValue() + second.floatValue()),
      Map.entry(FSUB, (first, second) -> first.floatValue() - second.floatValue()),
      Map.entry(FMUL, (first, second) -> first.floatValue() * second.floatValue()),
      Map.entry(FDIV, (first, second) -> first.floatValue() / second.floatValue()),
      Map.entry(FREM, (first, second) -> first.floatValue() % second.floatValue()),
      // Double
      Map.entry(DADD, (first, second) -> first.doubleValue() + second.doubleValue()),
      Map.entry(DSUB, (first, second) -> first.doubleValue() - second.doubleValue()),
      Map.entry(DMUL, (first, second) -> first.doubleValue() * second.doubleValue()),
      Map.entry(DDIV, (first, second) -> first.doubleValue() / second.doubleValue()),
      Map.entry(DREM, (first, second) -> first.doubleValue() % second.doubleValue())
  );

  private static final Map<Integer, BiFunction<Number, Number, Integer>> MATH_COMPARES = Map.ofEntries(
      Map.entry(LCMP, (first, second) -> Long.compare(first.longValue(), second.longValue())),
      Map.entry(FCMPL, (first, second) -> Float.isNaN(first.floatValue()) || Float.isNaN(second.floatValue()) ? -1 : Float.compare(first.floatValue(), second.floatValue())),
      Map.entry(FCMPG, (first, second) -> Float.isNaN(first.floatValue()) || Float.isNaN(second.floatValue()) ? 1 : Float.compare(first.floatValue(), second.floatValue())),
      Map.entry(DCMPL, (first, second) -> Double.isNaN(first.doubleValue()) || Double.isNaN(second.doubleValue()) ? -1 : Double.compare(first.doubleValue(), second.doubleValue())),
      Map.entry(DCMPG, (first, second) -> Double.isNaN(first.doubleValue()) || Double.isNaN(second.doubleValue()) ? 1 : Double.compare(first.doubleValue(), second.doubleValue()))
  );

  private static final Map<Integer, Function<Number, Number>> NUMBER_CASTS = Map.ofEntries(
      // Integer
      Map.entry(INEG, number -> -number.intValue()),
      Map.entry(I2B, number -> number.byteValue()),
      Map.entry(I2C, number -> number.shortValue()),
      Map.entry(I2D, number -> number.doubleValue()),
      Map.entry(I2F, number -> number.floatValue()),
      Map.entry(I2L, number -> number.longValue()),
      Map.entry(I2S, number -> number.shortValue()),
      // Long
      Map.entry(LNEG, number -> -number.longValue()),
      Map.entry(L2D, number -> number.doubleValue()),
      Map.entry(L2F, number -> number.floatValue()),
      Map.entry(L2I, number -> number.intValue()),
      // Float
      Map.entry(FNEG, number -> -number.floatValue()),
      Map.entry(F2D, number -> number.doubleValue()),
      Map.entry(F2I, number -> number.intValue()),
      Map.entry(F2L, number -> number.longValue()),
      // Double
      Map.entry(DNEG, number -> -number.doubleValue()),
      Map.entry(D2F, number -> number.floatValue()),
      Map.entry(D2I, number -> number.intValue()),
      Map.entry(D2L, number -> number.longValue())
  );

  public static final Match STRING_LENGTH =
      MethodMatch.invokeVirtual()
          .owner("java/lang/String")
          .name("length")
          .desc("()I")
          .defineTransformation(
              (methodNode, node, frame) -> {
                // Get value from stack
                OriginalSourceValue sourceValue = frame.getStack(frame.getStackSize() - 1);
                OriginalSourceValue originalSourceValue = sourceValue.originalSource;
                if (!originalSourceValue.isOneWayProduced()) return false;

                AbstractInsnNode originalInsn = originalSourceValue.getProducer();
                if (!originalInsn.isString()) return false;

                methodNode.instructions.set(
                    node,
                    AsmHelper.getNumber(originalInsn.asString().length())
                );
                methodNode.instructions.remove(sourceValue.getProducer());
                return true;
              });

  public static final Match STRING_HASHCODE =
      MethodMatch.invokeVirtual()
          .owner("java/lang/String")
          .name("hashCode")
          .desc("()I")
          .defineTransformation(
              (methodNode, node, frame) -> {
                // Get value from stack
                OriginalSourceValue sourceValue = frame.getStack(frame.getStackSize() - 1);
                OriginalSourceValue originalSourceValue = sourceValue.originalSource;
                if (!originalSourceValue.isOneWayProduced()) return false;

                AbstractInsnNode originalInsn = originalSourceValue.getProducer();
                if (!originalInsn.isString()) return false;

                methodNode.instructions.set(
                    node,
                    AsmHelper.getNumber(originalInsn.asString().hashCode())
                );
                methodNode.instructions.remove(sourceValue.getProducer());
                return true;
              });

  public static final Match STRING_TO_INTEGER =
      MethodMatch.invokeStatic()
          .owner("java/lang/Integer")
          .name("parseInt", "valueOf")
          .desc("(Ljava/lang/String;)I")
          .defineTransformation(
              (methodNode, node, frame) -> {
                // Get value from stack
                OriginalSourceValue sourceValue = frame.getStack(frame.getStackSize() - 1);
                OriginalSourceValue originalSourceValue = sourceValue.originalSource;
                if (!originalSourceValue.isOneWayProduced()) return false;

                AbstractInsnNode originalInsn = originalSourceValue.getProducer();
                // Integer#parseInt(String)
                if (!originalInsn.isString()) return false;

                methodNode.instructions.set(
                    node,
                    AsmHelper.getNumber(Integer.parseInt(originalInsn.asString()))
                );
                methodNode.instructions.remove(sourceValue.getProducer());
                return true;
              });

  public static final Match STRING_TO_INTEGER_RADIX =
      MethodMatch.invokeStatic()
          .owner("java/lang/Integer")
          .name("parseInt", "valueOf")
          .desc("(Ljava/lang/String;I)I")
          .defineTransformation(
              (methodNode, node, frame) -> {
                // Get values from stack
                OriginalSourceValue firstValue = frame.getStack(frame.getStackSize() - 2);
                OriginalSourceValue originalFirstValue = firstValue.originalSource;
                OriginalSourceValue secondValue = frame.getStack(frame.getStackSize() - 1);
                OriginalSourceValue originalSecondValue = secondValue.originalSource;
                if (!originalFirstValue.isOneWayProduced() || !originalSecondValue.isOneWayProduced()) return false;

                AbstractInsnNode originalFirstInsn = originalFirstValue.getProducer();
                AbstractInsnNode originalSecondInsn = originalSecondValue.getProducer();

                // Integer#parseInt(String, int)
                if (!originalFirstInsn.isString() || !originalSecondInsn.isInteger()) return false;

                methodNode.instructions.set(
                    node,
                    AsmHelper.getNumber(
                        Integer.parseInt(originalFirstInsn.asString(), originalSecondInsn.asInteger())
                    )
                );
                methodNode.instructions.remove(firstValue.getProducer());
                methodNode.instructions.remove(secondValue.getProducer());
                return true;
              });

  public static final Match INTEGER_REVERSE =
      MethodMatch.invokeStatic()
          .owner("java/lang/Integer")
          .name("reverse")
          .desc("(I)I")
          .defineTransformation(
              (methodNode, node, frame) -> {
                // Get value from stack
                OriginalSourceValue sourceValue = frame.getStack(frame.getStackSize() - 1);
                OriginalSourceValue originalSourceValue = sourceValue.originalSource;
                if (!originalSourceValue.isOneWayProduced()) return false;

                AbstractInsnNode originalInsn = originalSourceValue.getProducer();
                // Integer#reverse(int)
                if (!originalInsn.isInteger()) return false;

                methodNode.instructions.set(
                    node,
                    AsmHelper.getNumber(Integer.reverse(originalInsn.asInteger()))
                );
                methodNode.instructions.remove(sourceValue.getProducer());
                return true;
              });

  public static final Match LONG_REVERSE =
      MethodMatch.invokeStatic()
          .owner("java/lang/Long")
          .name("reverse")
          .desc("(J)J")
          .defineTransformation(
              (methodNode, node, frame) -> {
                // Get value from stack
                OriginalSourceValue sourceValue = frame.getStack(frame.getStackSize() - 1);
                OriginalSourceValue originalSourceValue = sourceValue.originalSource;
                if (!originalSourceValue.isOneWayProduced()) return false;

                AbstractInsnNode originalInsn = originalSourceValue.getProducer();
                // Long#reverse(long)
                if (!originalInsn.isLong()) return false;

                methodNode.instructions.set(
                    node,
                    AsmHelper.getNumber(Long.reverse(originalInsn.asLong()))
                );
                methodNode.instructions.remove(sourceValue.getProducer());
                return true;
              });

  public static final Match FLOAT_TO_BITS =
      MethodMatch.invokeStatic()
          .owner("java/lang/Float")
          .name("floatToIntBits")
          .desc("(F)I")
          .defineTransformation(
              (methodNode, node, frame) -> {
                // Get value from stack
                OriginalSourceValue sourceValue = frame.getStack(frame.getStackSize() - 1);
                OriginalSourceValue originalSourceValue = sourceValue.originalSource;
                if (!originalSourceValue.isOneWayProduced()) return false;

                AbstractInsnNode originalInsn = originalSourceValue.getProducer();
                // Float#floatToIntBits(float)
                if (!originalInsn.isFloat()) return false;

                methodNode.instructions.set(
                    node,
                    AsmHelper.getNumber(Float.floatToIntBits(originalInsn.asFloat()))
                );
                methodNode.instructions.remove(sourceValue.getProducer());
                return true;
              });

  public static final Match BITS_TO_FLOAT =
      MethodMatch.invokeStatic()
          .owner("java/lang/Float")
          .name("intBitsToFloat")
          .desc("(I)F")
          .defineTransformation(
              (methodNode, node, frame) -> {
                // Get value from stack
                OriginalSourceValue sourceValue = frame.getStack(frame.getStackSize() - 1);
                OriginalSourceValue originalSourceValue = sourceValue.originalSource;
                if (!originalSourceValue.isOneWayProduced()) return false;

                AbstractInsnNode originalInsn = originalSourceValue.getProducer();
                // Float#intBitsToFloat(int)
                if (!originalInsn.isInteger()) return false;

                methodNode.instructions.set(
                    node,
                    AsmHelper.getNumber(Float.intBitsToFloat(originalInsn.asInteger()))
                );
                methodNode.instructions.remove(sourceValue.getProducer());
                return true;
              });

  public static final Match DOUBLE_TO_BITS =
      MethodMatch.invokeStatic()
          .owner("java/lang/Double")
          .name("doubleToLongBits")
          .desc("(D)J")
          .defineTransformation(
              (methodNode, node, frame) -> {
                // Get value from stack
                OriginalSourceValue sourceValue = frame.getStack(frame.getStackSize() - 1);
                OriginalSourceValue originalSourceValue = sourceValue.originalSource;
                if (!originalSourceValue.isOneWayProduced()) return false;

                AbstractInsnNode originalInsn = originalSourceValue.getProducer();
                // Double#doubleToLongBits(double)
                if (!originalInsn.isDouble()) return false;

                methodNode.instructions.set(
                    node,
                    AsmHelper.getNumber(Double.doubleToLongBits(originalInsn.asDouble()))
                );
                methodNode.instructions.remove(sourceValue.getProducer());
                return true;
              });

  public static final Match BITS_TO_DOUBLE =
      MethodMatch.invokeStatic()
          .owner("java/lang/Double")
          .name("longBitsToDouble")
          .desc("(J)D")
          .defineTransformation(
              (methodNode, node, frame) -> {
                // Get value from stack
                OriginalSourceValue sourceValue = frame.getStack(frame.getStackSize() - 1);
                OriginalSourceValue originalSourceValue = sourceValue.originalSource;
                if (!originalSourceValue.isOneWayProduced()) return false;

                AbstractInsnNode originalInsn = originalSourceValue.getProducer();
                // Double#longBitsToDouble(long)
                if (!originalInsn.isLong()) return false;

                methodNode.instructions.set(
                    node,
                    AsmHelper.getNumber(Double.longBitsToDouble(originalInsn.asLong()))
                );
                methodNode.instructions.remove(sourceValue.getProducer());
                return true;
              });

  public static final List<Match> METHOD_CALLS_ON_LITERALS = List.of(
      STRING_LENGTH,
      STRING_HASHCODE,
      STRING_TO_INTEGER,
      STRING_TO_INTEGER_RADIX,
      INTEGER_REVERSE,
      LONG_REVERSE,
      FLOAT_TO_BITS,
      BITS_TO_FLOAT,
      DOUBLE_TO_BITS,
      BITS_TO_DOUBLE
  );

  private AsmMathHelper() {
    throw new IllegalArgumentException();
  }

  public static boolean isMathOperation(int opcode) {
    return MATH_OPERATIONS.containsKey(opcode);
  }

  public static Number mathOperation(Number first, Number second, int opcode) {
    return MATH_OPERATIONS.get(opcode).apply(first, second);
  }

  public static int mathOperation(int first, int second, int opcode) {
    return MATH_OPERATIONS.get(opcode).apply(first, second).intValue();
  }

  public static long mathOperation(long first, long second, int opcode) {
    return MATH_OPERATIONS.get(opcode).apply(first, second).longValue();
  }

  public static float mathOperation(float first, float second, int opcode) {
    return MATH_OPERATIONS.get(opcode).apply(first, second).floatValue();
  }

  public static double mathOperation(double first, double second, int opcode) {
    return MATH_OPERATIONS.get(opcode).apply(first, second).doubleValue();
  }

  public static boolean isOneValueCondition(int opcode) {
    return ONE_VALUE_CONDITION_PREDICATES.containsKey(opcode);
  }

  public static boolean condition(int value, int opcode) {
    return ONE_VALUE_CONDITION_PREDICATES.get(opcode).test(value);
  }

  public static boolean isTwoValuesCondition(int opcode) {
    return TWO_VALUES_VALUE_CONDITION_PREDICATES.containsKey(opcode);
  }

  public static boolean condition(int first, int second, int opcode) {
    return TWO_VALUES_VALUE_CONDITION_PREDICATES.get(opcode).test(first, second);
  }

  public static boolean isMathCompare(int opcode) {
    return MATH_COMPARES.containsKey(opcode);
  }

  public static int compare(Number first, Number second, int opcode) {
    return MATH_COMPARES.get(opcode).apply(first, second);
  }

  public static boolean isNumberCast(int opcode) {
    return NUMBER_CASTS.containsKey(opcode);
  }

  public static Number castNumber(Number number, int opcode) {
    return NUMBER_CASTS.get(opcode).apply(number);
  }
}
