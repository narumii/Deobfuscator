package uwu.narumi.deobfuscator.api.classpath;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.ClassNode;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class CombinedClassProvider implements ClassProvider {
  private final ClassProvider[] classProviders;

  public CombinedClassProvider(ClassProvider... classProviders) {
    this.classProviders = classProviders;
  }

  @Override
  public byte @Nullable [] getClass(String name) {
    for (ClassProvider classProvider : this.classProviders) {
      byte[] bytes = classProvider.getClass(name);
      if (bytes != null) return bytes;
    }
    return null;
  }

  @Override
  public byte @Nullable [] getFile(String path) {
    for (ClassProvider classProvider : this.classProviders) {
      byte[] bytes = classProvider.getFile(path);
      if (bytes != null) return bytes;
    }
    return null;
  }

  @Override
  public @Nullable ClassNode getClassInfo(String name) {
    for (ClassProvider classProvider : this.classProviders) {
      ClassNode classInfo = classProvider.getClassInfo(name);
      if (classInfo != null) return classInfo;
    }
    return null;
  }

  @Override
  public Collection<String> getLoadedClasses() {
    Set<String> classes = new HashSet<>();
    for (ClassProvider classProvider : this.classProviders) {
      classes.addAll(classProvider.getLoadedClasses());
    }
    return classes;
  }
}
