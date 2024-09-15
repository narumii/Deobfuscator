package uwu.narumi.deobfuscator.api.execution;

import dev.xdark.ssvm.classloading.SupplyingClassLoaderInstaller;
import uwu.narumi.deobfuscator.api.classpath.ClassPath;

public class ClassPathDataSupplier implements SupplyingClassLoaderInstaller.DataSupplier {

  private final ClassPath classPath;

  public ClassPathDataSupplier(ClassPath classPath) {
    this.classPath = classPath;
  }

  @Override
  public byte[] getClass(String className) {
    return classPath.getClasses().get(className.replace('.', '/'));
  }

  @Override
  public byte[] getResource(String resourcePath) {
    return classPath.getFiles().get(resourcePath);
  }
}
