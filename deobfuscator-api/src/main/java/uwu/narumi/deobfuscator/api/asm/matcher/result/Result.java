package uwu.narumi.deobfuscator.api.asm.matcher.result;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class Result {

  private final AbstractInsnNode start;
  private final AbstractInsnNode end;
  private final AbstractInsnNode[] nodes;

  private final MethodNode methodNode;

  public Result(
      AbstractInsnNode start,
      AbstractInsnNode end,
      MethodNode methodNode,
      AbstractInsnNode... nodes) {
    this.start = start;
    this.end = end;
    this.methodNode = methodNode;
    this.nodes = nodes;
  }

  public void set(int position, AbstractInsnNode node) {
    if (methodNode == null || position < 0 || position >= nodes.length) return;

    methodNode.instructions.set(nodes[position], node);
  }

  public void remove() {
    remove(0, nodes.length);
  }

  public void remove(int startingPosition) {
    remove(startingPosition, nodes.length);
  }

  public void remove(int startingPosition, int endingPosition) {
    if (methodNode == null || startingPosition > endingPosition) return;

    for (int i = Math.max(startingPosition, 0); i < Math.min(endingPosition, nodes.length); i++) {
      methodNode.instructions.remove(nodes[i]);
    }
  }

  public <T extends AbstractInsnNode> T previous(int position) {
    AbstractInsnNode current = start;
    for (int i = 0; i < Math.abs(position); i++) current = current.getPrevious();

    return (T) current;
  }

  public <T extends AbstractInsnNode> T next(int position) {
    AbstractInsnNode current = start;
    for (int i = 0; i < Math.abs(position); i++) current = current.getNext();

    return (T) current;
  }

  public <T extends AbstractInsnNode> T get(int position) {
    if (nodes.length <= position || position < 0) throw new ArrayIndexOutOfBoundsException();

    return (T) nodes[position];
  }

  public <T extends AbstractInsnNode> T start() {
    return (T) start;
  }

  public <T extends AbstractInsnNode> T end() {
    return (T) end;
  }

  public AbstractInsnNode[] nodes() {
    return nodes;
  }

  public MethodNode getMethodNode() {
    return methodNode;
  }
}
