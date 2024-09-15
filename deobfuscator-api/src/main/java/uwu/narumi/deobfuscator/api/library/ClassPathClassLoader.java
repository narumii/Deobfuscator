package uwu.narumi.deobfuscator.api.library;

import java.util.Optional;

/**
 * A {@link ClassLoader} that holds all classpath of the current deobfuscation context
 */
public class ClassPathClassLoader extends ClassLoader {
  private final ClassPath classPath;

  public ClassPathClassLoader(ClassLoader parent, ClassPath classPath) {
    super(parent);
    this.classPath = classPath;
  }

  @Override
  protected Class<?> findClass(String name) throws ClassNotFoundException {
    String internalName = name.replace('.', '/');

    // Find class in classPath
    if (this.classPath.getClassFiles().containsKey(internalName)) {
      byte[] classBytes = this.classPath.getClassFiles().get(internalName);
      return defineClass(name, classBytes, 0, classBytes.length);
    }
    return super.findClass(name);
  }

  /**
   * Gets raw bytes of the class
   *
   * @param name Internal name of class
   * @return The bytes of class
   */
  public Optional<byte[]> fetchRaw(String name) {
    return Optional.ofNullable(classPath.getClassFiles().get(name));
  }

  public Optional<byte[]> fetchFile(String name) {
    return Optional.ofNullable(classPath.getFiles().get(name));
  }
}
