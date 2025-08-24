package uwu.narumi.deobfuscator.api.context;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassWriter;
import uwu.narumi.deobfuscator.api.environment.JavaEnv;
import uwu.narumi.deobfuscator.api.environment.JavaInstall;
import uwu.narumi.deobfuscator.api.execution.SandBox;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Immutable options for deobfuscator
 */
public record DeobfuscatorOptions(
    @Nullable Path inputJar,
    List<ExternalFile> externalFiles,
    Set<Path> libraries,
    @Nullable Path rtJarPath,

    @Nullable Path outputJar,
    @Nullable Path outputDir,

    List<Supplier<Transformer>> transformers,

    @MagicConstant(flagsFromClass = ClassWriter.class) int classWriterFlags,

    boolean printStacktraces,
    boolean continueOnError,
    boolean verifyBytecode,
    boolean skipFiles
) {
  public static DeobfuscatorOptions.Builder builder() {
    return new DeobfuscatorOptions.Builder();
  }

  /**
   * @param path Path to the raw file
   * @param pathInJar Relative path to file as if it were in .jar
   */
  public record ExternalFile(Path path, String pathInJar) {
  }

  /**
   * Builder for {@link DeobfuscatorOptions}
   */
  public static class Builder {
    private static final Logger LOGGER = LogManager.getLogger();

    // Inputs
    @Nullable
    private Path inputJar = null;
    private final List<ExternalFile> externalFiles = new ArrayList<>();
    private final Set<Path> libraries = new HashSet<>();
    @Nullable
    private Path rtJarPath = null;

    // Outputs
    @Nullable
    private Path outputJar = null;
    @Nullable
    private Path outputDir = null;

    // Transformers
    private final List<Supplier<Transformer>> transformers = new ArrayList<>();

    // Other config
    @MagicConstant(flagsFromClass = ClassWriter.class)
    private int classWriterFlags = ClassWriter.COMPUTE_FRAMES;

    private boolean printStacktraces = true;
    private boolean continueOnError = false;
    private boolean verifyBytecode = false;
    private boolean skipFiles = false;

    private Builder() {
    }

    /**
     * Your input jar file
     */
    @Contract("_ -> this")
    public DeobfuscatorOptions.Builder inputJar(@Nullable Path inputJar) {
      this.inputJar = inputJar;
      if (this.inputJar != null) {
        // Auto fill output jar
        if (this.outputJar == null) {
          String fullName = inputJar.getFileName().toString();
          int dot = fullName.lastIndexOf('.');

          this.outputJar = inputJar.getParent()
              .resolve(dot == -1 ? fullName + "-out" : fullName.substring(0, dot) + "-out" + fullName.substring(dot));
        }
      }
      return this;
    }

    /**
     * Add an external file to the deobfuscation context. You can add raw .class files or files that would be in .jar
     *
     * @param path Path to an external file
     * @param pathInJar Relative path to file if it were in .jar
     */
    @Contract("_,_ -> this")
    public DeobfuscatorOptions.Builder externalFile(Path path, String pathInJar) {
      this.externalFiles.add(new ExternalFile(path, pathInJar));
      return this;
    }

    /**
     * Adds all files from the directory to the deobfuscation context
     *
     * @param path Path to the directory
     */
    @Contract("_ -> this")
    public DeobfuscatorOptions.Builder inputDir(Path path) {
      try {
        Files.walkFileTree(path, new SimpleFileVisitor<>() {
          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            String pathInJar = path.relativize(file).toString();
            externalFile(file, pathInJar);
            return FileVisitResult.CONTINUE;
          }
        });
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      return this;
    }

    /**
     * Add libraries to the classpath. You can pass here files or directories.
     *
     * @param paths Paths to libraries
     */
    @Contract("_ -> this")
    public DeobfuscatorOptions.Builder libraries(Path... paths) {
      for (Path path : paths) {
        if (Files.isDirectory(path)) {
          try {
            // Walk through directory
            Files.walkFileTree(path, new SimpleFileVisitor<>() {
              @Override
              public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                libraries.add(file);
                return FileVisitResult.CONTINUE;
              }
            });
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        } else {
          this.libraries.add(path);
        }
      }
      return this;
    }

    /**
     * Path to rt.jar from Java 8 binaries. Required for sandbox to work properly.
     * Examples:
     * - Oracle JDK 8: <code>C:/Program Files/Java/jdk1.8.0_202/jre/lib/rt.jar</code>
     * - Eclipse Adoptium JDK 8: <code>C:/Program Files/Eclipse Adoptium/jdk-8.0.462.8-hotspot/jre/lib/rt.jar</code>
     */
    @Contract("_ -> this")
    public DeobfuscatorOptions.Builder rtJarPath(@Nullable Path rtJarPath) {
      this.rtJarPath = rtJarPath;
      return this;
    }

    /**
     * Output jar for deobfuscated classes. Automatically filled when input jar is set
     */
    @Contract("_ -> this")
    public DeobfuscatorOptions.Builder outputJar(@Nullable Path outputJar) {
      this.outputJar = outputJar;
      return this;
    }

    /**
     * Set output dir it if you want to output raw compiled classes instead of jar file
     */
    @Contract("_ -> this")
    public DeobfuscatorOptions.Builder outputDir(@Nullable Path outputDir) {
      this.outputDir = outputDir;
      return this;
    }

    /**
     * Transformers to run. You need to specify them in lambda form:
     * <pre>
     * {@code
     * () -> new MyTransformer(true, false),
     * () -> new AnotherTransformer(),
     * () -> new SuperTransformer()
     * }
     * </pre>
     *
     * We can push it further, and we can replace lambdas with no arguments with method references:
     * <pre>
     * {@code
     * () -> new MyTransformer(true, false),
     * AnotherTransformer::new,
     * SuperTransformer::new
     * }
     * </pre>
     */
    @SafeVarargs
    @Contract("_ -> this")
    public final DeobfuscatorOptions.Builder transformers(Supplier<Transformer>... transformers) {
      this.transformers.addAll(List.of(transformers));
      return this;
    }

    /**
     * Flags for {@link ClassWriter}.
     * <ul>
     * <li><code>0</code> - Deobfuscated jar can't be run</li>
     * <li>{@link ClassWriter#COMPUTE_FRAMES} - Makes a runnable deobfuscated jar</li>
     * </ul>
     */
    @Contract("_ -> this")
    public DeobfuscatorOptions.Builder classWriterFlags(@MagicConstant(flagsFromClass = ClassWriter.class) int classWriterFlags) {
      this.classWriterFlags = classWriterFlags;
      return this;
    }

    /**
     * Disables stacktraces logging
     */
    @Contract(" -> this")
    public DeobfuscatorOptions.Builder noStacktraces() {
      this.printStacktraces = false;
      return this;
    }

    /**
     * Continue deobfuscation even if errors occur
     */
    @Contract(" -> this")
    public DeobfuscatorOptions.Builder continueOnError() {
      this.continueOnError = true;
      return this;
    }

    /**
     * Verify bytecode after each transformer run. Useful when debugging which
     * transformer is causing issues (aka broke bytecode)
     */
    @Contract(" -> this")
    public DeobfuscatorOptions.Builder verifyBytecode() {
      this.verifyBytecode = true;
      return this;
    }

    /**
     * Skips files during saving.
     */
    @Contract(" -> this")
    public DeobfuscatorOptions.Builder skipFiles() {
      this.skipFiles = true;
      return this;
    }

    /**
     * Try to find rt.jar from Java 8 installation
     */
    @Nullable
    private Path findRtJarPath() {
      String userDefinedRtJarPath = System.getProperty("rtJarPath");
      if (userDefinedRtJarPath != null) {
        return Path.of(userDefinedRtJarPath);
      }

      Optional<JavaInstall> javaInstall = JavaEnv.getJavaInstalls().stream()
          .filter(javaInstall1 -> javaInstall1.version() == 8)
          .findFirst();

      if (javaInstall.isPresent()) {
        JavaInstall install = javaInstall.get();
        Path possibleRtJarPath = install.javaExecutable().getParent().getParent().resolve("jre").resolve("lib").resolve("rt.jar");
        if (Files.exists(possibleRtJarPath)) {
          return possibleRtJarPath;
        }
      }
      return null;
    }

    /**
     * Build immutable {@link DeobfuscatorOptions} with options verification
     */
    public DeobfuscatorOptions build() {
      // Verify some options
      if (this.inputJar == null && this.externalFiles.isEmpty()) {
        throw new IllegalStateException("No input files provided");
      }
      if (this.outputJar == null && this.outputDir == null) {
        throw new IllegalStateException("No output file or directory provided");
      }
      if (this.outputJar != null && this.outputDir != null) {
        throw new IllegalStateException("Output jar and output dir cannot be set at the same time");
      }
      // Try to auto-detect rt.jar path
      if (this.rtJarPath == null) {
        Path rtJar = findRtJarPath();
        if (rtJar != null) {
          LOGGER.info("Auto-detected rt.jar path: {}", rtJar);
          this.rtJarPath = rtJar;
        } else {
          LOGGER.error("Failed to auto-detect rt.jar path. Perhaps you don't have Java 8 installed?");
        }
      }

      return new DeobfuscatorOptions(
          // Input
          inputJar,
          externalFiles,
          libraries,
          rtJarPath,
          // Output
          outputJar,
          outputDir,
          // Transformers
          transformers,
          // Flags
          classWriterFlags,
          // Other config
          printStacktraces,
          continueOnError,
          verifyBytecode,
          skipFiles
      );
    }
  }
}
