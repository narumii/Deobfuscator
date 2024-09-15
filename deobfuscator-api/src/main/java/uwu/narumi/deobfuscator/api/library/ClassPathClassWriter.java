package uwu.narumi.deobfuscator.api.library;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

public class ClassPathClassWriter extends ClassWriter {

  private final ClassPathClassLoader loader;

  public ClassPathClassWriter(int flags, ClassPathClassLoader loader) {
    super(flags);
    this.loader = loader;
  }

  public ClassPathClassWriter(ClassReader classReader, int flags, ClassPathClassLoader loader) {
    super(classReader, flags);
    this.loader = loader;
  }

  @Override
  protected ClassLoader getClassLoader() {
    return loader;
  }
}
