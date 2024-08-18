package org.objectweb.asm.tree.analysis;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.AbstractInsnNode;

import java.util.Objects;
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
   * When the current class is source value of instruction at line 6, then it will follow all
   * instructions (DUP, ILOAD) and jumps to get the original source value of this instruction at line 6.
   * In this example, it will return source value of instruction at line 2.
   */
  public final OriginalSourceValue originalSource;

  public OriginalSourceValue(OriginalSourceValue copiedFrom, AbstractInsnNode insnNode) {
    this(copiedFrom.size, Set.of(insnNode), copiedFrom);
  }

  public OriginalSourceValue(int size) {
    this(size, Set.of());
  }

  public OriginalSourceValue(int size, AbstractInsnNode insnNode) {
    this(size, Set.of(insnNode));
  }

  public OriginalSourceValue(int size, Set<AbstractInsnNode> insnSet) {
    this(size, insnSet, null);
  }

  public OriginalSourceValue(int size, Set<AbstractInsnNode> insnSet, @Nullable OriginalSourceValue copiedFrom) {
    super(size, insnSet);
    this.copiedFrom = copiedFrom;
    this.originalSource = copiedFrom == null ? this : copiedFrom.originalSource;
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
    return Objects.equals(copiedFrom, that.copiedFrom);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), copiedFrom);
  }
}
