package uwu.narumi.deobfuscator.api.execution;

import dev.xdark.ssvm.classloading.SupplyingClassLoaderInstaller;
import uwu.narumi.deobfuscator.api.library.LibraryClassLoader;

public class ClassLoaderDataSupplier implements SupplyingClassLoaderInstaller.DataSupplier {

  private final LibraryClassLoader loader;

  public ClassLoaderDataSupplier(LibraryClassLoader loader) {
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
