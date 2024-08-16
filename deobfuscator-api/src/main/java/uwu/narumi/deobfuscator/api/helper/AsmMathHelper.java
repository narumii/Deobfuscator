package uwu.narumi.deobfuscator.api.helper;

import static org.objectweb.asm.Opcodes.*;

import uwu.narumi.deobfuscator.api.asm.matcher.rule.Match;
import uwu.narumi.deobfuscator.api.asm.matcher.rule.impl.MethodMatch;

import java.util.Map;
import java.util.Objects;
import java.util.function.BiPredicate;
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

  public static final Match STRING_LENGTH =
      MethodMatch.invokeVirtual()
          .owner("java/lang/String")
          .name("length")
          .desc("()I")
          .invokeAction(
              (methodNode, node) -> {
                if (!node.previous().isString()) return false;

                methodNode.instructions.set(
                    node.previous(), AsmHelper.getNumber(node.previous().asString().length()));
                methodNode.instructions.remove(node);
                return true;
              });

  public static final Match STRING_HASHCODE =
      MethodMatch.invokeVirtual()
          .owner("java/lang/String")
          .name("hashCode")
          .desc("()I")
          .invokeAction(
              (methodNode, node) -> {
                if (!node.previous().isString()) return false;

                methodNode.instructions.set(
                    node.previous(), AsmHelper.getNumber(node.previous().asString().hashCode()));
                methodNode.instructions.remove(node);
                return true;
              });

  public static final Match STRING_TO_INTEGER =
      MethodMatch.invokeStatic()
          .owner("java/lang/Integer")
          .name("parseInt", "valueOf")
          .desc("(Ljava/lang/String;)I")
          .invokeAction(
              (methodNode, node) -> {
                if (!node.previous().isString()) return false;

                methodNode.instructions.set(
                    node.previous(),
                    AsmHelper.getNumber(Integer.parseInt(node.previous().asString())));
                methodNode.instructions.remove(node);
                return true;
              });

  public static final Match STRING_TO_INTEGER_RADIX =
      MethodMatch.invokeStatic()
          .owner("java/lang/Integer")
          .name("parseInt", "valueOf")
          .desc("(Ljava/lang/String;I)I")
          .invokeAction(
              (methodNode, node) -> {
                if (!(node.previous().isInteger() && node.previous().previous().isString()))
                  return false;

                methodNode.instructions.set(
                    node.previous(),
                    AsmHelper.getNumber(
                        Integer.parseInt(
                            node.previous().previous().asString(), node.previous().asInteger())));
                methodNode.instructions.remove(node);
                return true;
              });

  public static final Match INTEGER_REVERSE =
      MethodMatch.invokeStatic()
          .owner("java/lang/Integer")
          .name("reverse")
          .desc("(I)I")
          .invokeAction(
              (methodNode, node) -> {
                if (!node.previous().isInteger()) return false;

                methodNode.instructions.set(
                    node.previous(),
                    AsmHelper.getNumber(Integer.reverse(node.previous().asInteger())));
                methodNode.instructions.remove(node);
                return true;
              });

  public static final Match LONG_REVERSE =
      MethodMatch.invokeStatic()
          .owner("java/lang/Long")
          .name("reverse")
          .desc("(J)J")
          .invokeAction(
              (methodNode, node) -> {
                if (!node.previous().isLong()) return false;

                methodNode.instructions.set(
                    node.previous(), AsmHelper.getNumber(Long.reverse(node.previous().asLong())));
                methodNode.instructions.remove(node);
                return true;
              });

  public static final Match FLOAT_TO_BITS =
      MethodMatch.invokeStatic()
          .owner("java/lang/Float")
          .name("floatToIntBits")
          .desc("(F)I")
          .invokeAction(
              (methodNode, node) -> {
                if (!node.previous().isFloat()) return false;

                methodNode.instructions.set(
                    node.previous(),
                    AsmHelper.getNumber(Float.floatToIntBits(node.previous().asFloat())));
                methodNode.instructions.remove(node);
                return true;
              });

  public static final Match BITS_TO_FLOAT =
      MethodMatch.invokeStatic()
          .owner("java/lang/Float")
          .name("intBitsToFloat")
          .desc("(I)F")
          .invokeAction(
              (methodNode, node) -> {
                if (!node.previous().isInteger()) return false;

                methodNode.instructions.set(
                    node.previous(),
                    AsmHelper.getNumber(Float.intBitsToFloat(node.previous().asInteger())));
                methodNode.instructions.remove(node);
                return true;
              });

  public static final Match DOUBLE_TO_BITS =
      MethodMatch.invokeStatic()
          .owner("java/lang/Double")
          .name("doubleToLongBits")
          .desc("(D)J")
          .invokeAction(
              (methodNode, node) -> {
                if (!node.previous().isDouble()) return false;

                methodNode.instructions.set(
                    node.previous(),
                    AsmHelper.getNumber(Double.doubleToLongBits(node.previous().asDouble())));
                methodNode.instructions.remove(node);
                return true;
              });

  public static final Match BITS_TO_DOUBLE =
      MethodMatch.invokeStatic()
          .owner("java/lang/Double")
          .name("longBitsToDouble")
          .desc("(J)D")
          .invokeAction(
              (methodNode, node) -> {
                if (!node.previous().isLong()) return false;

                methodNode.instructions.set(
                    node.previous(),
                    AsmHelper.getNumber(Double.longBitsToDouble(node.previous().asLong())));
                methodNode.instructions.remove(node);
                return true;
              });

  private AsmMathHelper() {
    throw new IllegalArgumentException();
  }

  public static int mathOperation(int first, int second, int opcode) {
    return switch (opcode) {
      case IADD -> first + second;
      case ISUB -> first - second;
      case IMUL -> first * second;
      case IDIV -> first / second;
      case IXOR -> first ^ second;
      case IAND -> first & second;
      case IOR -> first | second;
      case IREM -> first % second;
      case ISHL -> first << second;
      case ISHR -> first >> second;
      case IUSHR -> first >>> second;
      default -> throw new IllegalArgumentException();
    };
  }

  public static long mathOperation(long first, long second, int opcode) {
    return switch (opcode) {
      case LADD -> first + second;
      case LSUB -> first - second;
      case LMUL -> first * second;
      case LDIV -> first / second;
      case LXOR -> first ^ second;
      case LAND -> first & second;
      case LOR -> first | second;
      case LREM -> first % second;
      case LSHL -> first << second;
      case LSHR -> first >> second;
      case LUSHR -> first >>> second;
      default -> throw new IllegalArgumentException();
    };
  }

  public static float mathOperation(float first, float second, int opcode) {
    return switch (opcode) {
      case FADD -> first + second;
      case FSUB -> first - second;
      case FMUL -> first * second;
      case FDIV -> first / second;
      case FREM -> first % second;
      default -> throw new IllegalArgumentException();
    };
  }

  public static double mathOperation(double first, double second, int opcode) {
    return switch (opcode) {
      case DADD -> first + second;
      case DSUB -> first - second;
      case DMUL -> first * second;
      case DDIV -> first / second;
      case DREM -> first % second;
      default -> throw new IllegalArgumentException();
    };
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

  public static int lcmp(long first, long second) {
    return Long.compare(first, second);
  }

  public static int fcmpl(float first, float second) {
    return Float.isNaN(first) || Float.isNaN(second) ? -1 : Float.compare(first, second);
  }

  public static int fcmpg(float first, float second) {
    return Float.isNaN(first) || Float.isNaN(second) ? 1 : Float.compare(first, second);
  }

  public static int dcmpl(double first, double second) {
    return Double.isNaN(first) || Double.isNaN(second) ? -1 : Double.compare(first, second);
  }

  public static int dcmpg(double first, double second) {
    return Double.isNaN(first) || Double.isNaN(second) ? 1 : Double.compare(first, second);
  }
}
