// ASM: a very small and fast Java bytecode manipulation framework
// Copyright (c) 2000-2011 INRIA, France Telecom
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
// 1. Redistributions of source code must retain the above copyright
//    notice, this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright
//    notice, this list of conditions and the following disclaimer in the
//    documentation and/or other materials provided with the distribution.
// 3. Neither the name of the copyright holders nor the names of its
//    contributors may be used to endorse or promote products derived from
//    this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
// LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
// SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
// CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
// THE POSSIBILITY OF SUCH DAMAGE.
package org.objectweb.asm.tree;

import static org.objectweb.asm.Opcodes.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.OriginalSourceValue;
import org.objectweb.asm.tree.analysis.SourceValue;
import org.objectweb.asm.tree.analysis.Value;
import uwu.narumi.deobfuscator.api.asm.NamedOpcodes;
import org.objectweb.asm.Type;

/**
 * A node that represents a bytecode instruction. <i>An instruction can appear at most once in at
 * most one {@link InsnList} at a time</i>.
 *
 * @author Eric Bruneton
 */
public abstract class AbstractInsnNode {

  /** The type of {@link InsnNode} instructions. */
  public static final int INSN = 0;

  /** The type of {@link IntInsnNode} instructions. */
  public static final int INT_INSN = 1;

  /** The type of {@link VarInsnNode} instructions. */
  public static final int VAR_INSN = 2;

  /** The type of {@link TypeInsnNode} instructions. */
  public static final int TYPE_INSN = 3;

  /** The type of {@link FieldInsnNode} instructions. */
  public static final int FIELD_INSN = 4;

  /** The type of {@link MethodInsnNode} instructions. */
  public static final int METHOD_INSN = 5;

  /** The type of {@link InvokeDynamicInsnNode} instructions. */
  public static final int INVOKE_DYNAMIC_INSN = 6;

  /** The type of {@link JumpInsnNode} instructions. */
  public static final int JUMP_INSN = 7;

  /** The type of {@link LabelNode} "instructions". */
  public static final int LABEL = 8;

  /** The type of {@link LdcInsnNode} instructions. */
  public static final int LDC_INSN = 9;

  /** The type of {@link IincInsnNode} instructions. */
  public static final int IINC_INSN = 10;

  /** The type of {@link TableSwitchInsnNode} instructions. */
  public static final int TABLESWITCH_INSN = 11;

  /** The type of {@link LookupSwitchInsnNode} instructions. */
  public static final int LOOKUPSWITCH_INSN = 12;

  /** The type of {@link MultiANewArrayInsnNode} instructions. */
  public static final int MULTIANEWARRAY_INSN = 13;

  /** The type of {@link FrameNode} "instructions". */
  public static final int FRAME = 14;

  /** The type of {@link LineNumberNode} "instructions". */
  public static final int LINE = 15;

  // Narumii start
  private static final AbstractInsnNode EMPTY =
      new AbstractInsnNode(-2) {
        @Override
        public int getType() {
          return 0;
        }

        @Override
        public void accept(MethodVisitor methodVisitor) {}

        @Override
        public AbstractInsnNode clone(Map<LabelNode, LabelNode> clonedLabels) {
          return this;
        }
      };
  // Narumii end

  /**
   * The opcode of this instruction, or -1 if this is not a JVM instruction (e.g. a label or a line
   * number).
   */
  protected int opcode;

  /**
   * The runtime visible type annotations of this instruction. This field is only used for real
   * instructions (i.e. not for labels, frames, or line number nodes). This list is a list of {@link
   * TypeAnnotationNode} objects. May be {@literal null}.
   */
  public List<TypeAnnotationNode> visibleTypeAnnotations;

  /**
   * The runtime invisible type annotations of this instruction. This field is only used for real
   * instructions (i.e. not for labels, frames, or line number nodes). This list is a list of {@link
   * TypeAnnotationNode} objects. May be {@literal null}.
   */
  public List<TypeAnnotationNode> invisibleTypeAnnotations;

  /** The previous instruction in the list to which this instruction belongs. */
  AbstractInsnNode previousInsn;

  /** The next instruction in the list to which this instruction belongs. */
  AbstractInsnNode nextInsn;

  /**
   * The index of this instruction in the list to which it belongs. The value of this field is
   * correct only when {@link InsnList#cache} is not null. A value of -1 indicates that this
   * instruction does not belong to any {@link InsnList}.
   */
  int index;

  /**
   * Constructs a new {@link AbstractInsnNode}.
   *
   * @param opcode the opcode of the instruction to be constructed.
   */
  protected AbstractInsnNode(final int opcode) {
    this.opcode = opcode;
    this.index = -1;
  }

  /**
   * Returns the opcode of this instruction.
   *
   * @return the opcode of this instruction, or -1 if this is not a JVM instruction (e.g. a label or
   *     a line number).
   */
  public int getOpcode() {
    return opcode;
  }

  /**
   * Returns the type of this instruction.
   *
   * @return the type of this instruction, i.e. one the constants defined in this class.
   */
  public abstract int getType();

  /**
   * Returns the previous instruction in the list to which this instruction belongs, if any.
   *
   * @return the previous instruction in the list to which this instruction belongs, if any. May be
   *     {@literal null}.
   */
  public AbstractInsnNode getPrevious() {
    return previousInsn;
  }

  /**
   * Returns the next instruction in the list to which this instruction belongs, if any.
   *
   * @return the next instruction in the list to which this instruction belongs, if any. May be
   *     {@literal null}.
   */
  public AbstractInsnNode getNext() {
    return nextInsn;
  }

  /**
   * Makes the given method visitor visit this instruction.
   *
   * @param methodVisitor a method visitor.
   */
  public abstract void accept(MethodVisitor methodVisitor);

  /**
   * Makes the given visitor visit the annotations of this instruction.
   *
   * @param methodVisitor a method visitor.
   */
  protected final void acceptAnnotations(final MethodVisitor methodVisitor) {
    if (visibleTypeAnnotations != null) {
      for (int i = 0, n = visibleTypeAnnotations.size(); i < n; ++i) {
        TypeAnnotationNode typeAnnotation = visibleTypeAnnotations.get(i);
        typeAnnotation.accept(
            methodVisitor.visitInsnAnnotation(
                typeAnnotation.typeRef, typeAnnotation.typePath, typeAnnotation.desc, true));
      }
    }
    if (invisibleTypeAnnotations != null) {
      for (int i = 0, n = invisibleTypeAnnotations.size(); i < n; ++i) {
        TypeAnnotationNode typeAnnotation = invisibleTypeAnnotations.get(i);
        typeAnnotation.accept(
            methodVisitor.visitInsnAnnotation(
                typeAnnotation.typeRef, typeAnnotation.typePath, typeAnnotation.desc, false));
      }
    }
  }

  /**
   * Returns a copy of this instruction.
   *
   * @param clonedLabels a map from LabelNodes to cloned LabelNodes.
   * @return a copy of this instruction. The returned instruction does not belong to any {@link
   *     InsnList}.
   */
  public abstract AbstractInsnNode clone(Map<LabelNode, LabelNode> clonedLabels);

  /**
   * Returns the clone of the given label.
   *
   * @param label a label.
   * @param clonedLabels a map from LabelNodes to cloned LabelNodes.
   * @return the clone of the given label.
   */
  static LabelNode clone(final LabelNode label, final Map<LabelNode, LabelNode> clonedLabels) {
    return clonedLabels.get(label);
  }

  /**
   * Returns the clones of the given labels.
   *
   * @param labels a list of labels.
   * @param clonedLabels a map from LabelNodes to cloned LabelNodes.
   * @return the clones of the given labels.
   */
  static LabelNode[] clone(
      final List<LabelNode> labels, final Map<LabelNode, LabelNode> clonedLabels) {
    LabelNode[] clones = new LabelNode[labels.size()];
    for (int i = 0, n = clones.length; i < n; ++i) {
      clones[i] = clonedLabels.get(labels.get(i));
    }
    return clones;
  }

  /**
   * Clones the annotations of the given instruction into this instruction.
   *
   * @param insnNode the source instruction.
   * @return this instruction.
   */
  protected final AbstractInsnNode cloneAnnotations(final AbstractInsnNode insnNode) {
    if (insnNode.visibleTypeAnnotations != null) {
      this.visibleTypeAnnotations = new ArrayList<>();
      for (int i = 0, n = insnNode.visibleTypeAnnotations.size(); i < n; ++i) {
        TypeAnnotationNode sourceAnnotation = insnNode.visibleTypeAnnotations.get(i);
        TypeAnnotationNode cloneAnnotation =
            new TypeAnnotationNode(
                sourceAnnotation.typeRef, sourceAnnotation.typePath, sourceAnnotation.desc);
        sourceAnnotation.accept(cloneAnnotation);
        this.visibleTypeAnnotations.add(cloneAnnotation);
      }
    }
    if (insnNode.invisibleTypeAnnotations != null) {
      this.invisibleTypeAnnotations = new ArrayList<>();
      for (int i = 0, n = insnNode.invisibleTypeAnnotations.size(); i < n; ++i) {
        TypeAnnotationNode sourceAnnotation = insnNode.invisibleTypeAnnotations.get(i);
        TypeAnnotationNode cloneAnnotation =
            new TypeAnnotationNode(
                sourceAnnotation.typeRef, sourceAnnotation.typePath, sourceAnnotation.desc);
        sourceAnnotation.accept(cloneAnnotation);
        this.invisibleTypeAnnotations.add(cloneAnnotation);
      }
    }
    return this;
  }

  // Narumii start - shorthand methods
  public boolean isString() {
    return this instanceof LdcInsnNode && ((LdcInsnNode) this).cst instanceof String;
  }

  public boolean isType() {
    return this instanceof LdcInsnNode && ((LdcInsnNode) this).cst instanceof Type;
  }

  public boolean isInteger() {
    int opcode = this.getOpcode();
    return ((opcode >= ICONST_M1 && opcode <= ICONST_5)
        || opcode == BIPUSH
        || opcode == SIPUSH
        || (this instanceof LdcInsnNode && ((LdcInsnNode) this).cst instanceof Integer));
  }

  public boolean isLong() {
    int opcode = this.getOpcode();
    return (opcode == LCONST_0
        || opcode == LCONST_1
        || (this instanceof LdcInsnNode && ((LdcInsnNode) this).cst instanceof Long));
  }

  public boolean isFloat() {
    int opcode = this.getOpcode();
    return (opcode >= FCONST_0 && opcode <= FCONST_2)
        || (this instanceof LdcInsnNode && ((LdcInsnNode) this).cst instanceof Float);
  }

  public boolean isDouble() {
    int opcode = this.getOpcode();
    return (opcode >= DCONST_0 && opcode <= DCONST_1)
        || (this instanceof LdcInsnNode && ((LdcInsnNode) this).cst instanceof Double);
  }

  public boolean isNumber() {
    return (isInteger() || isLong() || isFloat() || isDouble());
  }

  public boolean isConstant() {
    return isNumber() || isType() || isString() || isNull();
  }

  public boolean isNull() {
    return this.getOpcode() == ACONST_NULL;
  }

  public String asString() {
    return (String) ((LdcInsnNode) this).cst;
  }

  public Type asType() {
    return (Type) ((LdcInsnNode) this).cst;
  }

  @Nullable
  public Object asConstant() {
    if (isNumber()) {
      return asNumber();
    } else if (isString()) {
      return asString();
    } else if (isType()) {
      return asType();
    } else if (isNull()) {
      return null;
    }

    throw new IllegalArgumentException("Not a constant");
  }

  public int asInteger() {
    int opcode = this.getOpcode();

    if (opcode >= ICONST_M1 && opcode <= ICONST_5) {
      return opcode - 3;
    } else if (this instanceof IntInsnNode && this.getOpcode() != NEWARRAY) {
      return ((IntInsnNode) this).operand;
    } else if (this instanceof LdcInsnNode && ((LdcInsnNode) this).cst instanceof Integer) {
      return (Integer) ((LdcInsnNode) this).cst;
    }

    throw new IllegalArgumentException("Not an integer");
  }

  public long asLong() {
    int opcode = this.getOpcode();

    if (opcode >= LCONST_0 && opcode <= LCONST_1) {
      return opcode - 9;
    } else if (this instanceof LdcInsnNode && ((LdcInsnNode) this).cst instanceof Long) {
      return (Long) ((LdcInsnNode) this).cst;
    }

    throw new IllegalArgumentException("Not a long");
  }

  public float asFloat() {
    int opcode = this.getOpcode();

    if (opcode >= FCONST_0 && opcode <= FCONST_2) {
      return opcode - 11;
    } else if (this instanceof LdcInsnNode && ((LdcInsnNode) this).cst instanceof Float) {
      return (Float) ((LdcInsnNode) this).cst;
    }

    throw new IllegalArgumentException("Not a float");
  }

  public double asDouble() {
    int opcode = this.getOpcode();

    if (opcode >= DCONST_0 && opcode <= DCONST_1) {
      return opcode - 14;
    } else if (this instanceof LdcInsnNode && ((LdcInsnNode) this).cst instanceof Double) {
      return (Double) ((LdcInsnNode) this).cst;
    }

    throw new IllegalArgumentException("Not a double");
  }

  public Number asNumber() {
    if (isInteger()) {
      return asInteger();
    } else if (isLong()) {
      return asLong();
    } else if (isDouble()) {
      return asDouble();
    } else if (isFloat()) {
      return asFloat();
    }

    throw new IllegalArgumentException("Not a number");
  }

  public boolean isMathOperator() {
    return (this.getOpcode() >= IADD && this.getOpcode() < INEG)
        || (this.getOpcode() > DNEG && this.getOpcode() <= LXOR);
  }

  public boolean isNumberOperator() {
    return (this.getOpcode() >= INEG && this.getOpcode() <= DNEG)
        || (this.getOpcode() >= I2L && this.getOpcode() <= I2S);
  }

  public boolean isVarLoad() {
    return this.getOpcode() >= ILOAD && this.getOpcode() <= ALOAD;
  }

  public boolean isVarStore() {
    return this.getOpcode() >= ISTORE && this.getOpcode() <= ASTORE;
  }

  public int sizeOnStack() {
    if (this.isLong() || this.isDouble()) {
      // Only long and double values take up two stack values
      return 2;
    } else {
      return 1;
    }
  }

  public boolean isJump() {
    return this instanceof JumpInsnNode;
  }

  public JumpInsnNode asJump() {
    return (JumpInsnNode) this;
  }

  public int conditionStackSize() {
    if (this.getOpcode() >= IF_ICMPEQ && this.getOpcode() <= IF_ICMPLE) return 2;

    if ((this.getOpcode() >= IFEQ && this.getOpcode() <= IFLE)
        || (this.getOpcode() == IFNULL || this.getOpcode() == IFNONNULL)) return 1;

    return 0;
  }

  public MethodInsnNode asMethodInsn() {
    return (MethodInsnNode) this;
  }

  public JumpInsnNode asFieldInsn() {
    return (JumpInsnNode) this;
  }

  public InvokeDynamicInsnNode asInvokeDynamicInsn() {
    return (InvokeDynamicInsnNode) this;
  }

  public boolean isMethodInsn() {
    return this instanceof MethodInsnNode;
  }

  public boolean isFieldInsn() {
    return this instanceof FieldInsnNode;
  }

  public boolean isInvokeDynamicInsn() {
    return this instanceof InvokeDynamicInsnNode;
  }

  public AbstractInsnNode getPrevious(int offset) {
    AbstractInsnNode current = this;
    for (int i = 0; i < offset; i++) {
      if (current == null) break;

      current = current.previousInsn;
    }

    return current;
  }

  public <T extends AbstractInsnNode> T getPreviousAs() {
    return (T) getPrevious();
  }

  public <T extends AbstractInsnNode> T getPreviousAs(int offset) {
    return (T) getPrevious(offset);
  }

  public AbstractInsnNode getNext(int offset) {
    AbstractInsnNode current = this;
    for (int i = 0; i < offset; i++) {
      if (current == null) break;

      current = current.nextInsn;
    }

    return current;
  }

  public <T extends AbstractInsnNode> T getNextAs() {
    return (T) getNext();
  }

  public <T extends AbstractInsnNode> T getNextAs(int offset) {
    return (T) getNext(offset);
  }

  public AbstractInsnNode previous() {
    AbstractInsnNode current = this;
    do {
      current = current.previousInsn;
      if (current == null) break;
    } while (current instanceof LabelNode
        || current instanceof FrameNode
        || current instanceof LineNumberNode);

    return current == null ? EMPTY : current;
  }

  public AbstractInsnNode previous(int offset) {
    AbstractInsnNode current = this;
    for (int i = 0; i < offset; i++) {
      if (current == null) break;

      current = current.previous();
    }

    return current;
  }

  public <T extends AbstractInsnNode> T previousAs() {
    return (T) previous();
  }

  public <T extends AbstractInsnNode> T previousAs(int offset) {
    return (T) previous(offset);
  }

  public AbstractInsnNode next() {
    AbstractInsnNode current = this;
    do {
      current = current.nextInsn;
      if (current == null) break;
    } while (current instanceof LabelNode
        || current instanceof FrameNode
        || current instanceof LineNumberNode);

    return current == null ? EMPTY : current;
  }

  public AbstractInsnNode next(int offset) {
    AbstractInsnNode current = this;
    for (int i = 0; i < offset; i++) {
      if (current == null) break;

      current = current.next();
    }

    return current;
  }

  public <T extends AbstractInsnNode> T nextAs() {
    return (T) next();
  }

  public <T extends AbstractInsnNode> T nextAs(int offset) {
    return (T) next(offset);
  }

  public AbstractInsnNode walkPreviousUntil(
      Predicate<AbstractInsnNode> until, Consumer<AbstractInsnNode> consumer) {
    AbstractInsnNode current = this;
    while (current != null && !until.test(current)) {
      consumer.accept(current);
      current = current.previousInsn;
    }

    return current;
  }

  public AbstractInsnNode walkNextUntil(
      Predicate<AbstractInsnNode> until, Consumer<AbstractInsnNode> consumer) {
    AbstractInsnNode current = this;
    while (current != null && !until.test(current)) {
      consumer.accept(current);
      current = current.nextInsn;
    }

    return current;
  }

  public AbstractInsnNode walkPreviousUntil(
      Predicate<AbstractInsnNode> until,
      Predicate<AbstractInsnNode> filter,
      Consumer<AbstractInsnNode> consumer) {
    AbstractInsnNode current = this;
    while (current != null && !until.test(current)) {
      if (filter.test(current)) consumer.accept(current);
      current = current.previousInsn;
    }

    return current;
  }

  public AbstractInsnNode walkNextUntil(
      Predicate<AbstractInsnNode> until,
      Predicate<AbstractInsnNode> filter,
      Consumer<AbstractInsnNode> consumer) {
    AbstractInsnNode current = this;
    while (current != null && !until.test(current)) {
      if (filter.test(current)) consumer.accept(current);
      current = current.nextInsn;
    }

    return current;
  }

  public String namedOpcode() {
    return NamedOpcodes.map(this.getOpcode());
  }

  /**
   * Returns the number of stack values required by this instruction.
   */
  public int getRequiredStackValuesCount(Frame<? extends Value> frame) {
    return switch (this.getOpcode()) {
      // Unary operations (one value)
      case ISTORE, LSTORE, FSTORE, DSTORE, ASTORE, POP, DUP, DUP_X1, SWAP, INEG,
           LNEG, FNEG, DNEG, I2L, I2F, I2D, L2I, L2F, L2D, F2I, F2L, F2D, D2I, D2L, D2F, I2B, I2C, I2S, IFEQ, IFNE,
           IFLT, IFGE, IFGT, IFLE, TABLESWITCH, LOOKUPSWITCH, IRETURN, LRETURN, FRETURN, DRETURN, ARETURN, PUTSTATIC,
           GETFIELD, NEWARRAY, ANEWARRAY, ARRAYLENGTH, ATHROW, CHECKCAST, INSTANCEOF, MONITORENTER, MONITOREXIT, IFNULL,
           IFNONNULL -> 1;
      // Binary operations (two values)
      case IALOAD, LALOAD, FALOAD, DALOAD, AALOAD, BALOAD, CALOAD, SALOAD, IADD, LADD, FADD, DADD, ISUB, LSUB, FSUB,
           DSUB, IMUL, LMUL, FMUL, DMUL, IDIV, LDIV, FDIV, DDIV, IREM, LREM, FREM, DREM, ISHL, LSHL, ISHR, LSHR, IUSHR,
           LUSHR, IAND, LAND, IOR, LOR, IXOR, LXOR, LCMP, FCMPL, FCMPG, DCMPL, DCMPG, IF_ICMPEQ, IF_ICMPNE, IF_ICMPLT,
           IF_ICMPGE, IF_ICMPGT, IF_ICMPLE, IF_ACMPEQ, IF_ACMPNE, PUTFIELD -> 2;
      // Ternary operations (three values)
      case IASTORE, LASTORE, FASTORE, DASTORE, AASTORE, BASTORE, CASTORE, SASTORE -> 3;

      // Method invocation
      case INVOKEVIRTUAL, INVOKESPECIAL, INVOKESTATIC, INVOKEINTERFACE, INVOKEDYNAMIC -> getRequiredStackValuesCountForMethodInvocation();
      // Multi-dimensional array creation
      case MULTIANEWARRAY -> ((MultiANewArrayInsnNode) this).dims;

      // Dynamic forms (uses frame)
      case POP2, DUP2 -> {
        Value sourceValue = frame.getStack(frame.getStackSize() - 1);
        yield sourceValue.getSize() == 2 ? 1 : 2;
      }
      case DUP_X2 -> {
        Value sourceValue2 = frame.getStack(frame.getStackSize() - 2);
        yield sourceValue2.getSize() == 2 ? 2 : 3;
      }
      case DUP2_X1 -> {
        Value sourceValue1 = frame.getStack(frame.getStackSize() - 1);
        yield sourceValue1.getSize() == 2 ? 2 : 3;
      }
      case DUP2_X2 -> {
        Value sourceValue1 = frame.getStack(frame.getStackSize() - 1);
        Value sourceValue2 = frame.getStack(frame.getStackSize() - 2);
        if (sourceValue1.getSize() == 2 && sourceValue2.getSize() == 2) {
          yield 2;
        }
        Value sourceValue3 = frame.getStack(frame.getStackSize() - 2);
        if (sourceValue1.getSize() == 2 || sourceValue3.getSize() == 2) {
          yield 3;
        }
        yield 4;
      }

      // No values required
      default -> 0;
    };
  }

  /**
   * Calculates the number of stack values required for a method invocation by descriptor.
   */
  private int getRequiredStackValuesCountForMethodInvocation() {
    String desc;
    if (this instanceof MethodInsnNode methodInsn) {
      desc = methodInsn.desc;
    } else if (this instanceof InvokeDynamicInsnNode invokeDynamicInsn) {
      desc = invokeDynamicInsn.desc;
    } else {
      throw new IllegalStateException("Not a method instruction");
    }

    int count = Type.getArgumentCount(desc); // Arguments count = Stack values count
    if (this.getOpcode() != INVOKESTATIC && this.getOpcode() != INVOKEDYNAMIC) {
      count++; // "this" reference
    }

    return count;
  }

  @Override
  public String toString() {
    return "(" + namedOpcode() + ") " + super.toString();
  }
  // Narumii end
}
