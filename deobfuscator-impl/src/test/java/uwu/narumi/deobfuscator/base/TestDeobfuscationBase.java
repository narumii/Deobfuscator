package uwu.narumi.deobfuscator.base;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.java.decompiler.api.Decompiler;
import org.jetbrains.java.decompiler.main.extern.IContextSource;
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.Timeout;
import org.opentest4j.TestAbortedException;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.api.context.DeobfuscatorOptions;
import uwu.narumi.deobfuscator.api.helper.FileHelper;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

@Timeout(60)
public abstract class TestDeobfuscationBase {
  public static final Path TEST_DATA_PATH = Path.of("..", "testData");
  public static final Path COMPILED_CLASSES_PATH = TEST_DATA_PATH.resolve("compiled");
  public static final Path DEOBFUSCATED_CLASSES_PATH = TEST_DATA_PATH.resolve("deobfuscated");
  public static final Path RESULTS_CLASSES_PATH = TEST_DATA_PATH.resolve("results");

  private final List<RegisteredTest> registeredTests = new ArrayList<>();

  /**
   * Register your tests here
   */
  protected abstract void registerAll();

  /**
   * Register input files for testing
   *
   * @param testName     Test name
   * @param inputType    Input type. See enum optionsConsumer.
   * @param transformers Transformers to use
   * @param sources      You can choose one class or multiple classes for testing
   */
  protected void register(String testName, InputType inputType, List<Supplier<Transformer>> transformers, Source... sources) {
    this.registeredTests.add(new RegisteredTest(testName, inputType, transformers, sources));
  }

  @BeforeAll
  public static void setup() {
    // Don't spam logs
    Configurator.setRootLevel(Level.WARN);
  }

  @TestFactory
  @DisplayName("Test deobfuscation")
  public Stream<DynamicTest> testDeobfuscation() {
    this.registeredTests.clear();
    FileHelper.deleteDirectory(DEOBFUSCATED_CLASSES_PATH);

    this.registerAll();
    return this.registeredTests.stream().map(RegisteredTest::buildTest);
  }

  /**
   * @see TestDeobfuscationBase#register(String, InputType, List, Source...)
   */
  public record RegisteredTest(
      String testName,
      InputType inputType,
      List<Supplier<Transformer>> transformers,
      Source[] sources
  ) {
    /**
     * Build test
     */
    public DynamicTest buildTest() {
      return DynamicTest.dynamicTest(this.testName, this::runTest);
    }

    /**
     * Run test
     */
    private void runTest() {
      // Setup builder
      DeobfuscatorOptions.Builder optionsBuilder = DeobfuscatorOptions.builder()
          .transformers(this.transformers.toArray(new Supplier[0]));

      Path decompilerInputDir = DEOBFUSCATED_CLASSES_PATH.resolve(this.inputType.directory());
      String jarSource = null;

      // Get sources paths
      if (this.inputType == InputType.CUSTOM_JAR) {
        if (sources.length > 1) {
          throw new IllegalArgumentException("Cannot use multiple sources with a jar input");
        }

        jarSource = sources[0].sourceName;

        Path relativePath = Path.of(this.inputType.directory(), jarSource);

        // Add jar input
        Path inputJarPath = COMPILED_CLASSES_PATH.resolve(relativePath + ".jar");
        optionsBuilder.inputJar(inputJarPath);

        decompilerInputDir = decompilerInputDir.resolve(jarSource);
      } else {
        for (Source source : sources) {
          Path compiledClassPath = COMPILED_CLASSES_PATH.resolve(this.inputType.directory()).resolve(source.sourceName + ".class");
          if (Files.notExists(compiledClassPath)) {
            throw new IllegalArgumentException(
                "Compiled class not found: '" + compiledClassPath.toAbsolutePath().normalize() + "'. You might forgot to compile the class. Use 'mvn test' to compile test classes."
            );
          }

          // Add class
          optionsBuilder.clazz(compiledClassPath, source.sourceName + ".class");
        }
      }

      // Last configurations
      optionsBuilder
          .outputJar(null)
          .outputDir(decompilerInputDir)
          .skipFiles();

      // Build and run deobfuscator!
      Deobfuscator.from(optionsBuilder.build()).start();

      if (!shouldDecompile()) return;

      // Init context sources
      List<IContextSource> contextSources = new ArrayList<>();
      if (this.inputType != InputType.CUSTOM_JAR) {
        for (Source source : sources) {
          if (!source.decompile) continue;

          contextSources.add(new SingleClassContextSource(
              DEOBFUSCATED_CLASSES_PATH.resolve(this.inputType.directory()).resolve(source.sourceName + ".class"),
              source.sourceName
          ));
        }
      }

      // Assert output
      this.assertOutput(contextSources, decompilerInputDir, jarSource);
    }

    /**
     * Asserts output of a decompilation result
     *
     * @param contextSources  Classes to be decompiled
     * @param inputDir        Optionally you can give a whole directory to decompile.
     * @param jarRelativePath Specifies a relative path in save directory. Only used for jars
     */
    private void assertOutput(List<IContextSource> contextSources, @Nullable Path inputDir, @Nullable String jarRelativePath) {
      AssertingResultSaver assertingResultSaver = new AssertingResultSaver(this.inputType, jarRelativePath);

      // Decompile classes
      Decompiler.Builder decompilerBuilder = Decompiler.builder()
          .option(IFernflowerPreferences.INDENT_STRING, "    ")
          .output(assertingResultSaver); // Assert output

      // Add sources
      if (this.inputType == InputType.CUSTOM_JAR) {
        decompilerBuilder.inputs(inputDir.toFile()); //fuck you path > file
      } else {
        for (IContextSource contextSource : contextSources) {
          decompilerBuilder.inputs(contextSource);
        }
      }

      // Decompile
      decompilerBuilder.build().decompile();

      if (assertingResultSaver.savedContent()) {
        throw new TestAbortedException("No previous decompiled code found, skipping test");
      }
    }

    private boolean shouldDecompile() {
      for (Source source : sources) {
        if (source.decompile) return true;
      }
      return false;
    }
  }

  public enum InputType {
    CUSTOM_CLASS("custom-classes"),
    CUSTOM_JAR("custom-jars"),
    JAVA_CODE("java")
    ;

    private final String directory;

    InputType(String directory) {
      this.directory = directory;
    }

    public String directory() {
      return directory;
    }
  }

  /**
   * @param sourceName Class or jar path
   * @param decompile  Should decompile this source. Does not work with {@link InputType#CUSTOM_JAR}
   */
  public record Source(String sourceName, boolean decompile) {
    public static Source of(String source, boolean decompile) {
      return new Source(source, decompile);
    }

    public static Source of(String source) {
      return of(source, true);
    }
  }
}
