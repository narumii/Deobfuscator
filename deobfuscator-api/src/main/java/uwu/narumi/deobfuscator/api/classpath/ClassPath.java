package uwu.narumi.deobfuscator.api.classpath;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.ClassReader;
import uwu.narumi.deobfuscator.api.context.DeobfuscatorOptions;
import uwu.narumi.deobfuscator.api.helper.ClassHelper;
import uwu.narumi.deobfuscator.api.helper.FileHelper;

/**
 * All sources for deobfuscated jar
 */
public class ClassPath {

  private static final Logger LOGGER = LogManager.getLogger(ClassPath.class);

  private final Map<String, byte[]> files = new ConcurrentHashMap<>();
  private final Map<String, byte[]> classes = new ConcurrentHashMap<>();

  private final int classWriterFlags;

  public ClassPath(int classWriterFlags) {
    this.classWriterFlags = classWriterFlags;
  }

  /**
   * Adds jar to classpath
   *
   * @param jarPath Jar path
   */
  public void addJar(@NotNull Path jarPath) {
    int prevSize = classes.size();

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

            classes.putIfAbsent(className, bytes);
          } catch (Exception e) {
            LOGGER.error("Could not load {} class from {} library", classPath, jarPath, e);
          }
        });

    LOGGER.info("Loaded {} classes from {}", classes.size() - prevSize, jarPath.getFileName());
  }

  /**
   * Adds {@link DeobfuscatorOptions.ExternalClass} to classpath
   *
   * @param externalClass External class
   */
  public void addExternalClass(DeobfuscatorOptions.ExternalClass externalClass) {
    try {
      byte[] classBytes = Files.readAllBytes(externalClass.path());
      String className = ClassHelper.loadClass(
          externalClass.relativePath(),
          classBytes,
          ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG,
          classWriterFlags
      ).name();

      // Add class to classpath
      classes.putIfAbsent(className, classBytes);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public Map<String, byte[]> getFiles() {
    return this.files;
  }

  public Map<String, byte[]> getClasses() {
    return this.classes;
  }
}
