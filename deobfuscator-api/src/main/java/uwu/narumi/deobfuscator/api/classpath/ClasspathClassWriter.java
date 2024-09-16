package uwu.narumi.deobfuscator.api.classpath;

import org.objectweb.asm.ClassWriter;

public class ClasspathClassWriter extends ClassWriter {

  private final ClasspathClassLoader loader;

  public ClasspathClassWriter(int flags, Classpath classpath) {
    super(flags);
    this.loader = new ClasspathClassLoader(classpath);
  }

  @Override
  protected ClassLoader getClassLoader() {
    return loader;
  }
}
