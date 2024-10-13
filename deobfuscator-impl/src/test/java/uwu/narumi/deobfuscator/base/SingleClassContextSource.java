package uwu.narumi.deobfuscator.base;

import java.io.FileInputStream;
import java.nio.file.Path;
import org.jetbrains.java.decompiler.main.extern.IContextSource;
import org.jetbrains.java.decompiler.main.extern.IResultSaver;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class SingleClassContextSource implements IContextSource {
  private final Path file;
  private final String pathInJar;

  /**
   * @param file Path to .class file
   * @param pathInJar Relative path to .class file as if it were in .jar
   */
  public SingleClassContextSource(Path file, String pathInJar) {
    this.file = file;
    this.pathInJar = pathInJar;
  }

  @Override
  public String getName() {
    return "file " + this.file;
  }

  @Override
  public Entries getEntries() {
    return new Entries(List.of(Entry.atBase(this.pathInJar)), List.of(), List.of());
  }

  @Override
  public InputStream getInputStream(String resource) throws IOException {
    return new FileInputStream(this.file.toFile());
  }

  @Override
  public IOutputSink createOutputSink(IResultSaver saver) {
    return new IOutputSink() {
      @Override
      public void begin() {

      }

      @Override
      public void acceptClass(String qualifiedName, String fileName, String content, int[] mapping) {
        saver.saveClassFile("", qualifiedName, pathInJar, content, mapping);
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
