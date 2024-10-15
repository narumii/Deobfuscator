package uwu.narumi.deobfuscator.api.execution;

import dev.xdark.ssvm.classloading.SupplyingClassLoaderInstaller;
import uwu.narumi.deobfuscator.api.classpath.ClassProvider;

public class ClasspathDataSupplier implements SupplyingClassLoaderInstaller.DataSupplier {

  private final ClassProvider classpath;

  public ClasspathDataSupplier(ClassProvider classpath) {
    this.classpath = classpath;
  }

  @Override
  public byte[] getClass(String className) {
    return classpath.getClass(className.replace('.', '/'));
  }

  @Override
  public byte[] getResource(String resourcePath) {
    return classpath.getFile(resourcePath);
  }
}
