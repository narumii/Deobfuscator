package uwu.narumi.deobfuscator.api.library;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import uwu.narumi.deobfuscator.api.context.DeobfuscatorOptions;
import uwu.narumi.deobfuscator.api.helper.ClassHelper;
import uwu.narumi.deobfuscator.api.helper.FileHelper;

public class Library {

  private static final Logger LOGGER = LogManager.getLogger(Library.class);

  private final Map<String, byte[]> files = new ConcurrentHashMap<>();
  private final Map<String, byte[]> classFiles = new ConcurrentHashMap<>();
  @Nullable
  private final Path path;

  /**
   * Create {@link Library} from jar
   *
   * @param jarPath Jar path
   * @param classWriterFlags Class writer flags
   */
  public Library(@NotNull Path jarPath, int classWriterFlags) {
    this.path = jarPath;
    FileHelper.loadFilesFromZip(
        jarPath,
        (classPath, bytes) -> {
          if (!ClassHelper.isClass(classPath, bytes)) {
            files.putIfAbsent(classPath, bytes);
            return;
          }

          try {
            String className = ClassHelper.loadClass(
                classPath,
                bytes,
                ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG,
                classWriterFlags
            ).name();

            classFiles.putIfAbsent(className, bytes);
          } catch (Exception e) {
            LOGGER.error("Could not load {} class from {} library", classPath, jarPath, e);
          }
        });

    LOGGER.info("Loaded {} classes from {}", classFiles.size(), jarPath.getFileName());
  }

  /**
   * Create {@link Library} from external classes
   *
   * @param externalClasses External classes
   * @param classWriterFlags Class writer flags
   */
  public Library(List<DeobfuscatorOptions.ExternalClass> externalClasses, int classWriterFlags) {
    this.path = null;
    for (DeobfuscatorOptions.ExternalClass externalClass : externalClasses) {
      try {
        byte[] classBytes = Files.readAllBytes(externalClass.path());
        String className = ClassHelper.loadClass(
            externalClass.relativePath(),
            classBytes,
            ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG,
            classWriterFlags
        ).name();

        // Add class to lib
        classFiles.putIfAbsent(className, classBytes);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  public Map<String, byte[]> getFiles() {
    return files;
  }

  public Map<String, byte[]> getClassFiles() {
    return classFiles;
  }

  @Nullable
  public Path getPath() {
    return path;
  }
}
