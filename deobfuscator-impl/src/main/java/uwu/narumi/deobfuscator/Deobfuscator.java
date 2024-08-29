package uwu.narumi.deobfuscator;

import java.io.FileInputStream;
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
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.context.Context;
import uwu.narumi.deobfuscator.api.context.DeobfuscatorOptions;
import uwu.narumi.deobfuscator.api.execution.SandBox;
import uwu.narumi.deobfuscator.api.helper.ClassHelper;
import uwu.narumi.deobfuscator.api.helper.FileHelper;
import uwu.narumi.deobfuscator.api.library.Library;
import uwu.narumi.deobfuscator.api.library.LibraryClassLoader;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

public class Deobfuscator {

  /**
   * Creates a new {@link Deobfuscator} instance from its options
   */
  public static Deobfuscator from(DeobfuscatorOptions options) {
    return new Deobfuscator(options);
  }

  private static final Logger LOGGER = LogManager.getLogger(Deobfuscator.class);

  private final DeobfuscatorOptions options;
  private final Context context;

  private Deobfuscator(DeobfuscatorOptions options) {
    if (options.inputJar() != null && Files.notExists(options.inputJar())) {
      throw new IllegalArgumentException("Input jar does not exist");
    }

    if (options.outputJar() != null && Files.exists(options.outputJar())) {
      LOGGER.warn("Output file already exist, data will be overwritten");
    }

    this.options = options;

    LibraryClassLoader libraryClassLoader = new LibraryClassLoader(
        this.getClass().getClassLoader(),
        options.libraries().stream().map(path -> new Library(path, options.classWriterFlags())).toList()
    );

    // Temporary disabled until the sandbox is fixed
    SandBox sandBox = null;
    /*try {
      sandBox = new SandBox(
          this.context.getLoader(),
          options.virtualMachine() == null ? new VirtualMachine() : options.virtualMachine()
      );
    } catch (Throwable t) {
      LOGGER.error("SSVM bootstrap failed");
      LOGGER.debug("Error", t);
      if (options.consoleDebug()) t.printStackTrace();
    }*/

    this.context = new Context(options, libraryClassLoader, sandBox);
  }

  public void start() {
    loadInput();
    transform(this.options.transformers());
    saveOutput();
  }

  public Context getContext() {
    return context;
  }

  private void loadInput() {
    if (this.options.inputJar() != null) {
      LOGGER.info("Loading jar file: {}", this.options.inputJar());
      // Load jar
      FileHelper.loadFilesFromZip(this.options.inputJar(), this::loadClass);
      LOGGER.info("Loaded jar file: {}\n", this.options.inputJar());
    }

    for (DeobfuscatorOptions.ExternalClass clazz : this.options.classes()) {
      LOGGER.info("Loading class: {}", clazz.relativePath());

      try (InputStream inputStream = new FileInputStream(clazz.path().toFile())) {
        // Load class
        this.loadClass(clazz.relativePath(), inputStream.readAllBytes());

        LOGGER.info("Loaded class: {}\n", clazz.relativePath());
      } catch (IOException e) {
        LOGGER.error("Could not load class: {}", clazz.relativePath(), e);
      }
    }
  }

  private void loadClass(String path, byte[] bytes) {
    try {
      if (ClassHelper.isClass(bytes)) {
        ClassWrapper classWrapper = ClassHelper.loadClass(
            path,
            bytes,
            this.options.classReaderFlags(),
            this.options.classWriterFlags(),
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
      if (this.options.consoleDebug()) e.printStackTrace();
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
    if (this.options.outputJar() != null) {
      saveToJar();
    } else if (this.options.outputDir() != null) {
      saveClassesToDir();
    } else {
      throw new IllegalStateException("No output file or directory provided");
    }
  }

  private void saveClassesToDir() {
    LOGGER.info("Saving classes to output directory: {}", this.options.outputDir());

    context
        .getClasses()
        .forEach((ignored, classWrapper) -> {
          try {
            byte[] data = classWrapper.compileToBytes(this.context);

            Path path = this.options.outputDir().resolve(classWrapper.getPath() + ".class");
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
    LOGGER.info("Saving output file: {}", this.options.outputJar());

    // Create directories if not exists
    if (this.options.outputJar().getParent() != null) {
      try {
        Files.createDirectories(this.options.outputJar().getParent());
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(this.options.outputJar()))) {
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
                  if (this.options.consoleDebug()) e.printStackTrace();

                  try {
                    byte[] data =
                        ClassHelper.classToBytes(
                            context.getOriginalClasses().get(classWrapper.name()).getClassNode(),
                            this.options.classWriterFlags()
                        );

                    zipOutputStream.putNextEntry(new ZipEntry(classWrapper.name() + ".class"));
                    zipOutputStream.write(data);
                  } catch (Exception e2) {
                    LOGGER.error("Could not save original class: {}", classWrapper.name());
                    LOGGER.debug("Error", e2);

                    if (this.options.consoleDebug()) e2.printStackTrace();
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

                  if (this.options.consoleDebug()) e.printStackTrace();
                }

                context.getFiles().remove(name);
              });
    } catch (IOException e) {
      LOGGER.error("Could not save output file: {}", this.options.outputJar());
      throw new RuntimeException(e);
    }

    LOGGER.info("Saved output file: {}\n", this.options.outputJar());
  }
}
