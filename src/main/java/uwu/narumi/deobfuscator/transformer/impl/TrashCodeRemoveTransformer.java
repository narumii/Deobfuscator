package uwu.narumi.deobfuscator.transformer.impl;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.exception.DeobfuscationException;
import uwu.narumi.deobfuscator.helper.ASMHelper;
import uwu.narumi.deobfuscator.transformer.Transformer;

public class TrashCodeRemoveTransformer implements Transformer {

  @Override
  public void transform(Deobfuscator deobfuscator) throws DeobfuscationException {
    deobfuscator.getClasses().stream()
        .flatMap(classNode -> classNode.methods.stream())
        .forEach(methodNode -> {
          ConcurrentMap<AbstractInsnNode, AbstractInsnNode> nodes = new ConcurrentHashMap<>();
          Arrays.stream(methodNode.instructions.toArray())
              .filter(node -> node.getOpcode() == IFEQ)
              .filter(node -> node.getPrevious().getOpcode() == INVOKEVIRTUAL)
              .filter(node -> ((MethodInsnNode) node.getPrevious()).name.equals("equals"))
              .filter(node -> ASMHelper.isString(node.getPrevious().getPrevious()))
              .filter(node -> ASMHelper.isString(node.getPrevious().getPrevious().getPrevious()))
              .filter(node -> !ASMHelper
                  .getStringFromNode(node.getPrevious().getPrevious().getPrevious())
                  .equals(ASMHelper.getStringFromNode(node.getPrevious().getPrevious())))
              .forEach(node -> {
                methodNode.instructions.remove(node.getPrevious().getPrevious().getPrevious());
                methodNode.instructions.remove(node.getPrevious().getPrevious());
                methodNode.instructions.remove(node.getPrevious());
                nodes.put(node, ((JumpInsnNode) node).label);
              });

          if (!nodes.isEmpty()) {
            Set<AbstractInsnNode> toRemove = new HashSet<>();
            nodes.forEach((start, end) -> {
              AbstractInsnNode current = start;
              do {
                toRemove.add(current);
              } while (current != null && ((current = current.getNext()) != end));
            });

            if (!toRemove.isEmpty()) {
              toRemove.stream().filter(Objects::nonNull)
                  .forEach(remove -> methodNode.instructions.remove(remove));
            }
            toRemove.clear();
          }
        });
  }

  @Override
  public int weight() {
    return 2;
  }

  @Override
  public String name() {
    return "Trash Code Remover";
  }
}
