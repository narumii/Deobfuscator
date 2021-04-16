package uwu.narumi.deobfuscator.transformer.impl;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.exception.DeobfuscationException;
import uwu.narumi.deobfuscator.helper.ASMHelper;
import uwu.narumi.deobfuscator.transformer.Transformer;

public class AntiDebugRemoveTransformer implements Transformer {

  @Override
  public void transform(Deobfuscator deobfuscator) throws DeobfuscationException {
    deobfuscator.getClasses().forEach(classNode -> {
      Set<MethodNode> toRemove = new HashSet<>();
      classNode.methods.forEach(methodNode -> Arrays.stream(methodNode.instructions.toArray())
          .filter(node -> node instanceof MethodInsnNode)
          .filter(node -> node.getNext() instanceof MethodInsnNode)
          .filter(node -> ((MethodInsnNode) node).owner
              .equals("java/lang/management/ManagementFactory"))
          .filter(node -> ((MethodInsnNode) node).name.equals("getRuntimeMXBean"))
          .filter(node -> ((MethodInsnNode) node.getNext()).owner
              .equals("java/lang/management/RuntimeMXBean"))
          .filter(node -> ((MethodInsnNode) node.getNext()).name.equals("getInputArguments"))
          .findAny()
          .ifPresent(node -> toRemove.add(methodNode)));

      ASMHelper.findCLInit(classNode).ifPresent(
          clInit -> toRemove.forEach(method -> Arrays.stream(clInit.instructions.toArray())
              .filter(node -> node instanceof MethodInsnNode)
              .filter(node -> ((MethodInsnNode) node).name.equals(method.name))
              .filter(node -> ((MethodInsnNode) node).desc.equals(method.desc))
              .forEach(node -> clInit.instructions.remove(node))));

      classNode.methods.removeAll(toRemove);
      toRemove.clear();
    });
  }

  @Override
  public int weight() {
    return 5;
  }

  @Override
  public String name() {
    return "AntiDebug Remover";
  }
}
