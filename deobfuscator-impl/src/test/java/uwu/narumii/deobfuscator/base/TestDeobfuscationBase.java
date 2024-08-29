package uwu.narumii.deobfuscator.base;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.util.FileUtils;
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
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.context.DeobfuscatorOptions;
import uwu.narumi.deobfuscator.api.helper.FileHelper;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
   * @param testName Test name
   * @param inputType Input type. See enum options.
   * @param transformers Transformers to use
   * @param sources You can choose one class or multiple classes for testing
   */
  protected void register(String testName, InputType inputType, List<Supplier<Transformer>> transformers, String... sources) {
    // Register
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

  public record RegisteredTest(String testName, InputType inputType, List<Supplier<Transformer>> transformers, String[] sources) {
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

      Path inputDir = null;
      String jarSource = null;

      // Get sources paths
      if (this.inputType == InputType.CUSTOM_JAR) {
        if (sources.length > 1) {
          throw new IllegalArgumentException("Cannot use multiple sources with a jar input");
        }

        jarSource = sources[0];

        Path relativePath = Path.of(this.inputType.directory(), sources[0] + ".jar");

        // Add jar input
        Path inputJarPath = COMPILED_CLASSES_PATH.resolve(relativePath);
        optionsBuilder.inputJar(inputJarPath);

        inputDir = DEOBFUSCATED_CLASSES_PATH.resolve(relativePath);
      } else {
        for (String sourceName : sources) {
          Path compiledClassPath = COMPILED_CLASSES_PATH.resolve(this.inputType.directory()).resolve(sourceName + ".class");
          if (Files.notExists(compiledClassPath)) {
            throw new IllegalArgumentException(
                "Compiled class not found: '" + compiledClassPath.toAbsolutePath().normalize() + "'. You might forgot to compile the class. Use 'mvn test' to compile test classes."
            );
          }

          // Add class
          optionsBuilder.clazz(compiledClassPath, sourceName);
        }
      }

      // Last configurations
      optionsBuilder
          .outputJar(null)
          .outputDir(DEOBFUSCATED_CLASSES_PATH.resolve(this.inputType.directory()));

      // Build and run deobfuscator!
      Deobfuscator.from(optionsBuilder.build()).start();

      // Init context sources
      List<IContextSource> contextSources = new ArrayList<>();
      if (this.inputType != InputType.CUSTOM_JAR) {
        for (String sourceName : sources) {
          contextSources.add(new SingleClassContextSource(
              DEOBFUSCATED_CLASSES_PATH.resolve(this.inputType.directory()).resolve(sourceName + ".class"),
              sourceName));
        }
      }

      // Assert output
      this.assertOutput(contextSources, inputDir, jarSource);
    }

    /**
     * Asserts output of a decompilation result
     *
     * @param contextSources Classes to be decompiled
     * @param inputDir Optionally you can give a whole directory to decompile.
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
}
