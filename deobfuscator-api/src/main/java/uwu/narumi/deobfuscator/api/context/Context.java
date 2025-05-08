package uwu.narumi.deobfuscator.api.context;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import software.coley.cafedude.InvalidClassException;
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.asm.FieldRef;
import uwu.narumi.deobfuscator.api.asm.MethodContext;
import uwu.narumi.deobfuscator.api.asm.MethodRef;
import uwu.narumi.deobfuscator.api.classpath.ClassProvider;
import uwu.narumi.deobfuscator.api.classpath.ClassInfoStorage;
import uwu.narumi.deobfuscator.api.classpath.CombinedClassProvider;
import uwu.narumi.deobfuscator.api.classpath.JvmClassProvider;
import uwu.narumi.deobfuscator.api.execution.SandBox;
import uwu.narumi.deobfuscator.api.helper.ClassHelper;

public class Context implements ClassProvider {

  private static final Logger LOGGER = LogManager.getLogger();

  private final Map<String, ClassWrapper> classesMap = new ConcurrentHashMap<>();
  private final Map<String, byte[]> filesMap = new ConcurrentHashMap<>();

  private final DeobfuscatorOptions options;

  private final ClassInfoStorage compiledClasses;
  private final ClassInfoStorage libraries;

  private SandBox globalSandBox = null;

  /**
   * Creates a new {@link Context} instance from its options
   *
   * @param options Deobfuscator options
   * @param compiledClasses {@link ClassInfoStorage} that holds the original classes of the primary jar
   * @param libraries {@link ClassInfoStorage} that holds the libraries' classes
   */
  public Context(DeobfuscatorOptions options, ClassInfoStorage compiledClasses, ClassInfoStorage libraries) {
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
  public ClassInfoStorage getCompiledClasses() {
    return compiledClasses;
  }

  /**
   * Class storage that holds libraries' classes
   */
  public ClassInfoStorage getLibraries() {
    return libraries;
  }

  public Collection<ClassWrapper> classes() {
    return classesMap.values();
  }

  public void removeMethod(MethodRef methodRef) {
    ClassWrapper classWrapper = this.getClassesMap().get(methodRef.owner());
    classWrapper.methods().removeIf(methodNode -> methodNode.name.equals(methodRef.name()) && methodNode.desc.equals(methodRef.desc()));
  }

  public void removeField(FieldRef fieldRef) {
    ClassWrapper classWrapper = this.getClassesMap().get(fieldRef.owner());
    classWrapper.fields().removeIf(fieldNode -> fieldNode.name.equals(fieldRef.name()) && fieldNode.desc.equals(fieldRef.desc()));
  }

  public Optional<MethodContext> getMethodContext(MethodRef methodRef) {
    ClassWrapper classWrapper = this.getClassesMap().get(methodRef.owner());
    if (classWrapper == null) return Optional.empty();
    Optional<MethodNode> methodNode = classWrapper.findMethod(methodRef);

    return methodNode.map(node -> MethodContext.of(classWrapper, node));
  }

  @UnmodifiableView
  public List<ClassWrapper> scopedClasses(ClassWrapper scope) {
    return classesMap.values().stream()
        .filter(classWrapper -> scope == null || classWrapper.name().equals(scope.name()))
        .toList();
  }

  public void addCompiledClass(String pathInJar, byte[] bytes) {
    try {
      // Fix class bytes
      bytes = ClassHelper.fixClass(bytes);

      // Class is always a file, not a directory. Remove last slash if it exists
      pathInJar = pathInJar.replaceAll("/$", "");

      ClassWrapper classWrapper = ClassHelper.loadClass(pathInJar, bytes, ClassReader.SKIP_FRAMES);
      this.classesMap.putIfAbsent(classWrapper.name(), classWrapper);

      this.compiledClasses.addRawClass(bytes);
    } catch (InvalidClassException e) {
      LOGGER.error("Failed to load class {}", pathInJar);
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

  /**
   * Gets the class provider that holds all classes in the context, including the jvm runtime.
   */
  public ClassProvider getFullClassProvider() {
    return new CombinedClassProvider(this, this.getLibraries(), JvmClassProvider.INSTANCE);
  }
}
