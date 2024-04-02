package uwu.narumi.deobfuscator;

import dev.xdark.ssvm.VirtualMachine;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.xml.transform.TransformerException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
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

  private final List<Transformer> transformers = new ArrayList<>();
  private final Path input;
  private final Path output;
  private final int classReaderFlags;
  private final int classWriterFlags;
  private final boolean consoleDebug;

  private Deobfuscator(Builder builder) throws FileNotFoundException {
    if (!builder.input.toFile().exists()) throw new FileNotFoundException(builder.input.toString());

    if (builder.output.toFile().exists())
      LOGGER.warn("Output file already exist, data will be overwritten");

    this.input = builder.input;
    this.output = builder.output;
    this.transformers.addAll(builder.transformers);
    this.classReaderFlags = builder.classReaderFlags;
    this.classWriterFlags = builder.classWriterFlags;
    this.consoleDebug = builder.consoleDebug;

    this.context.setLoader(
        new LibraryClassLoader(
            this.getClass().getClassLoader(),
            builder.libraries.stream().map(Library::new).toList()));

    try {
      this.context.setSandBox(
          new SandBox(
              this.context.getLoader(),
              builder.virtualMachine == null ? new VirtualMachine() : builder.virtualMachine));
    } catch (Throwable t) {
      LOGGER.error("SSVM bootstrap failed");
      LOGGER.debug("Error", t);
      if (consoleDebug) t.printStackTrace();

      this.context.setSandBox(null);
    }

    System.out.println();
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

  private void loadInput() {
    LOGGER.info("Loading input file: {}", input);

    FileHelper.loadFilesFromZip(
        input,
        (name, bytes) -> {
          try {
            if (ClassHelper.isClass(bytes)) {
              ClassHelper.loadClass(bytes, classReaderFlags)
                  .ifPresentOrElse(
                      classWrapper -> {
                        if (context.getClasses().containsKey(classWrapper.name())) return;

                        context.getClasses().put(classWrapper.name(), classWrapper);
                        context.getOriginalClasses().put(classWrapper.name(), classWrapper.clone());
                      },
                      () -> {
                        if (!context.getFiles().containsKey(name))
                          context.getFiles().put(name, bytes);
                      });
            } else if (!context.getFiles().containsKey(name)) {
              context.getFiles().put(name, bytes);
            }
          } catch (Exception e) {
            LOGGER.error("Could not load class: {}, adding as file", name);
            LOGGER.debug("Error", e);
            context.getFiles().put(name, bytes);

            if (consoleDebug) e.printStackTrace();
          }
        });

    LOGGER.info("Loaded input file: {}\n", input);
  }

  public void transform(List<Transformer> transformers) {
    if (transformers == null || transformers.isEmpty()) return;

    transformers.forEach(
        transformer -> {
          LOGGER.info("-------------------------------------");
          try {
            LOGGER.info("Running {} transformer", transformer.name());
            long start = System.currentTimeMillis();
            transformer.transform(null, context);
            LOGGER.info(
                "Ended {} transformer in {} ms",
                transformer.name(),
                (System.currentTimeMillis() - start));
          } catch (TransformerException e) {
            LOGGER.error("! {}: {}", transformer.name(), e.getMessage());

            if (consoleDebug) e.printStackTrace();
          } catch (Exception e) {
            LOGGER.error("Error occurred when transforming {}", transformer.name());
            LOGGER.debug("Error", e);

            if (consoleDebug) e.printStackTrace();
          }
          LOGGER.info("-------------------------------------\n");
        });
  }

  private void saveOutput() {
    LOGGER.info("Saving output file: {}", output);

    try (ZipOutputStream zipOutputStream =
        new ZipOutputStream(new FileOutputStream(output.toFile()))) {
      zipOutputStream.setLevel(9);

      context
          .getClasses()
          .forEach(
              (ignored, classWrapper) -> {
                try {
                  ClassWriter classWriter =
                      new LibraryClassWriter(classWriterFlags, context.getLoader());
                  classWrapper.getClassNode().accept(classWriter);

                  byte[] data = classWriter.toByteArray();
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
    private Path input = Path.of("input.jar");
    private Path output = Path.of("output.jar");
    private List<Transformer> transformers;

    private int classReaderFlags = ClassReader.EXPAND_FRAMES;
    private int classWriterFlags = ClassWriter.COMPUTE_MAXS;

    private boolean consoleDebug;

    private VirtualMachine virtualMachine;

    private Builder() {}

    public Builder input(Path input) {
      this.input = input;
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
      return this;
    }

    public Builder output(Path output) {
      this.output = output;
      return this;
    }

    public Builder libraries(Path... paths) {
      this.libraries.addAll(List.of(paths));
      return this;
    }

    public Builder transformers(Transformer... transformers) {
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
