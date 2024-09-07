package uwu.narumi.deobfuscator.api.execution;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.context.Context;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Secure sandbox class loader that utilizes {@link SecurityManager} and {@link SandboxPolicy} to block
 * dangerous operations like {@link System#exit(int)}, file operations or command execution
 */
public class SandboxClassLoader extends ClassLoader {

  protected static final Logger LOGGER = LogManager.getLogger(SandboxClassLoader.class);

  /*private static final List<String> TRUSTED_CLASSES = List.of(
      "java.lang.Object",
      "java.util.Collection",
      "java.util.Vector",
      "java.util.ArrayList",
      "java.util.concurrent.ConcurrentHashMap",
      "java.lang.System" // Whitelist
  );*/

  //private Map<String, byte[]> classes = new HashMap<>(); // Canonical class name -> class bytes

  private final Context context;
  private final List<String> loadedCustomClasses = new ArrayList<>();

  public SandboxClassLoader(Context context) {
    if (System.getSecurityManager() == null) {
      LOGGER.error("SecurityManager is not initialized. It mandatory to secure you from running arbitrary code on your machine. Please follow the setup tutorial in the README.md");
      throw new SecurityException("SecurityManager is not initialized");
    }
    this.context = context;
  }

  @Override
  protected Class<?> findClass(String name) throws ClassNotFoundException {
    LOGGER.info("Loading class {}", name);
    String internalName = name.replace('.', '/');
    ClassWrapper classWrapper = context.getClasses().get(internalName);
    if (classWrapper != null) {
      byte[] classBytes = classWrapper.compileToBytes(this.context);
      Class<?> clazz = defineClass(name, classBytes, 0, classBytes.length);
      this.loadedCustomClasses.add(internalName);
      return clazz;
    }
    return super.findClass(name);
  }

  /*@Override
  protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
    System.out.println("loadclass: "+name);

    if (TRUSTED_CLASSES.contains(name) || context.getClasses().containsKey(name.replace('.', '/'))) {
      return super.loadClass(name, resolve);
    }
    throw new ClassNotFoundException("Tried to load untrusted class '"+name+"'. The execution has been stopped immediately.");
  }*/

  @Override
  public @Nullable InputStream getResourceAsStream(String name) {
    throw new RuntimeException("Tried to load resource in sandbox: "+name);
  }

  public List<String> getLoadedCustomClasses() {
    return loadedCustomClasses;
  }
}
