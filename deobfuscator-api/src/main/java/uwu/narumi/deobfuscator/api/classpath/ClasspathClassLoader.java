package uwu.narumi.deobfuscator.api.classpath;

/**
 * A {@link ClassLoader} that holds all classpath of the current deobfuscation context
 */
public class ClasspathClassLoader extends ClassLoader {
  private final Classpath classpath;

  public ClasspathClassLoader(Classpath classpath) {
    this.classpath = classpath;
  }

  @Override
  protected Class<?> findClass(String name) throws ClassNotFoundException {
    String internalName = name.replace('.', '/');

    // Find class in classPath
    byte[] classBytes = null;
    if (this.classpath.getClasses().containsKey(internalName)) {
      // Find in normal classes
      classBytes = this.classpath.getClasses().get(internalName);
    }

    if (classBytes != null) {
      // If found then return it
      return defineClass(name, classBytes, 0, classBytes.length);
    }
    return super.findClass(name);
  }
}
