package uwu.narumi.deobfuscator.transformer.impl;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.exception.DeobfuscationException;
import uwu.narumi.deobfuscator.transformer.Transformer;

public class TrashLabelsRemoveTransformer implements Transformer {

  @Override
  public void transform(Deobfuscator deobfuscator) throws DeobfuscationException {
    deobfuscator.getClasses().stream()
        .flatMap(classNode -> classNode.methods.stream())
        .forEach(methodNode -> {
          Set<LabelNode> usedLabels = new HashSet<>();
          for (AbstractInsnNode node : methodNode.instructions.toArray()) {
            if (node instanceof JumpInsnNode) {
              usedLabels.add(((JumpInsnNode) node).label);
            } else if (node instanceof LookupSwitchInsnNode) {
              LookupSwitchInsnNode lookupNode = (LookupSwitchInsnNode) node;
              usedLabels.addAll(lookupNode.labels);
              usedLabels.add(lookupNode.dflt);
            } else if (node instanceof TableSwitchInsnNode) {
              TableSwitchInsnNode switchNode = (TableSwitchInsnNode) node;
              usedLabels.addAll(switchNode.labels);
              usedLabels.add(switchNode.dflt);
            } else if (node instanceof LineNumberNode) {
              usedLabels.add(((LineNumberNode) node).start);
            }
          }

          if (methodNode.localVariables != null && !methodNode.localVariables.isEmpty()) {
            methodNode.localVariables.forEach(node -> {
              usedLabels.add(node.start);
              usedLabels.add(node.end);
            });
          }

          if (methodNode.tryCatchBlocks != null && !methodNode.tryCatchBlocks.isEmpty()) {
            methodNode.tryCatchBlocks.forEach(node -> {
              usedLabels.add(node.start);
              usedLabels.add(node.end);
            });
          }

          Arrays.stream(methodNode.instructions.toArray())
              .filter(node -> node instanceof LabelNode)
              .filter(node -> !usedLabels.contains(node))
              .forEach(node -> methodNode.instructions.remove(node));
          usedLabels.clear();
        });
  }

  @Override
  public int weight() {
    return 3;
  }

  @Override
  public String name() {
    return "Trash Labels Remover";
  }
}
