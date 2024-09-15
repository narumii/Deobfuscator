package uwu.narumi.deobfuscator.api.classpath;

import org.objectweb.asm.ClassWriter;

public class ClassPathClassWriter extends ClassWriter {

  private final ClassPathClassLoader loader;

  public ClassPathClassWriter(int flags, ClassPath classPath) {
    super(flags);
    this.loader = new ClassPathClassLoader(classPath);
  }

  @Override
  protected ClassLoader getClassLoader() {
    return loader;
  }
}
