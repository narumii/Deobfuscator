package uwu.narumi.deobfuscator.api.classpath;

/**
 * A {@link ClassLoader} that holds all classpath of the current deobfuscation context
 */
public class ClassPathClassLoader extends ClassLoader {
  private final ClassPath classPath;

  public ClassPathClassLoader(ClassPath classPath) {
    this.classPath = classPath;
  }

  @Override
  protected Class<?> findClass(String name) throws ClassNotFoundException {
    String internalName = name.replace('.', '/');

    // Find class in classPath
    byte[] classBytes = null;
    if (this.classPath.getClasses().containsKey(internalName)) {
      // Find in normal classes
      classBytes = this.classPath.getClasses().get(internalName);
    }

    if (classBytes != null) {
      // If found then return it
      return defineClass(name, classBytes, 0, classBytes.length);
    }
    return super.findClass(name);
  }
}
