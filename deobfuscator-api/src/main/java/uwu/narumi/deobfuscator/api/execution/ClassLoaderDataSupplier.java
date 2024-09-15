package uwu.narumi.deobfuscator.api.execution;

import dev.xdark.ssvm.classloading.SupplyingClassLoaderInstaller;
import uwu.narumi.deobfuscator.api.library.ClassPathClassLoader;

public class ClassLoaderDataSupplier implements SupplyingClassLoaderInstaller.DataSupplier {

  private final ClassPathClassLoader loader;

  public ClassLoaderDataSupplier(ClassPathClassLoader loader) {
    this.loader = loader;
  }

  @Override
  public byte[] getClass(String className) {
    return loader.fetchRaw(className.replace('.', '/')).orElse(null);
  }

  @Override
  public byte[] getResource(String resourcePath) {
    return loader.fetchFile(resourcePath).orElse(null);
  }
}
