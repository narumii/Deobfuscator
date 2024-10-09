package uwu.narumi.deobfuscator.api.classpath;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import uwu.narumi.deobfuscator.api.helper.ClassHelper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Classpath that fetches default JVM classes
 */
public class JvmClasspath {
  private static final Logger LOGGER = LogManager.getLogger();

  private static final Map<String, ClassNode> cache = new ConcurrentHashMap<>();

  @Nullable
  public static ClassNode getClassNode(String name) {
    if (cache.containsKey(name)) {
      return cache.get(name);
    }

    // Try to find it in classloader
    byte[] value = null;
    try (InputStream in = ClassLoader.getSystemResourceAsStream(name + ".class")) {
      if (in != null) {
        value = in.readAllBytes();
      }
    } catch (IOException ex) {
      LOGGER.error("Failed to fetch runtime bytecode of class: {}", name, ex);
    }

    if (value == null) return null;

    ClassNode classNode = ClassHelper.loadClassInfo(value);
    // Cache it!
    cache.put(name, classNode);

    return classNode;
  }
}
