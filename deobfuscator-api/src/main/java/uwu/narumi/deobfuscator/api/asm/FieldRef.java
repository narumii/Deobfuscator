package uwu.narumi.deobfuscator.api.asm;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;

/**
 * @param owner Class that owns this field
 * @param name Field's name
 * @param desc Field's descriptor
 */
public record FieldRef(String owner, String name, String desc) {
  public static FieldRef of(ClassNode classNode, FieldNode fieldNode) {
    return new FieldRef(classNode.name, fieldNode.name, fieldNode.desc);
  }

  public static FieldRef of(FieldInsnNode fieldInsn) {
    return new FieldRef(fieldInsn.owner, fieldInsn.name, fieldInsn.desc);
  }

  @Override
  public String toString() {
    return owner + "." + name + desc;
  }
}
