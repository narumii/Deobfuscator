package uwu.narumi.deobfuscator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import uwu.narumi.deobfuscator.helper.ClassHelper;
import uwu.narumi.deobfuscator.pool.ConstantPool;
import uwu.narumi.deobfuscator.transformer.Transformer;

public class Deobfuscator {

  private final ConcurrentMap<String, ConstantPool> constantPools = new ConcurrentHashMap<>();
  private final ConcurrentMap<String, ClassNode> classes = new ConcurrentHashMap<>();
  private final ConcurrentMap<String, byte[]> files = new ConcurrentHashMap<>();

  private final File input;
  private final File output;
  private final List<Transformer> transformers;

  private Deobfuscator(Builder builder) {
    if (builder.transformers == null) {
      throw new IllegalArgumentException("Please specify transformers");
    }

    this.input = new File(builder.input);
    this.output = new File(builder.output);
    this.transformers = Arrays.asList(builder.transformers);
    this.transformers.sort(Comparator.comparingInt(Transformer::weight));
  }

  public static Builder builder() {
    return new Builder();
  }

  public void start() {
    try {
      loadJar();
      transform();
      saveJar();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void loadJar() throws IOException {
    if (!input.exists()) {
      throw new IOException("Input file doesn't exists.");
    }

    System.out.println("Loading input file: " + input.getAbsolutePath());
    try (JarFile jarFile = new JarFile(input)) {
      Enumeration<JarEntry> enumeration = jarFile.entries();
      while (enumeration.hasMoreElements()) {
        JarEntry entry = enumeration.nextElement();
        try {
          byte[] bytes = jarFile.getInputStream(entry).readAllBytes();

          if (ClassHelper.isClass(entry.getName(), bytes)) {
            ClassNode classNode = new ClassNode();
            ClassReader classReader = new ClassReader(bytes);
            classReader.accept(classNode, ClassReader.SKIP_FRAMES);
            classes.put(classNode.name, classNode);
            constantPools.put(classNode.name, ClassHelper.getConstantPool(classReader));
          } else {
            files.put(entry.getName(), bytes);
          }
        } catch (Exception e) {
          System.out.printf("Can't load resource (%s): %s\n", entry.getName(), e);
        }
      }
    } catch (Exception e) {
      System.out.printf("Can't load input file (%s): %s\n", output, e);
    }

    System.out.println("Loaded input file: " + input.getAbsolutePath());
    System.out.println();
  }

  private void transform() {
    transformers.forEach(transformer -> {
      System.out.println();
      System.out.printf("Running %s\n", transformer.name());
      long start = System.currentTimeMillis();
      try {
        transformer.transform(this);
      } catch (Exception e) {
        System.out.printf("Error occurred while transforming (%s): %s\n", transformer.name(), e);
      }
      System.out.printf("Ended %s in %sms\n", transformer.name(),
          (System.currentTimeMillis() - start));
    });

    System.out.println();
  }

  private void saveJar() {
    System.out.println();

    if (output.exists()) {
      System.out.println("Output file exists, it will be replaced");
    }

    System.out.println("Saving output file: " + output.getAbsolutePath());
    try (JarOutputStream stream = new JarOutputStream(new FileOutputStream(output))) {
      stream.setLevel(9);

      for (Entry<String, ClassNode> entry : classes.entrySet()) {
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        entry.getValue().accept(classWriter);
        stream.putNextEntry(new ZipEntry(entry.getValue().name + ".class"));
        stream.write(classWriter.toByteArray());
      }

      for (Entry<String, byte[]> entry : files.entrySet()) {
        stream.putNextEntry(new ZipEntry(entry.getKey()));
        stream.write(entry.getValue());
      }
    } catch (Exception e) {
      System.out.printf("Error occurred while saving file (%s): %s\n", output, e);
    }
    System.out.println("Saved output file: " + output.getAbsolutePath());
  }

  public Collection<ClassNode> getClasses() {
    return classes.values();
  }

  public ConcurrentMap<String, ConstantPool> getConstantPools() {
    return constantPools;
  }

  public ConcurrentMap<String, ClassNode> getClassesEntry() {
    return classes;
  }

  public ConcurrentMap<String, byte[]> getFiles() {
    return files;
  }

  public static class Builder {

    private String input;
    private String output;

    private Transformer[] transformers;

    private Builder() {
    }

    public Builder input(String file) {
      this.input = file;
      this.output = file.replace(".jar", "-deobf.jar");
      return this;
    }

    public Builder output(String file) {
      this.output = file;
      return this;
    }

    public Builder with(Transformer... transformers) {
      this.transformers = transformers;
      return this;
    }

    public Deobfuscator build() {
      return new Deobfuscator(this);
    }
  }
}
