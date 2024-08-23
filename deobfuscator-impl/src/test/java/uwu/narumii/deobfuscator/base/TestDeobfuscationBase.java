package uwu.narumii.deobfuscator.base;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.jetbrains.java.decompiler.api.Decompiler;
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.opentest4j.TestAbortedException;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

public abstract class TestDeobfuscationBase {
  private static final Path COMPILED_CLASSES_PATH = Path.of("..", "testData", "compiled");

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
  @DisplayName("Test classes")
  public Stream<DynamicTest> testRegistered() {
    this.registeredTests.clear();
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
      Deobfuscator.Builder deobfuscatorBuilder = Deobfuscator.builder()
          .transformers(this.transformers.toArray(new Supplier[0]));

      String inputJar = null;

      // Get sources paths
      Map<String, String> sourcePathToSourceName = new HashMap<>();
      if (this.inputType == InputType.CUSTOM_JAR) {
        if (sources.length > 1) {
          throw new IllegalArgumentException("Cannot use multiple sources with a jar input");
        }

        inputJar = sources[0];

        // Add jar input
        deobfuscatorBuilder.input(
            Path.of(COMPILED_CLASSES_PATH.toString(), inputType.directory(), sources[0] + ".jar")
        );
      } else {
        for (String sourceName : sources) {
          Path path = Path.of("..", "testData", "compiled", inputType.directory(), sourceName + ".class");

          if (!path.toFile().exists()) {
            throw new IllegalArgumentException("File not found: " + path.toAbsolutePath().normalize());
          }

          // Add class
          deobfuscatorBuilder.clazz(path);
          sourcePathToSourceName.put(path.toString(), sourceName);
        }
      }

      Deobfuscator deobfuscator;
      try {
        // Build deobfuscator
        deobfuscator = deobfuscatorBuilder
            .output(null)
            .build();
      } catch (FileNotFoundException e) {
        throw new RuntimeException(e);
      }

      // Run deobfuscator!
      deobfuscator.start();

      // Assert output
      this.assertOutput(deobfuscator, sourcePathToSourceName, inputJar);
    }

    private void assertOutput(Deobfuscator deobfuscator, Map<String, String> sourcePathToSourceName, String inputJar) {
      AssertingResultSaver assertingResultSaver = new AssertingResultSaver(this.inputType, sourcePathToSourceName, inputJar);

      // Decompile classes
      Decompiler.Builder decompilerBuilder = Decompiler.builder()
          .option(IFernflowerPreferences.INDENT_STRING, "    ")
          .output(assertingResultSaver); // Assert output

      // Add sources
      for (ClassWrapper classWrapper : deobfuscator.getContext().classes()) {
        decompilerBuilder.inputs(new ClassWrapperContextSource(classWrapper, deobfuscator.getContext()));
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
