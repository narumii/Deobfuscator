package uwu.narumi.deobfuscator.api.environment;

import java.nio.file.Path;
import java.util.Comparator;

/**
 * Model of a Java installation.
 *
 * @param javaExecutable Path to the Java executable.
 * @param version Major version of the installation.
 *
 * https://github.com/Col-E/Recaf-Launcher/blob/master/core/src/main/java/software/coley/recaf/launcher/info/JavaInstall.java
 */
public record JavaInstall(Path javaExecutable, int version) {
  /**
   * Compare installs by path.
   */
  public static Comparator<JavaInstall> COMPARE_PATHS = Comparator.comparing(o -> o.javaExecutable);
  /**
   * Compare installs by version <i>(newest first)</i>.
   */
  public static Comparator<JavaInstall> COMPARE_VERSIONS = (o1, o2) -> {
    // Negated so newer versions are sorted to be first
    int cmp = -Integer.compare(o1.version, o2.version);
    if (cmp == 0)
      return COMPARE_PATHS.compare(o1, o2);
    return cmp;
  };
}
