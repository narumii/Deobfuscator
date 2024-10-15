package uwu.narumi.deobfuscator.api.classpath;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.ClassNode;
import software.coley.cafedude.InvalidClassException;
import uwu.narumi.deobfuscator.api.helper.ClassHelper;
import uwu.narumi.deobfuscator.api.helper.FileHelper;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClassStorage implements ClassProvider {
  private final Map<String, byte[]> compiledClasses = new ConcurrentHashMap<>();
  private final Map<String, byte[]> files = new ConcurrentHashMap<>();

  private final Map<String, ClassNode> classesInfo = new ConcurrentHashMap<>();

  /**
   * Adds jar to class storage
   *
   * @param jarPath Jar path
   */
  public void addJar(@NotNull Path jarPath) {
    FileHelper.loadFilesFromZip(jarPath, (classPath, bytes) -> {
      if (!ClassHelper.isClass(classPath, bytes)) {
        files.putIfAbsent(classPath, bytes);
        return;
      }

      addRawClass(bytes);
    });
  }

  public void addRawClass(byte[] bytes) {
    try {
      ClassNode classNode = ClassHelper.loadUnknownClassInfo(bytes);
      String className = classNode.name;

      // Add class to class storage
      compiledClasses.putIfAbsent(className, bytes);
      classesInfo.putIfAbsent(className, classNode);
    } catch (InvalidClassException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public byte @Nullable [] getClass(String name) {
    return compiledClasses.get(name);
  }

  @Override
  public byte @Nullable [] getFile(String path) {
    return files.get(path);
  }

  @Override
  public @Nullable ClassNode getClassInfo(String name) {
    return classesInfo.get(name);
  }

  @Override
  public Collection<String> getLoadedClasses() {
    return compiledClasses.keySet();
  }

  public Map<String, byte[]> compiledClasses() {
    return compiledClasses;
  }

  public Map<String, byte[]> files() {
    return files;
  }

  public Map<String, ClassNode> classesInfo() {
    return classesInfo;
  }
}
