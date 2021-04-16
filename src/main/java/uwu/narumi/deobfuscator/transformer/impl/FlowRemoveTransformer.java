package uwu.narumi.deobfuscator.transformer.impl;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.JumpInsnNode;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.exception.DeobfuscationException;
import uwu.narumi.deobfuscator.transformer.Transformer;

public class FlowRemoveTransformer implements Transformer {

  @Override
  public void transform(Deobfuscator deobfuscator) throws DeobfuscationException {
    deobfuscator.getClasses().forEach(classNode -> {
      Set<FieldInsnNode> fields = new HashSet<>();
      Set<FieldNode> toRemove = new HashSet<>();
      classNode.methods.forEach(methodNode -> Arrays.stream(methodNode.instructions.toArray())
          .filter(node -> node.getOpcode() == IFEQ)
          .filter(node -> node.getPrevious().getOpcode() == GETSTATIC)
          .filter(node -> node.getNext().getOpcode() == ACONST_NULL)
          .filter(node -> node.getNext().getNext().getOpcode() == ATHROW)
          .forEach(node -> {
            fields.add((FieldInsnNode) node.getPrevious());

            methodNode.instructions.remove(node.getPrevious()); //GETSTATIC
            methodNode.instructions.remove(node.getNext().getNext()); //THROW
            methodNode.instructions.remove(node.getNext()); //NULL

            methodNode.instructions.set(node, new JumpInsnNode(GOTO, ((JumpInsnNode) node).label));
          }));

      fields.forEach(fieldInsn -> classNode.fields.stream()
          .filter(fieldNode -> fieldNode.desc.equals(fieldInsn.desc))
          .filter(fieldNode -> fieldNode.name.equals(fieldInsn.name))
          .forEach(toRemove::add));

      classNode.fields.removeAll(toRemove);
      toRemove.clear();
      fields.clear();
    });
  }

  @Override
  public int weight() {
    return 4;
  }

  @Override
  public String name() {
    return "Flow Remover";
  }
}
