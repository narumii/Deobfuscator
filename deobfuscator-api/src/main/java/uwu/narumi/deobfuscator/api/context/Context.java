package uwu.narumi.deobfuscator.api.context;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.execution.SandBox;
import uwu.narumi.deobfuscator.api.classpath.Classpath;

public class Context {

  private static final Logger LOGGER = LogManager.getLogger(Context.class);

  private final Map<String, ClassWrapper> classes = new ConcurrentHashMap<>();
  private final Map<String, byte[]> files = new ConcurrentHashMap<>();

  private final DeobfuscatorOptions options;

  private final Classpath primaryClasspath;
  private final Classpath libClasspath;
  private final Classpath combinedClasspath;

  private SandBox globalSandBox = null;

  /**
   * Creates a new {@link Context} instance from its options
   *
   * @param options Deobfuscator options
   * @param primaryClasspath Classpath which has only primary jar in it
   * @param libClasspath Classpath filled with libs
   */
  public Context(DeobfuscatorOptions options, Classpath primaryClasspath, Classpath libClasspath) {
    this.options = options;

    this.primaryClasspath = primaryClasspath;
    this.libClasspath = libClasspath;
    this.combinedClasspath = Classpath.builder()
        .addClasspath(primaryClasspath)
        .addClasspath(libClasspath)
        .build();
  }

  /**
   * Gets sandbox or creates if it does not exist.
   */
  public SandBox getSandBox() {
    if (this.globalSandBox == null) {
      // Lazily load sandbox
      this.globalSandBox = new SandBox(this);
    }
    return this.globalSandBox;
  }

  public DeobfuscatorOptions getOptions() {
    return options;
  }

  /**
   * Classpath for primary jar
   */
  public Classpath getPrimaryClasspath() {
    return primaryClasspath;
  }

  /**
   * Classpath filled with libs
   */
  public Classpath getLibClasspath() {
    return libClasspath;
  }

  /**
   * {@link #getPrimaryClasspath()} and {@link #getLibClasspath()} combined
   */
  public Classpath getCombinedClasspath() {
    return this.combinedClasspath;
  }

  public Collection<ClassWrapper> classes() {
    return classes.values();
  }

  public Stream<ClassWrapper> stream() {
    return classes.values().stream();
  }

  public Stream<ClassWrapper> stream(ClassWrapper scope) {
    return classes.values().stream()
        .filter(classWrapper -> scope == null || classWrapper.name().equals(scope.name()));
  }

  public List<ClassWrapper> classes(ClassWrapper scope) {
    return classes.values().stream()
        .filter(classWrapper -> scope == null || classWrapper.name().equals(scope.name()))
        .collect(Collectors.toList());
  }

  public Optional<ClassWrapper> get(String name) {
    return Optional.ofNullable(classes.get(name));
  }

  public Optional<ClassWrapper> remove(ClassWrapper classWrapper) {
    return remove(classWrapper.name());
  }

  public Optional<ClassWrapper> remove(String name) {
    return Optional.ofNullable(classes.remove(name));
  }

  public Map<String, ClassWrapper> getClasses() {
    return classes;
  }

  public Map<String, byte[]> getFiles() {
    return files;
  }
}
