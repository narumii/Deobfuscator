package uwu.narumi.deobfuscator.base;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.java.decompiler.api.Decompiler;
import org.jetbrains.java.decompiler.main.extern.IContextSource;
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.opentest4j.TestAbortedException;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.api.context.DeobfuscatorOptions;
import uwu.narumi.deobfuscator.api.helper.FileHelper;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public abstract class TestDeobfuscationBase {
  public static final Path TEST_DATA_PATH = Path.of("..", "testData");
  public static final Path COMPILED_PATH = TEST_DATA_PATH.resolve("compiled");
  public static final Path DEOBFUSCATED_PATH = TEST_DATA_PATH.resolve("deobfuscated");
  public static final Path RESULTS_PATH = TEST_DATA_PATH.resolve("results");

  private final List<RegisteredTest> registeredTests = new ArrayList<>();

  /**
   * Register your tests here
   */
  protected abstract void registerAll();

  @BeforeAll
  public static void setup() {
    // Don't spam logs
    Configurator.setRootLevel(Level.ERROR);
  }

  @TestFactory
  @DisplayName("Test deobfuscation")
  public Stream<DynamicTest> testDeobfuscation() {
    this.registeredTests.clear();
    FileHelper.deleteDirectory(DEOBFUSCATED_PATH);

    this.registerAll();
    return this.registeredTests.stream().map(RegisteredTest::buildTest);
  }

  public record RegisteredTest(
      String testName,
      InputType inputType,
      OutputType outputType,
      List<Supplier<Transformer>> transformers,
      String path,
      boolean decompile
  ) {
    public RegisteredTest {
      if (outputType == OutputType.SINGLE_CLASS && inputType == InputType.CUSTOM_JAR) {
        throw new IllegalArgumentException("Cannot use 'OutputType.SINGLE_CLASS' with a jar input");
      }
    }

    /**
     * Build test
     */
    public DynamicTest buildTest() {
      return DynamicTest.dynamicTest(this.testName,
          () -> assertTimeoutPreemptively(Duration.ofSeconds(300), this::runTest));
    }

    /**
     * Run test
     */
    private void runTest() {
      // Setup builder
      DeobfuscatorOptions.Builder optionsBuilder = DeobfuscatorOptions.builder()
          .transformers(this.transformers.toArray(new Supplier[0]));

      // Some paths
      Path outputDir = DEOBFUSCATED_PATH.resolve(this.inputType.directory());
      Path decompilerOutputDir = RESULTS_PATH.resolve(this.inputType.directory());
      IContextSource contextSource = null;

      if (this.outputType == OutputType.SINGLE_CLASS) {
        // Handle single class output

        Path relativePath = Path.of(this.inputType.directory()).resolve(this.path);
        Path compiledClassPath = COMPILED_PATH.resolve(relativePath);
        Path deobfuscatedClassPath = DEOBFUSCATED_PATH.resolve(relativePath);

        if (Files.notExists(compiledClassPath)) {
          throw new IllegalArgumentException(
              "Compiled class not found: '" + compiledClassPath.toAbsolutePath().normalize() + "'." +
                  (this.inputType == InputType.JAVA_CODE ? " Did you forgot to compile it? Use 'mvn test' to compile test classes." : "")
          );
        }

        // Add class
        optionsBuilder.externalFile(compiledClassPath, this.path);
        contextSource = new SingleClassContextSource(deobfuscatedClassPath, this.path);
      } else {
        // Handle multiple classes output

        Path inputPath = COMPILED_PATH.resolve(this.inputType.directory()).resolve(this.path);
        String relativePath = this.path;
        if (this.inputType == InputType.CUSTOM_JAR) {
          // Prepare input
          optionsBuilder.inputJar(inputPath);

          // Set the correct relative path
          relativePath = this.path.substring(0, this.path.length() - ".jar".length());
        } else {
          // Prepare input files
          optionsBuilder.inputDir(inputPath);
        }

        // Prepare output
        outputDir = outputDir.resolve(relativePath);
        decompilerOutputDir = decompilerOutputDir.resolve(relativePath);
      }

      // Last configurations
      optionsBuilder
          .outputJar(null)
          .outputDir(outputDir)
          .skipFiles();

      // Build and run deobfuscator!
      Deobfuscator.from(optionsBuilder.build()).start();

      if (!this.decompile) {
        return;
      }

      // Assert output
      this.assertOutput(contextSource, outputDir, decompilerOutputDir);
    }

    /**
     * Asserts output of a decompilation result
     *
     * @param contextSource  Class to be decompiled
     * @param inputDir Directory to decompile.
     */
    private void assertOutput(@Nullable IContextSource contextSource, @Nullable Path inputDir, Path decompilerOutputDir) {
      AssertingResultSaver assertingResultSaver = new AssertingResultSaver(decompilerOutputDir);

      Decompiler.Builder decompilerBuilder = Decompiler.builder()
          .option(IFernflowerPreferences.INDENT_STRING, "    ")
          .option(IFernflowerPreferences.INCLUDE_JAVA_RUNTIME, true)
          .output(assertingResultSaver); // Assert output

      // Add sources
      if (contextSource != null) {
        decompilerBuilder.inputs(contextSource);
      } else if (inputDir != null) {
        decompilerBuilder.inputs(inputDir.toFile()); //fuck you path > file
      } else {
        throw new IllegalArgumentException();
      }

      // Decompile
      decompilerBuilder.build().decompile();

      if (assertingResultSaver.savedContent()) {
        throw new TestAbortedException("No previous decompiled code found, skipping test");
      }
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

  public enum OutputType {
    SINGLE_CLASS,
    MULTIPLE_CLASSES
  }

  protected TestBuilder test(String testName) {
    return new TestBuilder().name(testName);
  }

  protected class TestBuilder {
    private String name = null;
    private InputType inputType = null;
    private OutputType outputType = null;
    private List<Supplier<Transformer>> transformers = null;
    private String path = null;

    private boolean decompile = true;

    private TestBuilder() {
    }

    /**
     * Test name
     */
    @Contract("_ -> this")
    public TestBuilder name(String name) {
      this.name = name;
      return this;
    }

    /**
     * Transformers to use
     */
    @Contract("_ -> this")
    @SafeVarargs
    public final TestBuilder transformers(Supplier<Transformer>... transformers) {
      this.transformers = List.of(transformers);
      return this;
    }

    /**
     * Specifies input to your test
     *
     * <p>Path should be:<br>
     * {@link OutputType#SINGLE_CLASS}:
     * <ul>
     *   <li>For {@link InputType#CUSTOM_CLASS} and {@link InputType#JAVA_CODE} - it should be a path to .class file</li>
     * </ul>
     * {@link OutputType#MULTIPLE_CLASSES}:
     * <ul>
     *   <li>For {@link InputType#CUSTOM_CLASS} and {@link InputType#JAVA_CODE} - it should be a path to a directory with .class files</li>
     *   <li>For {@link InputType#CUSTOM_JAR} -  it should be a path to a .jar file</li>
     * </ul>
     *
     * @param outputType Output type
     * @param inputType Input type
     * @param path Input path (see note above)
     */
    @Contract("_,_,_ -> this")
    public TestBuilder input(OutputType outputType, InputType inputType, String path) {
      this.outputType = outputType;
      this.inputType = inputType;
      this.path = path;
      return this;
    }

    /**
     * Alias for {@link #input(OutputType, InputType, String)} with {@link OutputType#SINGLE_CLASS}
     */
    @Contract("_,_ -> this")
    public TestBuilder inputClass(InputType inputType, String path) {
      return this.input(OutputType.SINGLE_CLASS, inputType, path);
    }

    /**
     * Alias for {@link #input(OutputType, InputType, String)} with {@link OutputType#MULTIPLE_CLASSES}
     */
    @Contract("_,_ -> this")
    public TestBuilder inputClassesDir(InputType inputType, String path) {
      return this.input(OutputType.MULTIPLE_CLASSES, inputType, path);
    }

    /**
     * Alias for {@link #input(OutputType, InputType, String)} with {@link OutputType#MULTIPLE_CLASSES} and {@link InputType#CUSTOM_JAR}
     */
    @Contract("_ -> this")
    public TestBuilder inputJar(String path) {
      return this.input(OutputType.MULTIPLE_CLASSES, InputType.CUSTOM_JAR, path);
    }

    @Contract(" -> this")
    public TestBuilder noDecompile() {
      this.decompile = false;
      return this;
    }

    /**
     * Register input files for testing
     */
    public void register() {
      registeredTests.add(
          new RegisteredTest(name, inputType, outputType, transformers, path, decompile)
      );
    }
  }
}
