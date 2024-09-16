package uwu.narumi.deobfuscator.api.execution;

import dev.xdark.ssvm.classloading.SupplyingClassLoaderInstaller;
import uwu.narumi.deobfuscator.api.classpath.Classpath;

public class ClasspathDataSupplier implements SupplyingClassLoaderInstaller.DataSupplier {

  private final Classpath classPath;

  public ClasspathDataSupplier(Classpath classpath) {
    this.classPath = classpath;
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
