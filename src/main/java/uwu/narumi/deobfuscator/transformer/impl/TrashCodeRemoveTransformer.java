package uwu.narumi.deobfuscator.transformer.impl;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.exception.DeobfuscationException;
import uwu.narumi.deobfuscator.helper.ASMHelper;
import uwu.narumi.deobfuscator.transformer.Transformer;

public class TrashCodeRemoveTransformer implements Transformer {

  //Smiesznie kiedy nauczysz sie czegos w koncu?
  //Bo to juz sie nudne robi
  @Override
  public void transform(Deobfuscator deobfuscator) throws DeobfuscationException {
    deobfuscator.getClasses().stream()
        .flatMap(classNode -> classNode.methods.stream())
        .forEach(methodNode -> {
          ConcurrentMap<AbstractInsnNode, AbstractInsnNode> nodes = new ConcurrentHashMap<>();

          Arrays.stream(methodNode.instructions.toArray())
              .filter(node -> node instanceof JumpInsnNode)
              .filter(node -> node.getPrevious().getPrevious() instanceof LdcInsnNode)
              .forEach(node -> {
                Object jumpType = ((LdcInsnNode) node.getPrevious().getPrevious()).cst;
                LabelNode labelNode = ((JumpInsnNode) node).label;
                int type = node.getOpcode();

                if (jumpType instanceof String && ASMHelper
                    .isString(node.getPrevious().getPrevious().getPrevious()) && node
                    .getPrevious() instanceof MethodInsnNode && ((MethodInsnNode) (node
                    .getPrevious())).name.equals("equals")) {
                  String string = ASMHelper
                      .getStringFromNode(node.getPrevious().getPrevious().getPrevious());
                  if ((type == IFEQ && !jumpType.equals(string)) || (type == IFNE && jumpType
                      .equals(string))) {
                    methodNode.instructions
                        .remove(node.getPrevious().getPrevious().getPrevious()); //string
                    methodNode.instructions.remove(node.getPrevious().getPrevious()); //string
                    methodNode.instructions.remove(node.getPrevious()); //invoke
                    nodes.put(node, labelNode);
                  }
                } else if ((jumpType instanceof Double || jumpType instanceof Float) && node
                    .getPrevious() instanceof MethodInsnNode && (
                    ((MethodInsnNode) node.getPrevious()).name.equals("isInfinite")
                        || ((MethodInsnNode) node.getPrevious()).name.equals("isNaN"))) {
                  String methodName = ((MethodInsnNode) node.getPrevious()).name;

                  if (!is((Number) jumpType, methodName)) {
                    methodNode.instructions.remove(node.getPrevious().getPrevious()); //ldc
                    methodNode.instructions.remove(node.getPrevious()); //invoke
                    nodes.put(node, labelNode);
                  }
                }
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

  private boolean is(Number number, String method) {
    if (method.equals("isNaN")) {
      return (number instanceof Float ? Float.isNaN(number.floatValue())
          : Double.isNaN(number.doubleValue()));
    } else if (method.equals("isInfinite")) {
      return (number instanceof Float ? Float.isInfinite(number.floatValue())
          : Double.isInfinite(number.doubleValue()));
    }
    return true;
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
