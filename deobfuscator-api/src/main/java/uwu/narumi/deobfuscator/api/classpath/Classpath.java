package uwu.narumi.deobfuscator.api.classpath;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.ClassNode;
import uwu.narumi.deobfuscator.api.context.DeobfuscatorOptions;
import uwu.narumi.deobfuscator.api.helper.ClassHelper;
import uwu.narumi.deobfuscator.api.helper.FileHelper;

/**
 * Immutable classpath
 *
 * @param classesInfo Class nodes that hold only the class information, not code
 */
public record Classpath(
    Map<String, byte[]> rawClasses,
    Map<String, byte[]> files,
    Map<String, ClassNode> classesInfo
) {

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private static final Logger LOGGER = LogManager.getLogger();

    private final Map<String, byte[]> rawClasses = new HashMap<>();
    private final Map<String, byte[]> files = new HashMap<>();

    private final Map<String, ClassNode> classesInfo = new HashMap<>();

    private Builder() {
    }

    /**
     * Adds jar to classpath
     *
     * @param jarPath Jar path
     */
    @Contract("_ -> this")
    public Builder addJar(@NotNull Path jarPath) {
      FileHelper.loadFilesFromZip(jarPath, (classPath, bytes) -> {
        if (!ClassHelper.isClass(classPath, bytes)) {
          files.putIfAbsent(classPath, bytes);
          return;
        }

        try {
          ClassNode classNode = ClassHelper.loadUnknownClassInfo(bytes);
          String className = classNode.name;

          rawClasses.putIfAbsent(className, bytes);
          classesInfo.putIfAbsent(className, classNode);
        } catch (Exception e) {
          LOGGER.error("Could not load {} class from {} library", classPath, jarPath, e);
        }
      });

      return this;
    }

    /**
     * Adds {@link DeobfuscatorOptions.ExternalClass} to classpath
     *
     * @param externalClass External class
     */
    @Contract("_ -> this")
    public Builder addExternalClass(DeobfuscatorOptions.ExternalClass externalClass) {
      try {
        byte[] classBytes = Files.readAllBytes(externalClass.path());

        ClassNode classNode = ClassHelper.loadUnknownClassInfo(classBytes);

        String className = classNode.name;

        // Add class to classpath
        rawClasses.putIfAbsent(className, classBytes);
        classesInfo.putIfAbsent(className, classNode);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }

      return this;
    }

    /**
     * Adds another classpath to this classpath
     */
    @Contract("_ -> this")
    public Builder addClasspath(Classpath classpath) {
      this.rawClasses.putAll(classpath.rawClasses);
      this.files.putAll(classpath.files);
      this.classesInfo.putAll(classpath.classesInfo);

      return this;
    }

    public Classpath build() {
      return new Classpath(
          Collections.unmodifiableMap(rawClasses),
          Collections.unmodifiableMap(files),
          Collections.unmodifiableMap(classesInfo)
      );
    }
  }
}
