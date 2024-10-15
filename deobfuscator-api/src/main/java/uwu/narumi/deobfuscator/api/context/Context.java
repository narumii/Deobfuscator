package uwu.narumi.deobfuscator.api.context;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import software.coley.cafedude.InvalidClassException;
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.classpath.ClassProvider;
import uwu.narumi.deobfuscator.api.classpath.ClassStorage;
import uwu.narumi.deobfuscator.api.execution.SandBox;
import uwu.narumi.deobfuscator.api.helper.ClassHelper;

public class Context implements ClassProvider {

  private final Map<String, ClassWrapper> classesMap = new ConcurrentHashMap<>();
  private final Map<String, byte[]> filesMap = new ConcurrentHashMap<>();

  private final DeobfuscatorOptions options;

  private final ClassStorage compiledClasses;
  private final ClassStorage libraries;

  private SandBox globalSandBox = null;

  /**
   * Creates a new {@link Context} instance from its options
   *
   * @param options Deobfuscator options
   * @param compiledClasses {@link ClassStorage} that holds the original classes of the primary jar
   * @param libraries {@link ClassStorage} that holds the libraries' classes
   */
  public Context(DeobfuscatorOptions options, ClassStorage compiledClasses, ClassStorage libraries) {
    this.options = options;

    this.compiledClasses = compiledClasses;
    this.libraries = libraries;
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
   * Class storage that holds already compiled classes from original jar
   */
  public ClassStorage getCompiledClasses() {
    return compiledClasses;
  }

  /**
   * Class storage that holds libraries' classes
   */
  public ClassStorage getLibraries() {
    return libraries;
  }

  public Collection<ClassWrapper> classes() {
    return classesMap.values();
  }

  @UnmodifiableView
  public List<ClassWrapper> scopedClasses(ClassWrapper scope) {
    return classesMap.values().stream()
        .filter(classWrapper -> scope == null || classWrapper.name().equals(scope.name()))
        .toList();
  }

  public void addCompiledClass(String pathInJar, byte[] bytes) {
    try {
      ClassWrapper classWrapper = ClassHelper.loadUnknownClass(pathInJar, bytes, ClassReader.SKIP_FRAMES);
      this.classesMap.putIfAbsent(classWrapper.name(), classWrapper);
      this.compiledClasses.addRawClass(bytes);
    } catch (InvalidClassException e) {
      throw new RuntimeException(e);
    }
  }

  public void addFile(String path, byte[] bytes) {
    this.filesMap.put(path, bytes);
    this.compiledClasses.files().put(path, bytes);
  }

  @Override
  public byte @Nullable [] getClass(String name) {
    // Not implemented because it would need to compile class which is CPU intensive
    return null;
  }

  @Override
  public byte @Nullable [] getFile(String path) {
    return filesMap.get(path);
  }

  @Override
  public @Nullable ClassNode getClassInfo(String name) {
    ClassWrapper classWrapper = classesMap.get(name);
    if (classWrapper == null) return null;
    return classWrapper.classNode();
  }

  @Override
  public Collection<String> getLoadedClasses() {
    return this.classesMap.keySet();
  }

  public Map<String, ClassWrapper> getClassesMap() {
    return classesMap;
  }

  public Map<String, byte[]> getFilesMap() {
    return filesMap;
  }
}
