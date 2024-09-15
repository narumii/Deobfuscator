package uwu.narumi.deobfuscator.api.context;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Immutable options for deobfuscator
 */
public record DeobfuscatorOptions(
    @Nullable Path inputJar,
    List<ExternalClass> classes,
    Set<Path> libraries,

    @Nullable Path outputJar,
    @Nullable Path outputDir,

    List<Supplier<Transformer>> transformers,

    int classReaderFlags,
    int classWriterFlags,

    boolean printStacktraces,
    boolean continueOnError,
    boolean verifyBytecode
) {
  public static DeobfuscatorOptions.Builder builder() {
    return new DeobfuscatorOptions.Builder();
  }

  public record ExternalClass(Path path, String relativePath) {
  }

  /**
   * Builder for {@link DeobfuscatorOptions}
   */
  public static class Builder {
    // Inputs
    @Nullable
    private Path inputJar = null;
    private final List<DeobfuscatorOptions.ExternalClass> classes = new ArrayList<>();
    private final Set<Path> libraries = new HashSet<>();

    // Outputs
    @Nullable
    private Path outputJar = null;
    @Nullable
    private Path outputDir = null;

    // Transformers
    private final List<Supplier<Transformer>> transformers = new ArrayList<>();

    // Other config
    private int classReaderFlags = ClassReader.SKIP_FRAMES;
    private int classWriterFlags = ClassWriter.COMPUTE_FRAMES;

    private boolean printStacktraces = true;
    private boolean continueOnError = false;
    private boolean verifyBytecode = false;

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

    @Contract("_ -> this")
    public DeobfuscatorOptions.Builder libraries(Path... paths) {
      this.libraries.addAll(List.of(paths));
      return this;
    }

    /**
     * Add external class to deobfuscate
     *
     * @param path         Path to external class
     * @param relativePath Relative path for saving purposes
     */
    @Contract("_,_ -> this")
    public DeobfuscatorOptions.Builder clazz(Path path, String relativePath) {
      this.classes.add(new DeobfuscatorOptions.ExternalClass(path, relativePath));
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
     * Flags for {@link ClassReader}
     */
    @Contract("_ -> this")
    public DeobfuscatorOptions.Builder classReaderFlags(int classReaderFlags) {
      this.classReaderFlags = classReaderFlags;
      return this;
    }

    /**
     * Flags for {@link ClassWriter}. When you set it to {@code 0} you will disable checking the validity
     * of the bytecode. Although this is not recommended.
     */
    @Contract("_ -> this")
    public DeobfuscatorOptions.Builder classWriterFlags(int classWriterFlags) {
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
     * Build immutable {@link DeobfuscatorOptions} with options verification
     */
    public DeobfuscatorOptions build() {
      // Verify some options
      if (this.inputJar == null && this.classes.isEmpty()) {
        throw new IllegalStateException("No input files provided");
      }
      if (this.outputJar == null && this.outputDir == null) {
        throw new IllegalStateException("No output file or directory provided");
      }
      if (this.outputJar != null && this.outputDir != null) {
        throw new IllegalStateException("Output jar and output dir cannot be set at the same time");
      }

      return new DeobfuscatorOptions(
          // Input
          inputJar,
          classes,
          libraries,
          // Output
          outputJar,
          outputDir,
          // Transformers
          transformers,
          // Flags
          classReaderFlags,
          classWriterFlags,
          // Other config
          printStacktraces,
          continueOnError,
          verifyBytecode
      );
    }
  }
}
