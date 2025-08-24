package uwu.narumi.deobfuscator.api.execution;

import dev.xdark.ssvm.classloading.BootClassFinder;
import dev.xdark.ssvm.classloading.ParsedClassData;
import dev.xdark.ssvm.util.ClassUtil;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * A {@link BootClassFinder} that finds classes in a given rt.jar file.
 */
public class JarBootClassFinder implements BootClassFinder {
  private final JarFile rtJarFile;

  public JarBootClassFinder(Path rtJarPath) {
    try {
      this.rtJarFile = new JarFile(rtJarPath.toFile());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public ParsedClassData findBootClass(String name) {
    // Find class in the rt.jar
    JarEntry jarEntry = this.rtJarFile.getJarEntry(name + ".class");
    if (jarEntry == null) {
      return null;
    }

    ClassReader cr;
    try (InputStream in = this.rtJarFile.getInputStream(jarEntry)) {
      if (in == null) {
        return null;
      }
      cr = new ClassReader(in);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    ClassNode node = ClassUtil.readNode(cr);
    return new ParsedClassData(cr, node);
  }
}
