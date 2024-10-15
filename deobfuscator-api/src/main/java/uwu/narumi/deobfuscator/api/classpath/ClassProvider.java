package uwu.narumi.deobfuscator.api.classpath;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.ClassNode;

import java.util.Collection;

public interface ClassProvider {
  /**
   * Gets class bytes by internal name
   *
   * @param name Internal name of class
   * @return Class bytes
   */
  byte @Nullable [] getClass(String name);

  /**
   * Gets file bytes by name
   *
   * @param path File path
   * @return File bytes
   */
  byte @Nullable [] getFile(String path);

  /**
   * Gets class node that holds only the class information. It is not guaranteed that the class holds code.
   *
   * @param name Internal name of class
   * @return Class node
   */
  @Nullable
  ClassNode getClassInfo(String name);

  /**
   * Gets all classes in the provider.
   */
  Collection<String> getLoadedClasses();
}
