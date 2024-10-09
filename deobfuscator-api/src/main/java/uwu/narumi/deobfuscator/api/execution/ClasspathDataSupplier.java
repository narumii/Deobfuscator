package uwu.narumi.deobfuscator.api.execution;

import dev.xdark.ssvm.classloading.SupplyingClassLoaderInstaller;
import uwu.narumi.deobfuscator.api.classpath.Classpath;

public class ClasspathDataSupplier implements SupplyingClassLoaderInstaller.DataSupplier {

  private final Classpath classpath;

  public ClasspathDataSupplier(Classpath classpath) {
    this.classpath = classpath;
  }

  @Override
  public byte[] getClass(String className) {
    return classpath.rawClasses().get(className.replace('.', '/'));
  }

  @Override
  public byte[] getResource(String resourcePath) {
    return classpath.files().get(resourcePath);
  }
}
