package uwu.narumi.deobfuscator;

import dev.xdark.ssvm.VirtualMachine;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.context.Context;
import uwu.narumi.deobfuscator.api.helper.ClassHelper;
import uwu.narumi.deobfuscator.api.helper.FileHelper;
import uwu.narumi.deobfuscator.api.library.Library;
import uwu.narumi.deobfuscator.api.library.LibraryClassLoader;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

public class Deobfuscator {

  private static final Logger LOGGER = LogManager.getLogger(Deobfuscator.class);

  private final Context context = new Context();

  private final List<Supplier<Transformer>> transformers = new ArrayList<>();
  @Nullable
  private final Path inputJar;
  @Nullable
  private final Path outputJar;
  @Nullable
  private final Path outputDir;
  private final List<ExternalClass> classes;

  private final int classReaderFlags;
  private final int classWriterFlags;
  private final boolean consoleDebug;

  private Deobfuscator(Builder builder) throws FileNotFoundException {
    if (builder.inputJar == null && builder.classes.isEmpty()) {
      throw new FileNotFoundException("No input files provided");
    }

    if (builder.outputJar != null && builder.outputJar.toFile().exists())
      LOGGER.warn("Output file already exist, data will be overwritten");

    this.inputJar = builder.inputJar;
    this.outputJar = builder.outputJar;
    this.outputDir = builder.outputDir;
    this.classes = builder.classes;

    this.transformers.addAll(builder.transformers);
    this.classReaderFlags = builder.classReaderFlags;
    this.classWriterFlags = builder.classWriterFlags;
    this.consoleDebug = builder.consoleDebug;

    this.context.setLoader(
        new LibraryClassLoader(
            this.getClass().getClassLoader(),
            builder.libraries.stream().map(path -> new Library(path, this.classWriterFlags)).toList()));

    // Temporary disabled until the sandbox is fixed
    /*try {
      this.context.setSandBox(
          new SandBox(
              this.context.getLoader(),
              builder.virtualMachine == null ? new VirtualMachine() : builder.virtualMachine));
    } catch (Throwable t) {
      LOGGER.error("SSVM bootstrap failed");
      LOGGER.debug("Error", t);
      if (consoleDebug) t.printStackTrace();

      this.context.setSandBox(null);
    }*/
  }

  public static Builder builder() {
    return new Builder();
  }

  public void start() {
    try {
      loadInput();
      transform(transformers);
      saveOutput();
    } catch (Exception e) {
      LOGGER.error("Error occurred while obfuscation");
      LOGGER.debug("Error", e);

      if (consoleDebug) e.printStackTrace();
    }
  }

  public Context getContext() {
    return context;
  }

  private void loadInput() {
    if (inputJar != null) {
      LOGGER.info("Loading jar file: {}", inputJar);
      // Load jar
      FileHelper.loadFilesFromZip(inputJar, this::loadClass);
      LOGGER.info("Loaded jar file: {}\n", inputJar);
    }

    for (ExternalClass clazz : classes) {
      LOGGER.info("Loading class: {}", clazz.relativePath);

      try (InputStream inputStream = new FileInputStream(clazz.path.toFile())) {
        // Load class
        this.loadClass(clazz.relativePath, inputStream.readAllBytes());

        LOGGER.info("Loaded class: {}\n", clazz.relativePath);
      } catch (IOException e) {
        LOGGER.error("Could not load class: {}", clazz.relativePath, e);
      }
    }
  }

  private void loadClass(String path, byte[] bytes) {
    try {
      if (ClassHelper.isClass(bytes)) {
        ClassWrapper classWrapper = ClassHelper.loadClass(
            path,
            bytes,
            this.classReaderFlags,
            this.classWriterFlags,
            true
        );
        context.getClasses().putIfAbsent(classWrapper.name(), classWrapper);
        context.getOriginalClasses().putIfAbsent(classWrapper.name(), classWrapper.clone());
      } else if (!context.getFiles().containsKey(path)) {
        context.getFiles().put(path, bytes);
      }
    } catch (Exception e) {
      LOGGER.error("Could not load class: {}, adding as file", path);
      LOGGER.debug("Error", e);

      context.getFiles().putIfAbsent(path, bytes);
      if (consoleDebug) e.printStackTrace();
    }
  }

  public void transform(List<Supplier<Transformer>> transformers) {
    if (transformers == null || transformers.isEmpty()) return;

    // Run all transformers!
    transformers.forEach(transformerSupplier -> Transformer.transform(transformerSupplier, null, this.context));
  }

  /**
   * Saves deobfuscation output result
   */
  private void saveOutput() {
    if (outputJar != null) {
      saveToJar();
    } else if (outputDir != null) {
      saveClassesToDir();
    } else {
      throw new IllegalStateException("No output file or directory provided");
    }
  }

  private void saveClassesToDir() {
    LOGGER.info("Saving classes to output directory: {}", outputDir);

    context
        .getClasses()
        .forEach((ignored, classWrapper) -> {
          try {
            byte[] data = classWrapper.compileToBytes(this.context);

            Path path = this.outputDir.resolve(classWrapper.getPath() + ".class");
            Files.createDirectories(path.getParent());
            Files.write(path, data);
          } catch (Exception e) {
            LOGGER.error("Could not save class: {}", classWrapper.name(), e);
          }

          context.getOriginalClasses().remove(classWrapper.name());
          context.getClasses().remove(classWrapper.name());
        });
  }

  private void saveToJar() {
    LOGGER.info("Saving output file: {}", outputJar);

    try (ZipOutputStream zipOutputStream =
        new ZipOutputStream(new FileOutputStream(outputJar.toFile()))) {
      zipOutputStream.setLevel(9);

      context
          .getClasses()
          .forEach(
              (ignored, classWrapper) -> {
                try {
                  byte[] data = classWrapper.compileToBytes(this.context);

                  zipOutputStream.putNextEntry(new ZipEntry(classWrapper.name() + ".class"));
                  zipOutputStream.write(data);
                } catch (Exception e) {
                  LOGGER.error(
                      "Could not save class, saving original class instead of deobfuscated: {}",
                      classWrapper.name());
                  LOGGER.debug("Error", e);
                  if (consoleDebug) e.printStackTrace();

                  try {
                    byte[] data =
                        ClassHelper.classToBytes(
                            context.getOriginalClasses().get(classWrapper.name()).getClassNode(),
                            classWriterFlags);

                    zipOutputStream.putNextEntry(new ZipEntry(classWrapper.name() + ".class"));
                    zipOutputStream.write(data);
                  } catch (Exception e2) {
                    LOGGER.error("Could not save original class: {}", classWrapper.name());
                    LOGGER.debug("Error", e2);

                    if (consoleDebug) e2.printStackTrace();
                  }
                }

                context.getOriginalClasses().remove(classWrapper.name());
                context.getClasses().remove(classWrapper.name());
              });

      context
          .getFiles()
          .forEach(
              (name, data) -> {
                try {
                  zipOutputStream.putNextEntry(new ZipEntry(name));
                  zipOutputStream.write(data);
                } catch (Exception e) {
                  LOGGER.error("Could not save file: {}", name);
                  LOGGER.debug("Error", e);

                  if (consoleDebug) e.printStackTrace();
                }

                context.getFiles().remove(name);
              });
    } catch (Exception e) {
      LOGGER.error("Could not save output file: {}", outputJar);
      LOGGER.debug("Error", e);
      if (consoleDebug) e.printStackTrace();
    }

    LOGGER.info("Saved output file: {}\n", outputJar);
  }

  public static class Builder {

    private final Set<Path> libraries = new HashSet<>();
    @Nullable
    private Path inputJar = null;
    @Nullable
    private Path outputJar = null;
    @Nullable
    private Path outputDir = null;
    private List<ExternalClass> classes = new ArrayList<>();
    private List<Supplier<Transformer>> transformers = List.of();

    private int classReaderFlags = ClassReader.SKIP_FRAMES;
    private int classWriterFlags = ClassWriter.COMPUTE_FRAMES;

    private boolean consoleDebug;

    private VirtualMachine virtualMachine;

    private Builder() {}

    public Builder inputJar(@Nullable Path inputJar) {
      this.inputJar = inputJar;
      if (this.inputJar != null) {
        String fullName = inputJar.getFileName().toString();
        int dot = fullName.lastIndexOf('.');
        this.outputJar =
            inputJar
                .getParent()
                .resolve(
                    dot == -1
                        ? fullName + "-out"
                        : fullName.substring(0, dot) + "-out" + fullName.substring(dot));
        this.libraries.add(inputJar);
      }
      return this;
    }

    public Builder outputJar(@Nullable Path outputJar) {
      this.outputJar = outputJar;
      return this;
    }

    /**
     * Output dir for deobfuscated classes
     */
    public Builder outputDir(@Nullable Path outputDir) {
      this.outputDir = outputDir;
      return this;
    }

    public Builder libraries(Path... paths) {
      this.libraries.addAll(List.of(paths));
      return this;
    }

    /**
     * Add external class to deobfuscate
     * @param path Path to external class
     * @param relativePath Relative path for saving purposes
     */
    public Builder clazz(Path path, String relativePath) {
      this.classes.add(new ExternalClass(path, relativePath));
      return this;
    }

    @SafeVarargs
    public final Builder transformers(Supplier<Transformer>... transformers) {
      this.transformers = Arrays.asList(transformers);
      return this;
    }

    public Builder classReaderFlags(int classReaderFlags) {
      this.classReaderFlags = classReaderFlags;
      return this;
    }

    public Builder classWriterFlags(int classWriterFlags) {
      this.classWriterFlags = classWriterFlags;
      return this;
    }

    public Builder consoleDebug() {
      this.consoleDebug = true;
      return this;
    }

    public Builder virtualMachine(VirtualMachine virtualMachine) {
      this.virtualMachine = virtualMachine;
      return this;
    }

    public Deobfuscator build() throws FileNotFoundException {
      return new Deobfuscator(this);
    }
  }

  public record ExternalClass(Path path, String relativePath) {
  }
}
