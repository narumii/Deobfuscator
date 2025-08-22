package uwu.narumi.deobfuscator.api.environment;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Supported Java version of the current JVM.
 *
 * https://github.com/Col-E/Recaf-Launcher/blob/master/core/src/main/java/software/coley/recaf/launcher/info/JavaVersion.java
 */
public class JavaVersion {
  /**
   * The offset from which a version and the version constant value is. For example, Java 8 is 52 <i>(44 + 8)</i>.
   */
  public static final int VERSION_OFFSET = 44;
  /**
   * Code indicator that we couldn't figure out the version.
   */
  public static final int UNKNOWN_VERSION = -2;
  /**
   * Regex pattern which extracts the major release version from a string.
   * Ignores most common suffix/prefix patterns.
   */
  private static final Pattern JAVA_VERSION_EXTRACTOR = Pattern.compile("(?:(?:[^\\d\\W]|[- ])+)?(?:1\\D)?(\\d+)(?:_.+)?(?:\\..+)?");
  private static final String JAVA_CLASS_VERSION = "java.class.version";
  private static final String JAVA_VM_SPEC_VERSION = "java.vm.specification.version";
  private static int version = -1;

  /**
   * @param version
   * 		Version string.
   *
   * @return Version if parsable, otherwise {@link #UNKNOWN_VERSION}.
   */
  public static int fromVersionString(@NotNull String version) {
    try {
      Matcher matcher = JAVA_VERSION_EXTRACTOR.matcher(version);
      if (matcher.find())
        return Integer.parseInt(matcher.group(1));
    } catch (Exception ignored) {
      // ignored
    }
    return UNKNOWN_VERSION;
  }

  /**
   * Get the supported Java version of the current JVM.
   *
   * @return Version. If normal detection means do not suffice, then {@link #UNKNOWN_VERSION}.
   */
  public static int get() {
    if (version == -1) {
      // Check for class version
      String property = System.getProperty(JAVA_CLASS_VERSION, "");
      if (!property.isEmpty())
        return version = (int) (Float.parseFloat(property) - VERSION_OFFSET);

      // Odd, not found. Try the spec version
      property = System.getProperty(JAVA_VM_SPEC_VERSION, "");
      if (property.contains("."))
        return version = (int) Float.parseFloat(property.substring(property.indexOf('.') + 1));
      else if (!property.isEmpty())
        return version = Integer.parseInt(property);

      // Very odd
      return version = UNKNOWN_VERSION;
    }
    return version;
  }
}
