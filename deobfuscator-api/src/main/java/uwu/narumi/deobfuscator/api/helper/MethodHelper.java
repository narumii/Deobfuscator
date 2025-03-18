package uwu.narumi.deobfuscator.api.helper;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.objectweb.asm.tree.analysis.BasicValue;
import uwu.narumi.deobfuscator.api.asm.NamedOpcodes;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.JumpPredictingAnalyzer;
import org.objectweb.asm.tree.analysis.OriginalSourceInterpreter;
import org.objectweb.asm.tree.analysis.OriginalSourceValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
      for (int i = 0; i < consumer.getRequiredStackValuesCount(frame); i++) {
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
}
