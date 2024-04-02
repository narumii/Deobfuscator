package uwu.narumi.deobfuscator.api.library;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

public class LibraryClassWriter extends ClassWriter {

  private final LibraryClassLoader loader;

  public LibraryClassWriter(int flags, LibraryClassLoader loader) {
    super(flags);
    this.loader = loader;
  }

  public LibraryClassWriter(ClassReader classReader, int flags, LibraryClassLoader loader) {
    super(classReader, flags);
    this.loader = loader;
  }

  @Override
  protected ClassLoader getClassLoader() {
    return loader;
  }
}
