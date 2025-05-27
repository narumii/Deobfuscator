package org.objectweb.asm.tree.analysis;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * A {@link SourceValue} that holds the original source value on which we can operate very easily.
 * See javadoc of {@link #originalSource} for more details.
 *
 * @author EpicPlayerA10
 */
public class OriginalSourceValue extends SourceValue {

  /**
   * If the value was copied from another value, then this field will contain the value from which it was copied.
   *
   * @apiNote If this field is not null then you are 100% sure that {@link #insns} field will contain only one instruction.
   * And therefore you can use {@link #getProducer()} method to get the producer of this value.
   */
  @Nullable
  public final OriginalSourceValue copiedFrom;

  /**
   * This field holds the original source value on which we can operate very easily. This means that
   * the original source value was obtained by following all copy-like instructions (DUP, ILOAD, etc.)
   * and all kind of jumps. Very useful!
   *
   * <p>
   * Consider this example set of instructions:
   * <pre>
   * 1: A:
   * 2:   ICONST_1
   * 3:   ISTORE exVar
   * 4: B:
   * 5:   ILOAD exVar
   * 6:   DUP
   * 7:   IFNE C
   * 8: C:
   * 9:   ...
   * </pre>
   *
   * When the current class is source value of instruction at line 6, then it will follow all
   * instructions (DUP, ILOAD) and jumps to get the original source value of this instruction at line 6.
   * In this example, it will return source value of instruction at line 2.
   */
  @NotNull
  public final OriginalSourceValue originalSource;

  /**
   * Predicted constant value that holds an object to constant value such as {@link Integer}, {@link Double},
   * {@link Float}, {@link String}, {@link Type} and {@code null}. Additionally, to {@link #originalSource} it is
   * also doing all math operations on math operation instructions.
   *
   * <p>
   * Consider this example:
   * <pre>
   * 1: A:
   * 2:   ldc 12L
   * 3:   ldc 2L
   * 4:   ldiv
   * 5:   l2i
   * 6:   lookupswitch {
   * 7:     ...
   * 8:   }
   * </pre>
   *
   * In line 2, the constant value is 12L.<br>
   * In line 3, the constant value is 2L.<br>
   * In line 4, the constant value is 12L / 2L = 6L.<br>
   * In line 5, the constant value is 6 (but cast to integer).
   *
   * <p>
   * It is so convenient because for example if you want to get value of a IMUL instruction,
   * then this field already contains the calculated value! No need to calculate it manually from stack values.
   */
  @Nullable
  private ConstantValue constantValue = null;

  /**
   * If a source value is a method parameter.
   */
  private final boolean isMethodParameter;

  public OriginalSourceValue(int size, boolean isMethodParameter) {
    super(size, new SmallSet<>());
    this.isMethodParameter = isMethodParameter;

    // Fill single-producer-only fields with empty
    this.copiedFrom = null;
    this.originalSource = this;
  }

  public OriginalSourceValue(int size, AbstractInsnNode insnNode) {
    this(size, insnNode, null, null);
  }

  /**
   * Create new {@link OriginalSourceValue} from multiple producers
   *
   * @param size Stack size of the value
   * @param insnSet Set of instructions that produce this value
   */
  public OriginalSourceValue(int size, Set<AbstractInsnNode> insnSet) {
    super(size, insnSet);
    this.isMethodParameter = false;

    // Fill single-producer-only fields with empty
    this.copiedFrom = null;
    this.originalSource = this;
  }

  public OriginalSourceValue(AbstractInsnNode insnNode, OriginalSourceValue copiedFrom) {
    this(copiedFrom.size, insnNode, copiedFrom, null);
  }

  /**
   * Create new {@link OriginalSourceValue} from a single producer
   *
   * @param size Stack size of the value
   * @param insn An instruction that produces this value
   * @param copiedFrom The value from which this value was copied or null if it was not copied
   * @param constantValue Predicted constant value if exists
   */
  public OriginalSourceValue(int size, AbstractInsnNode insn, @Nullable OriginalSourceValue copiedFrom, @Nullable ConstantValue constantValue) {
    super(size, new SmallSet<>(insn));
    this.isMethodParameter = false;
    this.copiedFrom = copiedFrom;
    this.originalSource = copiedFrom == null || copiedFrom.isMethodParameter ? this : copiedFrom.originalSource;

    if (constantValue != null) {
      // If the constant value is present, then use it
      this.constantValue = constantValue;
    } else if (copiedFrom != null) {
      // Copy constant value from copied value
      this.constantValue = copiedFrom.constantValue;
    } else {
      // Try to infer constant value from producer
      if (insn.isConstant()) {
        this.constantValue = ConstantValue.of(insn.asConstant());
      }
    }
  }

  /**
   * Check if the value was produced only by one instruction.
   *
   * @apiNote If this function returns {@code true}, then the {@link #insns} field will contain only one instruction.
   */
  public boolean isOneWayProduced() {
    return insns.size() == 1;
  }

  /**
   * Get the producer of this value.
   *
   * @throws IllegalStateException If there are multiple producers. Check {@link #isOneWayProduced()} before calling this method.
   */
  public AbstractInsnNode getProducer() {
    if (insns.size() != 1) {
      throw new IllegalStateException("Expected only one instruction, but got " + insns.size());
    }
    return insns.iterator().next();
  }

  /**
   * See {@link #constantValue}.
   */
  @Nullable
  public ConstantValue getConstantValue() {
    return constantValue;
  }

  public boolean isMethodParameter() {
    return isMethodParameter;
  }

  /**
   * Walk to the last parent value until the predicate returns true.
   *
   * @param until The predicate to stop walking. If the predicate returns true, the walking will stop.
   */
  public OriginalSourceValue walkToLastParentValue(Predicate<OriginalSourceValue> until) {
    OriginalSourceValue value = this;
    while (value.copiedFrom != null) {
      if (until.test(value)) {
        break;
      }
      value = value.copiedFrom;
    }
    return value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;
    OriginalSourceValue that = (OriginalSourceValue) o;
    return Objects.equals(constantValue, that.constantValue) && isMethodParameter == that.isMethodParameter && Objects.equals(copiedFrom, that.copiedFrom);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), constantValue, isMethodParameter, copiedFrom);
  }

  /**
   * We need to create our own {@link Optional}-like class because {@link Optional} can't
   * store nullable values which we need to store.
   *
   * @param value A constant value. It can be {@link Integer}, {@link Double},
   * {@link Float}, {@link String}, {@link Type} or {@code null}
   */
  public record ConstantValue(Object value) {
    public static ConstantValue of(Object value) {
      return new ConstantValue(value);
    }

    public Object get() {
      return value;
    }
  }
}
