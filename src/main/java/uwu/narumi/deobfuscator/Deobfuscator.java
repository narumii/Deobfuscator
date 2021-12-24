package uwu.narumi.deobfuscator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import uwu.narumi.deobfuscator.exception.TransformerException;
import uwu.narumi.deobfuscator.helper.ClassHelper;
import uwu.narumi.deobfuscator.helper.FileHelper;
import uwu.narumi.deobfuscator.sandbox.SandBox;
import uwu.narumi.deobfuscator.transformer.Transformer;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Deobfuscator {

    private static final Logger LOGGER = LogManager.getLogger(Deobfuscator.class);

    private final Map<String, ClassNode> classes = new ConcurrentHashMap<>();
    private final Map<String, ClassNode> originalClasses = new ConcurrentHashMap<>();
    private final Map<String, byte[]> files = new ConcurrentHashMap<>();

    private final Path input;
    private final Path output;
    private final List<Transformer> transformers;
    private final int classReaderFlags;
    private final int classWriterFlags;
    private final boolean consoleDebug;

    private Deobfuscator(Builder builder) throws FileNotFoundException {
        if (!builder.input.toFile().exists())
            throw new FileNotFoundException(builder.input.toString());

        if (builder.output.toFile().exists())
            LOGGER.warn("Output file already exist, data will be overwritten");

        this.input = builder.input;
        this.output = builder.output;
        this.transformers = builder.transformers;
        this.classReaderFlags = builder.classReaderFlags;
        this.classWriterFlags = builder.classWriterFlags;
        this.consoleDebug = builder.consoleDebug;

        SandBox.getInstance(); //YES
        //System.setSecurityManager(new SandBoxSecurityManager()); //disabled due to deobfuscation errors
        LOGGER.error("SecurityManager is disabled due to deobfuscation errors, transformers that using SandBox can be exploited");
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

            if (consoleDebug)
                e.printStackTrace();
        }
    }

    private void loadInput() {
        LOGGER.info("Loading input file: {}", input);

        FileHelper.loadFilesFromZip(input.toString()).forEach((name, data) -> {
            try {
                if (ClassHelper.isClass(name, data)) {
                    ClassNode classNode = ClassHelper.loadClass(data, classReaderFlags);
                    classes.put(classNode.name, classNode);
                    originalClasses.put(classNode.name, ClassHelper.copy(classNode)); //yes
                } else {
                    files.put(name, data);
                }
            } catch (Exception e) {
                LOGGER.error("Could not load class: {}, adding as file", name);
                LOGGER.debug("Error", e);
                files.put(name, data);

                if (consoleDebug)
                    e.printStackTrace();
            }
        });

        LOGGER.info("Loaded input file: {}\n", input);
    }

    public void transform(List<Transformer> transformers) {
        if (transformers == null || transformers.isEmpty())
            return;

        transformers.forEach(transformer -> {
            LOGGER.info("-------------------------------------");
            try {
                LOGGER.info("Running {} transformer", transformer.name());
                long start = System.currentTimeMillis();
                transformer.transform(this);
                LOGGER.info("Ended {} transformer in {} ms", transformer.name(), (System.currentTimeMillis() - start));
            } catch (TransformerException e) {
                LOGGER.error("! {}: {}", transformer.name(), e.getMessage());

                if (consoleDebug)
                    e.printStackTrace();
            } catch (Exception e) {
                LOGGER.error("Error occurred when transforming {}", transformer.name());
                LOGGER.debug("Error", e);

                if (consoleDebug)
                    e.printStackTrace();
            }
            LOGGER.info("-------------------------------------\n");
        });
    }

    private void saveOutput() {
        LOGGER.info("Saving output file: {}", output);

        try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(output.toFile()))) {
            zipOutputStream.setLevel(9);

            classes.forEach((ignored, classNode) -> {
                try {
                    byte[] data = ClassHelper.classToBytes(classNode, classWriterFlags);
                    zipOutputStream.putNextEntry(new ZipEntry(classNode.name + ".class"));
                    zipOutputStream.write(data);
                } catch (Exception e) {
                    LOGGER.error("Could not save class, saving original class instead of deobfuscated: {}", classNode.name);
                    LOGGER.debug("Error", e);

                    if (consoleDebug)
                        e.printStackTrace();
                    try {
                        byte[] data = ClassHelper.classToBytes(originalClasses.get(classNode.name), classWriterFlags);

                        zipOutputStream.putNextEntry(new ZipEntry(classNode.name + ".class"));
                        zipOutputStream.write(data);
                    } catch (Exception e2) {
                        LOGGER.error("Could not save original class: {}", classNode.name);
                        LOGGER.debug("Error", e2);

                        if (consoleDebug)
                            e2.printStackTrace();
                    }
                }

                originalClasses.remove(classNode.name);
                classes.remove(ignored);
            });

            files.forEach((name, data) -> {
                try {
                    zipOutputStream.putNextEntry(new ZipEntry(name));
                    zipOutputStream.write(data);
                } catch (Exception e) {
                    LOGGER.error("Could not save file: {}", name);
                    LOGGER.debug("Error", e);

                    if (consoleDebug)
                        e.printStackTrace();
                }

                files.remove(name);
            });
        } catch (Exception e) {
            LOGGER.error("Could not save output file: {}", output);
            LOGGER.debug("Error", e);
            if (consoleDebug)
                e.printStackTrace();

        }

        LOGGER.info("Saved output file: {}\n", output);
    }

    public Map<String, ClassNode> getClasses() {
        return classes;
    }

    public Map<String, ClassNode> getOriginalClasses() {
        return originalClasses;
    }

    public Collection<ClassNode> classes() {
        return classes.values();
    }

    public Map<String, byte[]> getFiles() {
        return files;
    }

    public int getClassReaderFlags() {
        return classReaderFlags;
    }

    public int getClassWriterFlags() {
        return classWriterFlags;
    }

    public Path getInput() {
        return input;
    }

    public Path getOutput() {
        return output;
    }

    public static class Builder {

        private Path input = Path.of("input.jar");
        private Path output = Path.of("output.jar");

        private List<Transformer> transformers;

        private int classReaderFlags = ClassReader.EXPAND_FRAMES;
        private int classWriterFlags = ClassWriter.COMPUTE_MAXS;

        private boolean consoleDebug;

        private Builder() {
        }

        public Builder input(Path input) {
            this.input = input;
            return this;
        }

        public Builder output(Path output) {
            this.output = output;
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

        public Deobfuscator build() throws FileNotFoundException {
            return new Deobfuscator(this);
        }
    }
}
