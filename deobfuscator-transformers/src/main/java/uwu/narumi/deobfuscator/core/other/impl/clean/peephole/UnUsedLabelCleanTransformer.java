package uwu.narumi.deobfuscator.core.other.impl.clean.peephole;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;
import org.objectweb.asm.tree.*;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

public class UnUsedLabelCleanTransformer extends Transformer {

  @Override
  protected void transform() throws Exception {
    scopedClasses().stream()
        .flatMap(classWrapper -> classWrapper.methods().stream())
        .forEach(
            methodNode -> {
              Set<LabelNode> labelNodes = new HashSet<>();

              if (methodNode.tryCatchBlocks != null)
                labelNodes.addAll(
                    methodNode.tryCatchBlocks.stream()
                        .flatMap(tbce -> Stream.of(tbce.end, tbce.start, tbce.handler))
                        .toList());

              if (methodNode.localVariables != null)
                labelNodes.addAll(
                    methodNode.localVariables.stream()
                        .flatMap(localVariable -> Stream.of(localVariable.end, localVariable.start))
                        .toList());

              Arrays.stream(methodNode.instructions.toArray())
                  .forEach(
                      node -> {
                        if (node instanceof JumpInsnNode) {
                          labelNodes.add(((JumpInsnNode) node).label);
                        } else if (node instanceof LookupSwitchInsnNode lookupSwitchInsnNode) {
                          labelNodes.addAll(lookupSwitchInsnNode.labels);
                          labelNodes.add(lookupSwitchInsnNode.dflt);
                        } else if (node instanceof TableSwitchInsnNode tableSwitchInsnNode) {
                          labelNodes.addAll(tableSwitchInsnNode.labels);
                          labelNodes.add(tableSwitchInsnNode.dflt);
                        } else if (node instanceof LineNumberNode) {
                          labelNodes.add(((LineNumberNode) node).start);
                        }
                      });

              // Remove label nodes that are not used
              Arrays.stream(methodNode.instructions.toArray())
                  .filter(node -> node instanceof LabelNode)
                  .map(LabelNode.class::cast)
                  .filter(node -> !labelNodes.contains(node))
                  .forEach(insn -> {
                    methodNode.instructions.remove(insn);
                    this.markChange();
                  });
            });
  }
}
