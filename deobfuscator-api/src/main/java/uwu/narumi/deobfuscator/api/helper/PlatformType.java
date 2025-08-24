package uwu.narumi.deobfuscator.api.helper;

/**
 * Operating system enumeration.
 *
 * https://github.com/Col-E/Recaf-Launcher/blob/master/core/src/main/java/software/coley/recaf/launcher/info/PlatformType.java
 */
public enum PlatformType {
  WINDOWS,
  MAC,
  LINUX;

  /**
   * @return {@code true} when the current platform is windows.
   */
  public static boolean isWindows() {
    return get() == WINDOWS;
  }

  /**
   * @return {@code true} when the current platform is mac.
   */
  public static boolean isMac() {
    return get() == MAC;
  }

  /**
   * @return {@code true} when the current platform is linux.
   */
  public static boolean isLinux() {
    return get() == LINUX;
  }

  /**
   * @return Operating system type.
   */
  public static PlatformType get() {
    String osName = System.getProperty("os.name").toLowerCase();
    if (osName.contains("win"))
      return WINDOWS;
    if (osName.contains("mac") || osName.contains("osx"))
      return MAC;
    return LINUX;
  }
}
