package uwu.narumi.deobfuscator;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.context.Context;
import uwu.narumi.deobfuscator.api.context.DeobfuscatorOptions;
import uwu.narumi.deobfuscator.api.helper.ClassHelper;
import uwu.narumi.deobfuscator.api.helper.FileHelper;
import uwu.narumi.deobfuscator.api.classpath.Classpath;
import uwu.narumi.deobfuscator.api.inheritance.InheritanceGraph;
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
    this.options = options;

    if (options.inputJar() != null && Files.notExists(options.inputJar())) {
      throw new IllegalArgumentException("Input jar does not exist");
    }

    if (options.outputJar() != null && Files.exists(options.outputJar())) {
      LOGGER.warn("Output file already exist, data will be overwritten");
    }

    Classpath primaryClasspath = buildPrimaryClasspath();
    LOGGER.info("Loaded {} classes from a primary source", primaryClasspath.rawClasses().size());

    Classpath libClasspath = buildLibClasspath();
    LOGGER.info("Loaded {} classes from libraries", libClasspath.rawClasses().size());

    this.context = new Context(options, primaryClasspath, libClasspath);
  }

  public Classpath buildPrimaryClasspath() {
    Classpath.Builder builder = Classpath.builder();
    // Add input jar as a library
    if (options.inputJar() != null) {
      builder.addJar(options.inputJar());
    }
    // Add raw classes as a library
    if (!options.classes().isEmpty()) {
      options.classes().forEach(builder::addExternalClass);
    }

    return builder.build();
  }

  public Classpath buildLibClasspath() {
    Classpath.Builder builder = Classpath.builder();
    // Add libraries
    options.libraries().forEach(builder::addJar);

    return builder.build();
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
      FileHelper.loadFilesFromZip(this.options.inputJar(), this::loadClassOrFile);
      LOGGER.info("Loaded jar file: {}", this.options.inputJar());
    }

    for (DeobfuscatorOptions.ExternalClass clazz : this.options.classes()) {
      LOGGER.info("Loading class: {}", clazz.pathInJar());

      try (InputStream inputStream = new FileInputStream(clazz.path().toFile())) {
        // Load class
        this.loadClassOrFile(clazz.pathInJar(), inputStream.readAllBytes());

        LOGGER.info("Loaded class: {}", clazz.pathInJar());
      } catch (IOException e) {
        LOGGER.error("Could not load class: {}", clazz.pathInJar(), e);
      }
    }
  }

  private void loadClassOrFile(String pathInJar, byte[] bytes) {
    // Load class
    if (ClassHelper.isClass(pathInJar, bytes)) {
      try {
        ClassWrapper classWrapper = ClassHelper.loadUnknownClass(pathInJar, bytes, ClassReader.SKIP_FRAMES);
        context.getClasses().putIfAbsent(classWrapper.name(), classWrapper);
        return;
      } catch (Exception e) {
        LOGGER.error("Could not load class: {}, adding as file", pathInJar, e);
        // Will add as a file
      }
    }

    // Load file
    if (!context.getFiles().containsKey(pathInJar)) {
      context.getFiles().put(pathInJar, bytes);
    }
  }

  public void transform(List<Supplier<Transformer>> transformers) {
    if (transformers.isEmpty()) return;

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
      saveToDir();
    } else {
      throw new IllegalStateException("No output file or directory provided");
    }
  }

  private void saveToDir() {
    LOGGER.info("Saving output to directory: {}", this.options.outputDir());

    save((path, data) -> {
      Path realPath = this.options.outputDir().resolve(path);
      try {
        Files.createDirectories(realPath.getParent());
        Files.write(realPath, data);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });
  }

  private void saveToJar() {
    LOGGER.info("Saving output to jar: {}", this.options.outputJar());

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

      save((path, data) -> {
        try {
          zipOutputStream.putNextEntry(new ZipEntry(path));
          zipOutputStream.write(data);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      });
    } catch (IOException e) {
      LOGGER.error("Could not save output to jar: {}", this.options.outputJar());
      throw new RuntimeException(e);
    }

    LOGGER.info("Saved output to jar: {}\n", this.options.outputJar());
  }

  /**
   * Saves classes and files using provided saver
   *
   * @param saver a consumer that accepts a path and data to save
   */
  private void save(BiConsumer<String, byte[]> saver) {
    InheritanceGraph inheritanceGraph = new InheritanceGraph(this.context.getCombinedClasspath());

    // Save classes
    context.getClasses().forEach((ignored, classWrapper) -> {
      String path = classWrapper.getPathInJar();

      try {
        byte[] data = classWrapper.compileToBytes(inheritanceGraph, this.options.classWriterFlags());
        saver.accept(path, data);
      } catch (Exception e) {
        LOGGER.error("Could not save class, saving original class instead of deobfuscated: {}", classWrapper.name());
        if (this.options.printStacktraces()) LOGGER.throwing(e);

        try {
          // Save original class as a fallback
          byte[] data = context.getPrimaryClasspath().rawClasses().get(classWrapper.name());
          saver.accept(path, data);
        } catch (Exception e2) {
          LOGGER.error("Could not save original class: {}", classWrapper.name());
          if (this.options.printStacktraces()) LOGGER.throwing(e2);
        }
      }

      context.getClasses().remove(classWrapper.name());
    });

    // Save files
    if (!this.options.skipFiles()) {
      context.getFiles().forEach((path, data) -> {
        try {
          saver.accept(path, data);
        } catch (Exception e) {
          LOGGER.error("Could not save file: {}", path);
          if (this.options.printStacktraces()) LOGGER.throwing(e);
        }

        context.getFiles().remove(path);
      });
    }
  }
}
