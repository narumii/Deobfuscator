package uwu.narumi.deobfuscator.api.helper;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.objectweb.asm.tree.analysis.BasicValue;
import uwu.narumi.deobfuscator.api.asm.NamedOpcodes;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.JumpPredictingAnalyzer;
import org.objectweb.asm.tree.analysis.OriginalSourceInterpreter;
import org.objectweb.asm.tree.analysis.OriginalSourceValue;
import uwu.narumi.deobfuscator.api.asm.FieldRef;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class MethodHelper implements Opcodes {
  /**
   * Analyzes the stack frames of the method using {@link OriginalSourceInterpreter}
   *
   * @param classNode The owner class
   * @param methodNode Method
   * @return A map which corresponds to: instruction -> its own stack frame
   */
  @NotNull
  @Unmodifiable
  public static Map<AbstractInsnNode, Frame<OriginalSourceValue>> analyzeSource(
      ClassNode classNode, MethodNode methodNode
  ) {
    Map<AbstractInsnNode, Frame<OriginalSourceValue>> frames = new HashMap<>();
    Frame<OriginalSourceValue>[] framesArray;
    try {
      framesArray = new Analyzer<>(new OriginalSourceInterpreter()).analyze(classNode.name, methodNode);
    } catch (AnalyzerException e) {
      throw new RuntimeException("Error analyzing " + classNode.name + "#" + methodNode.name + methodNode.desc, e);
    }
    for (int i = 0; i < framesArray.length; i++) {
      frames.put(methodNode.instructions.get(i), framesArray[i]);
    }
    return Collections.unmodifiableMap(frames);
  }

  /**
   * Analyzes the stack frames of the method using {@link OriginalSourceInterpreter}
   * and predicts jumps
   *
   * @param classNode The owner class
   * @param methodNode Method
   * @return A map which corresponds to: instruction -> its own stack frame
   */
  @NotNull
  @Unmodifiable
  public static Map<AbstractInsnNode, Frame<OriginalSourceValue>> analyzeSourcePredictJumps(
      ClassNode classNode, MethodNode methodNode
  ) {
    Map<AbstractInsnNode, Frame<OriginalSourceValue>> frames = new HashMap<>();
    Frame<OriginalSourceValue>[] framesArray;
    try {
      framesArray = new JumpPredictingAnalyzer(new OriginalSourceInterpreter()).analyze(classNode.name, methodNode);
    } catch (AnalyzerException e) {
      throw new RuntimeException("Error analyzing " + classNode.name + "#" + methodNode.name + methodNode.desc, e);
    }
    for (int i = 0; i < framesArray.length; i++) {
      frames.put(methodNode.instructions.get(i), framesArray[i]);
    }
    return Collections.unmodifiableMap(frames);
  }

  /**
   * Analyzes the stack frames of the method using {@link BasicInterpreter}
   *
   * @param classNode The owner class
   * @param methodNode Method
   * @return A map which corresponds to: instruction -> its own stack frame
   */
  @NotNull
  @Unmodifiable
  public static Map<AbstractInsnNode, Frame<BasicValue>> analyzeBasic(
      ClassNode classNode, MethodNode methodNode
  ) {
    Map<AbstractInsnNode, Frame<BasicValue>> frames = new HashMap<>();
    Frame<BasicValue>[] framesArray;
    try {
      framesArray = new Analyzer<>(new BasicInterpreter()).analyze(classNode.name, methodNode);
    } catch (AnalyzerException e) {
      throw new RuntimeException("Error analyzing " + classNode.name + "#" + methodNode.name + methodNode.desc, e);
    }
    for (int i = 0; i < framesArray.length; i++) {
      frames.put(methodNode.instructions.get(i), framesArray[i]);
    }
    return Collections.unmodifiableMap(frames);
  }

  /**
   * Computes a map which corresponds to: source value producer -> consumers
   *
   * @param frames Frames of the method
   * @return A map where keys are instructions that produce values and values are
   *  *      sets of instructions that consume those produced values
   */
  public static Map<AbstractInsnNode, Set<AbstractInsnNode>> computeConsumersMap(Map<AbstractInsnNode, Frame<OriginalSourceValue>> frames) {
    Map<AbstractInsnNode, Set<AbstractInsnNode>> consumers = new HashMap<>();
    for (var entry : frames.entrySet()) {
      AbstractInsnNode consumer = entry.getKey();
      Frame<OriginalSourceValue> frame = entry.getValue();
      if (frame == null) continue;

      // Loop through stack values and add consumer to them
      for (int i = 0; i < consumer.getConsumedStackValuesCount(frame); i++) {
        // Get the value from the stack (first consumed value is at the top)
        OriginalSourceValue sourceValue = frame.getStack(frame.getStackSize() - (i + 1));

        for (AbstractInsnNode producer : sourceValue.insns) {
          // Add this consumer to the set of consumers for the producer instruction
          consumers.computeIfAbsent(producer, k -> new HashSet<>()).add(consumer);
        }
      }
    }
    return consumers;
  }

  public static List<String> prettyInsnList(InsnList insnList) {
    return prettyInsnList(Arrays.asList(insnList.toArray()));
  }

  public static List<String> prettyInsnList(List<AbstractInsnNode> insnList) {
    return insnList.stream().map(insn -> NamedOpcodes.map(insn.getOpcode())).toList();
  }

  /**
   * Gets local variable table of the method. Sorted ascending
   */
  public static List<Integer> getLocalVariableTable(MethodNode methodNode) {
    List<Integer> localVariableTable = new ArrayList<>();
    for (AbstractInsnNode insn : methodNode.instructions.toArray()) {
      if (insn instanceof VarInsnNode varInsn) {
        if (!localVariableTable.contains(varInsn.var)) {
          localVariableTable.add(varInsn.var);
        }
      } else if (insn instanceof IincInsnNode iincInsn) {
        if (!localVariableTable.contains(iincInsn.var)) {
          localVariableTable.add(iincInsn.var);
        }
      }
    }

    // Sort ascending
    localVariableTable.sort(Comparator.naturalOrder());

    return localVariableTable;
  }

  /**
   * Gets var index of the first parameter of the passed method
   */
  public static int getFirstParameterIdx(MethodNode methodNode) {
    // When method is static, then the first var index is actually a reference to "this"
    return (methodNode.access & ACC_STATIC) != 0 ? 0 : 1;
  }

  public static Map<Integer, Integer> collectKnownIntLocals(
      MethodNode methodNode,
      Map<AbstractInsnNode, Frame<OriginalSourceValue>> frames,
      Map<FieldRef, Integer> fieldValues,
      Integer methodSalt
  ) {
    Map<Integer, Integer> locals = new HashMap<>();
    if (methodSalt != null && hasTrailingIntArgument(methodNode.desc)) {
      locals.put(lastArgumentSlot(methodNode.access, methodNode.desc), methodSalt);
    }

    boolean changed;
    int rounds = 0;
    do {
      changed = false;
      rounds++;

      for (AbstractInsnNode insn : methodNode.instructions.toArray()) {
        if (!(insn instanceof VarInsnNode store) || store.getOpcode() != ISTORE) continue;

        Frame<OriginalSourceValue> frame = frames.get(store);
        if (frame == null || frame.getStackSize() == 0) continue;

        Optional<Integer> value = evaluateInt(frame.getStack(frame.getStackSize() - 1), frames, locals, fieldValues);
        if (value.isPresent() && !value.get().equals(locals.get(store.var))) {
          locals.put(store.var, value.get());
          changed = true;
        }
      }
    } while (changed && rounds < 6);

    return locals;
  }

  public static Optional<Integer> evaluateInt(
      OriginalSourceValue source,
      Map<AbstractInsnNode, Frame<OriginalSourceValue>> frames,
      Map<Integer, Integer> locals,
      Map<FieldRef, Integer> fieldValues
  ) {
    if (source == null) return Optional.empty();
    if (source.getConstantValue() != null && source.getConstantValue().get() instanceof Number number) {
      return Optional.of(number.intValue());
    }
    if (!source.isOneWayProduced()) return Optional.empty();
    return evaluateInt(source.getProducer(), frames, locals, fieldValues, identitySet());
  }

  public static void removeStackProducers(
      MethodNode methodNode,
      AbstractInsnNode consumer,
      Map<AbstractInsnNode, Frame<OriginalSourceValue>> frames,
      int count
  ) {
    Frame<OriginalSourceValue> frame = frames.get(consumer);
    if (frame == null || frame.getStackSize() < count) return;

    Set<AbstractInsnNode> toRemove = identitySet();
    for (int i = 0; i < count; i++) {
      OriginalSourceValue source = frame.getStack(frame.getStackSize() - 1 - i);
      collectExpressionInsns(source, frames, toRemove, identitySet());
    }

    toRemove.stream()
        .filter(insn -> insn != consumer)
        .filter(methodNode.instructions::contains)
        .sorted(Comparator.comparingInt(methodNode.instructions::indexOf).reversed())
        .forEach(methodNode.instructions::remove);
  }

  public static List<Integer> argumentSlots(int access, String desc) {
    List<Integer> slots = new ArrayList<>();
    int slot = (access & ACC_STATIC) != 0 ? 0 : 1;
    for (Type arg : Type.getArgumentTypes(desc)) {
      slots.add(slot);
      slot += arg.getSize();
    }
    return slots;
  }

  public static boolean hasTrailingIntArgument(String desc) {
    Type[] args = Type.getArgumentTypes(desc);
    return args.length > 0 && args[args.length - 1].equals(Type.INT_TYPE);
  }

  private static int lastArgumentSlot(int access, String desc) {
    Type[] args = Type.getArgumentTypes(desc);
    int slot = (access & ACC_STATIC) != 0 ? 0 : 1;
    for (int i = 0; i < args.length - 1; i++) {
      slot += args[i].getSize();
    }
    return slot;
  }

  private static Optional<Integer> evaluateInt(
      AbstractInsnNode producer,
      Map<AbstractInsnNode, Frame<OriginalSourceValue>> frames,
      Map<Integer, Integer> locals,
      Map<FieldRef, Integer> fieldValues,
      Set<AbstractInsnNode> visiting
  ) {
    if (producer == null || !visiting.add(producer)) return Optional.empty();

    if (producer.isInteger()) return Optional.of(producer.asInteger());

    if (producer instanceof FieldInsnNode field && field.getOpcode() == GETSTATIC && field.desc.equals("I")) {
      return Optional.ofNullable(fieldValues.get(FieldRef.of(field)));
    }

    if (producer instanceof VarInsnNode varInsn && varInsn.getOpcode() == ILOAD) {
      return Optional.ofNullable(locals.get(varInsn.var));
    }

    if (producer.getOpcode() == INEG || producer.getOpcode() == I2B || producer.getOpcode() == I2C || producer.getOpcode() == I2S) {
      Frame<OriginalSourceValue> frame = frames.get(producer);
      if (frame == null || frame.getStackSize() < 1) return Optional.empty();

      Optional<Integer> value = evaluateInt(frame.getStack(frame.getStackSize() - 1), frames, locals, fieldValues);
      if (value.isEmpty()) return Optional.empty();

      return switch (producer.getOpcode()) {
        case INEG -> Optional.of(-value.get());
        case I2B -> Optional.of((int) value.get().byteValue());
        case I2C -> Optional.of((int) (char) value.get().intValue());
        case I2S -> Optional.of((int) value.get().shortValue());
        default -> Optional.empty();
      };
    }

    if (AsmMathHelper.isMathBinaryOperation(producer.getOpcode())) {
      Frame<OriginalSourceValue> frame = frames.get(producer);
      if (frame == null || frame.getStackSize() < 2) return Optional.empty();

      Optional<Integer> left = evaluateInt(frame.getStack(frame.getStackSize() - 2), frames, locals, fieldValues);
      Optional<Integer> right = evaluateInt(frame.getStack(frame.getStackSize() - 1), frames, locals, fieldValues);
      if (left.isEmpty() || right.isEmpty()) return Optional.empty();

      try {
        return Optional.of(AsmMathHelper.mathBinaryOperation(left.get(), right.get(), producer.getOpcode()).intValue());
      } catch (ArithmeticException ignored) {
        return Optional.empty();
      }
    }

    return Optional.empty();
  }

  private static void collectExpressionInsns(
      OriginalSourceValue source,
      Map<AbstractInsnNode, Frame<OriginalSourceValue>> frames,
      Set<AbstractInsnNode> output,
      Set<AbstractInsnNode> visiting
  ) {
    if (source == null || !source.isOneWayProduced()) return;

    AbstractInsnNode producer = source.getProducer();
    if (producer == null || !visiting.add(producer)) return;

    if (producer instanceof VarInsnNode && producer.isVarStore()) return;

    output.add(producer);

    Frame<OriginalSourceValue> frame = frames.get(producer);
    if (frame == null) return;

    int consumed = producer.getConsumedStackValuesCount(frame);
    for (int i = 0; i < consumed && i < frame.getStackSize(); i++) {
      collectExpressionInsns(frame.getStack(frame.getStackSize() - 1 - i), frames, output, visiting);
    }
  }

  private static Set<AbstractInsnNode> identitySet() {
    return Collections.newSetFromMap(new IdentityHashMap<>());
  }
}
