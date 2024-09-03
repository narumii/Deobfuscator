package uwu.narumi.deobfuscator.base;

import java.nio.file.Files;
import java.nio.file.Path;
import org.jetbrains.java.decompiler.main.extern.IContextSource;
import org.jetbrains.java.decompiler.main.extern.IResultSaver;
import org.objectweb.asm.ClassReader;
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.helper.ClassHelper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class SingleClassContextSource implements IContextSource {
  private final Path file;
  private final String relativePath;
  private final String qualifiedName;
  private final byte[] contents;

  public SingleClassContextSource(Path file, String relativePath) {
    this.file = file;
    this.relativePath = relativePath;
    try {
      // Get qualified name
      this.contents = Files.readAllBytes(file);
      ClassWrapper classWrapper = ClassHelper.loadClass(relativePath, this.contents, ClassReader.SKIP_FRAMES, 0);
      this.qualifiedName = classWrapper.name();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public String getName() {
    return "file " + this.file;
  }

  @Override
  public Entries getEntries() {
    return new Entries(List.of(Entry.atBase(this.qualifiedName)), List.of(), List.of());
  }

  @Override
  public InputStream getInputStream(String resource) throws IOException {
    return new ByteArrayInputStream(this.contents);
  }

  @Override
  public IOutputSink createOutputSink(IResultSaver saver) {
    return new IOutputSink() {
      @Override
      public void begin() {

      }

      @Override
      public void acceptClass(String qualifiedName, String fileName, String content, int[] mapping) {
        saver.saveClassFile("", qualifiedName, relativePath, content, mapping);
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
