package uwu.narumii.deobfuscator.base;

import org.jetbrains.java.decompiler.main.extern.IContextSource;
import org.jetbrains.java.decompiler.main.extern.IResultSaver;
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.context.Context;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class ClassWrapperContextSource implements IContextSource {
  private final ClassWrapper classWrapper;
  private final Context context;

  public ClassWrapperContextSource(ClassWrapper classWrapper, Context context) {
    this.classWrapper = classWrapper;
    this.context = context;
  }

  @Override
  public String getName() {
    return this.classWrapper.name();
  }

  @Override
  public Entries getEntries() {
    String qualifiedName = this.classWrapper.name().replace('/', '.');
    return new Entries(
        List.of(
            Entry.atBase(qualifiedName)
        ),
        List.of(),
        List.of()
    );
  }

  @Override
  public InputStream getInputStream(String resource) throws IOException {
    return new ByteArrayInputStream(this.classWrapper.compileToBytes(this.context));
  }

  @Override
  public IOutputSink createOutputSink(IResultSaver saver) {
    return new IOutputSink() {
      @Override
      public void begin() {

      }

      @Override
      public void acceptClass(String qualifiedName, String fileName, String content, int[] mapping) {
        // Call IResultSaver
        saver.saveClassFile(classWrapper.getPath(), qualifiedName, classWrapper.name(), content, mapping);
      }

      @Override
      public void acceptDirectory(String directory) {

      }

      @Override
      public void acceptOther(String path) {

      }

      @Override
      public void close() throws IOException {

      }
    };
  }
}
