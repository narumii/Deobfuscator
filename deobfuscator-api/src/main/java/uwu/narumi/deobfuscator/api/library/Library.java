package uwu.narumi.deobfuscator.api.library;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import me.coley.cafedude.InvalidClassException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import uwu.narumi.deobfuscator.api.helper.ClassHelper;
import uwu.narumi.deobfuscator.api.helper.FileHelper;

public class Library {

  private static final Logger LOGGER = LogManager.getLogger(Library.class);

  private final Map<String, byte[]> files = new ConcurrentHashMap<>();
  private final Map<String, byte[]> classFiles = new ConcurrentHashMap<>();
  private final Path path;

  public Library(Path path) {
    this.path = path;
    FileHelper.loadFilesFromZip(
        path,
        (name, bytes) -> {
          if (!ClassHelper.isClass(name, bytes)) {
            files.computeIfAbsent(name, ignored -> bytes);
            return;
          }

          try {
            ClassHelper.loadClass(
                    bytes, ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG)
                .ifPresent(
                    classWrapper ->
                        classFiles.computeIfAbsent(classWrapper.name(), ignored -> bytes));
          } catch (InvalidClassException ignored) {
          }
        });

    LOGGER.info("Loaded {} classes from {}", classFiles.size(), path.getFileName());
  }

  public Map<String, byte[]> getFiles() {
    return files;
  }

  public Map<String, byte[]> getClassFiles() {
    return classFiles;
  }

  public Path getPath() {
    return path;
  }
}
