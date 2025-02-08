package uwu.narumi.deobfuscator.api.asm;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * @param owner Class that owns this method
 * @param name Method's name
 * @param desc Method's descriptor
 */
public record MethodRef(String owner, String name, String desc) {
  public static MethodRef of(ClassNode classNode, MethodNode methodNode) {
    return new MethodRef(classNode.name, methodNode.name, methodNode.desc);
  }

  public static MethodRef of(MethodInsnNode methodInsn) {
    return new MethodRef(methodInsn.owner, methodInsn.name, methodInsn.desc);
  }

  @Override
  public String toString() {
    return owner + "." + name + desc;
  }
}
