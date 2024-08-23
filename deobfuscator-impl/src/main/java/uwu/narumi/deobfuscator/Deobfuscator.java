package uwu.narumi.deobfuscator;

import dev.xdark.ssvm.VirtualMachine;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.xml.transform.TransformerException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.context.Context;
import uwu.narumi.deobfuscator.api.execution.SandBox;
import uwu.narumi.deobfuscator.api.helper.ClassHelper;
import uwu.narumi.deobfuscator.api.helper.FileHelper;
import uwu.narumi.deobfuscator.api.library.Library;
import uwu.narumi.deobfuscator.api.library.LibraryClassLoader;
import uwu.narumi.deobfuscator.api.library.LibraryClassWriter;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

public class Deobfuscator {

  private static final Logger LOGGER = LogManager.getLogger(Deobfuscator.class);

  private final Context context = new Context();

  private final List<Supplier<Transformer>> transformers = new ArrayList<>();
  @Nullable
  private final Path input;
  @Nullable
  private final Path output;
  private final List<Path> classes;
  private final int classReaderFlags;
  private final int classWriterFlags;
  private final boolean consoleDebug;

  private Deobfuscator(Builder builder) throws FileNotFoundException {
    if (builder.input == null && builder.classes.isEmpty()) {
      throw new FileNotFoundException("No input files provided");
    }

    if (builder.output != null && builder.output.toFile().exists())
      LOGGER.warn("Output file already exist, data will be overwritten");

    this.input = builder.input;
    this.output = builder.output;
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
    if (input != null) {
      LOGGER.info("Loading jar file: {}", input);
      // Load jar
      FileHelper.loadFilesFromZip(input, this::loadClass);
      LOGGER.info("Loaded jar file: {}\n", input);
    }

    for (Path clazz : classes) {
      LOGGER.info("Loading class: {}", clazz);

      try (InputStream inputStream = new FileInputStream(clazz.toFile())) {
        // Load class
        this.loadClass(clazz.toString(), inputStream.readAllBytes());

        LOGGER.info("Loaded class: {}\n", clazz);
      } catch (IOException e) {
        LOGGER.error("Could not load class: {}", clazz, e);
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

  private void saveOutput() {
    if (this.output == null) return;

    LOGGER.info("Saving output file: {}", output);

    try (ZipOutputStream zipOutputStream =
        new ZipOutputStream(new FileOutputStream(output.toFile()))) {
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
      LOGGER.error("Could not save output file: {}", output);
      LOGGER.debug("Error", e);
      if (consoleDebug) e.printStackTrace();
    }

    LOGGER.info("Saved output file: {}\n", output);
  }

  public static class Builder {

    private final Set<Path> libraries = new HashSet<>();
    @Nullable
    private Path input = null;
    @Nullable
    private Path output = null;
    private List<Path> classes = new ArrayList<>();
    private List<Supplier<Transformer>> transformers = List.of();

    private int classReaderFlags = ClassReader.SKIP_FRAMES;
    private int classWriterFlags = ClassWriter.COMPUTE_FRAMES;

    private boolean consoleDebug;

    private VirtualMachine virtualMachine;

    private Builder() {}

    public Builder input(@Nullable Path input) {
      this.input = input;
      if (this.input != null) {
        String fullName = input.getFileName().toString();
        int dot = fullName.lastIndexOf('.');
        this.output =
            input
                .getParent()
                .resolve(
                    dot == -1
                        ? fullName + "-out"
                        : fullName.substring(0, dot) + "-out" + fullName.substring(dot));
        this.libraries.add(input);
      }
      return this;
    }

    public Builder output(@Nullable Path output) {
      this.output = output;
      return this;
    }

    public Builder libraries(Path... paths) {
      this.libraries.addAll(List.of(paths));
      return this;
    }

    public Builder classes(Path... paths) {
      this.classes = Arrays.asList(paths);
      return this;
    }

    public Builder clazz(Path path) {
      this.classes.add(path);
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
}
