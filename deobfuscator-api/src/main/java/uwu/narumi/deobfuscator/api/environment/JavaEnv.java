package uwu.narumi.deobfuscator.api.environment;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import uwu.narumi.deobfuscator.api.helper.PlatformType;
import uwu.narumi.deobfuscator.api.helper.SymLinks;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Tasks for Java environments.
 *
 * https://github.com/Col-E/Recaf-Launcher/blob/master/core/src/main/java/software/coley/recaf/launcher/task/JavaEnvTasks.java
 */
public class JavaEnv {
  private static final Set<JavaInstall> javaInstalls = new HashSet<>();

  /**
   * Must call {@link #scanForJavaInstalls()} before this list will be populated.
   *
   * @return Set of discovered Java installations.
   */
  @NotNull
  public static Collection<JavaInstall> getJavaInstalls() {
    return javaInstalls;
  }

  static {
    // On class-load, scan for java installs.
    scanForJavaInstalls();
  }

  /**
   * Detect common Java installations for the current platform.
   */
  private static void scanForJavaInstalls() {
    if (PlatformType.isWindows()) {
      scanForWindowsJavaPaths();
    } else if (PlatformType.isLinux()) {
      scanForLinuxJavaPaths();
    } else if (PlatformType.isMac()) {
      scanforMacJavaPaths();
    }
  }

  /**
   * Detect common Java installations on Linux.
   */
  private static void scanForLinuxJavaPaths() {
    // Check java alternative link.
    Path altJava = Paths.get("/etc/alternatives/java");
    if (Files.exists(altJava)) {
      addJavaInstall(altJava);
    }

    // Check home
    String homeEnv = System.getenv("JAVA_HOME");
    if (homeEnv != null) {
      Path homePath = Paths.get(homeEnv);
      if (Files.isDirectory(homePath)) {
        Path javaPath = homePath.resolve("bin/java");
        if (Files.exists(javaPath))
          addJavaInstall(javaPath);
      }
    }

    // Check common install locations.
    String[] javaRoots = {
        "/usr/lib/jvm/",
        System.getenv("HOME") + "/.jdks/"
    };
    for (String root : javaRoots) {
      Path rootPath = Paths.get(root);
      if (Files.isDirectory(rootPath)) {
        try (Stream<Path> subDirStream = Files.list(rootPath)) {
          subDirStream.filter(subDir -> Files.exists(subDir.resolve("bin/java")))
              .forEach(subDir -> {
                Path javaPath = subDir.resolve("bin/java");
                if (Files.exists(javaPath))
                  addJavaInstall(javaPath);
              });
        } catch (IOException ignored) {
          // Skip
        }
      }
    }
  }

  /**
   * Detect common Java installations on Mac.
   */
  private static void scanforMacJavaPaths() {
    Path[] jvmsRoots = new Path[]{
        Paths.get("/Library/Java/JavaVirtualMachines/"),
        Paths.get(System.getProperty("user.home")).resolve("Library/Java/JavaVirtualMachines/")
    };
    for (Path jvmsRoot : jvmsRoots) {
      if (Files.isDirectory(jvmsRoot)) {
        try (Stream<Path> stream = Files.walk(jvmsRoot)) {
          stream.forEach(path -> {
            if (path.toString().endsWith("bin/java"))
              addJavaMacInstall(path);
          });
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    }
  }

  /**
   * Detect common Java installations on Windows.
   */
  private static void scanForWindowsJavaPaths() {
    String homeProp = System.getProperty("java.home");
    if (homeProp != null)
      addJavaInstall(Paths.get(homeProp).resolve("bin/java.exe"));

    // Check java home
    String homeEnv = System.getenv("JAVA_HOME");
    if (homeEnv != null) {
      Path homePath = Paths.get(homeEnv);
      if (Files.isDirectory(homePath))
        addJavaInstall(homePath.resolve("bin/java.exe"));
    }

    // Check '%user%/.jdks'
    String homePath = System.getProperty("user.home");
    if (homePath != null) {
      Path jdksDir = Paths.get(homePath, ".jdks");
      if (Files.isDirectory(jdksDir))
        try (Stream<Path> subDirStream = Files.list(jdksDir)) {
          subDirStream.filter(subDir -> Files.exists(subDir.resolve("bin/java.exe")))
              .forEach(subDir -> addJavaInstall(subDir.resolve("bin/java.exe")));
        } catch (IOException ignored) {
          // Skip
        }
    }

    // Check system path for java entries.
    String path = System.getenv("PATH");
    if (path != null) {
      String[] entries = path.split(";");
      for (String entry : entries)
        if (entry.endsWith("bin"))
          addJavaInstall(Paths.get(entry).resolve("java.exe"));
    }

    // Check common install locations.
    String[] javaRoots = {
        "C:/Program Files/Amazon Corretto/",
        "C:/Program Files/Eclipse Adoptium/",
        "C:/Program Files/Eclipse Foundation/",
        "C:/Program Files/BellSoft/",
        "C:/Program Files/Java/",
        "C:/Program Files/Microsoft/",
        "C:/Program Files/SapMachine/JDK/",
        "C:/Program Files/Zulu/",
    };
    for (String root : javaRoots) {
      Path rootPath = Paths.get(root);
      if (Files.isDirectory(rootPath)) {
        try (Stream<Path> subDirStream = Files.list(rootPath)) {
          subDirStream.filter(subDir -> Files.exists(subDir.resolve("bin/java.exe")))
              .forEach(subDir -> addJavaInstall(subDir.resolve("bin/java.exe")));
        } catch (IOException ignored) {
          // Skip
        }
      }
    }
  }

  /**
   * @param javaExecutable Path to executable to add.
   * @return {@code true} when the path was recognized as a valid executable.
   * {@code false} when discarded.
   */
  @NotNull
  public static AdditionResult addJavaInstall(@NotNull Path javaExecutable) {
    return addJavaInstall(javaExecutable, executable -> {
      // Most installs are structured like: /whatever/jvms/openjdk-21.0.3/bin/java.exe
      // Thus, the parent of the bin directory has the name.
      Path binDir = executable.getParent();
      if (binDir == null)
        return null;
      Path jdkDir = binDir.getParent();
      if (jdkDir == null)
        return null;
      return jdkDir.getFileName().toString();
    });
  }

  /**
   * @param javaExecutable Path to executable to add.
   * @return {@code true} when the path was recognized as a valid executable.
   * {@code false} when discarded.
   */
  @NotNull
  public static AdditionResult addJavaMacInstall(@NotNull Path javaExecutable) {
    return addJavaInstall(javaExecutable, executable -> {
      // Mac structures things differently: /Library/Java/JavaVirtualMachines/openjdk-21.0.3.jdk/Contents/Home/bin/java.exe
      // Thus, going up 4 directory levels will reveal the name.
      Path binDir = executable.getParent();
      if (binDir == null)
        return null;
      Path jdkHomeDir = binDir.getParent();
      if (jdkHomeDir == null)
        return null;
      Path jdkContentsDir = jdkHomeDir.getParent();
      if (jdkContentsDir == null)
        return null;
      Path jdkDir = jdkContentsDir.getParent();
      if (jdkDir == null)
        return null;
      return jdkDir.getFileName().toString();
    });
  }

  /**
   * @param javaExecutable      Path to executable to add.
   * @param executableToJvmName Lookup to find JDK name from the path of the executable.
   * @return {@code true} when the path was recognized as a valid executable.
   * {@code false} when discarded.
   */
  @NotNull
  public static AdditionResult addJavaInstall(@NotNull Path javaExecutable, @NotNull Function<Path, String> executableToJvmName) {
    // Resolve sym-links
    if (Files.isSymbolicLink(javaExecutable)) {
      javaExecutable = SymLinks.resolveSymLink(javaExecutable);
      if (javaExecutable == null)
        return AdditionResult.ERR_RESOLVE_SYM_LINK;
    }

    // Validate executable is 'java' or 'javaw'
    String execName = javaExecutable.getFileName().toString();
    if (!execName.endsWith("java") && !javaExecutable.endsWith("java.exe")
        && !execName.endsWith("javaw") && !javaExecutable.endsWith("javaw.exe"))
      return AdditionResult.ERR_NOT_JAVA_EXEC;

    // Validate the given path points to a file that exists
    if (!Files.exists(javaExecutable))
      return AdditionResult.ERR_NOT_JAVA_EXEC;

    // Validate bin structure
    Path binDir = javaExecutable.getParent();
    if (binDir == null)
      return AdditionResult.ERR_PARENT;

    // Validate it's a JDK and not a JRE
    if (Files.notExists(binDir.resolve("javac")) && Files.notExists(binDir.resolve("javac.exe")))
      return AdditionResult.ERR_JRE_NOT_JDK;

    // Validate version
    String jdkDirName = executableToJvmName.apply(javaExecutable);
    if (jdkDirName == null)
      return AdditionResult.ERR_PARENT;
    int version = JavaVersion.fromVersionString(jdkDirName);
    if (version == JavaVersion.UNKNOWN_VERSION)
      return AdditionResult.ERR_UNRESOLVED_VERSION;
    if (version >= 8) {
      addJavaInstall(new JavaInstall(javaExecutable, version));
      return AdditionResult.SUCCESS;
    }
    return AdditionResult.ERR_TOO_OLD;
  }

  /**
   * @param path Path to executable to look up.
   * @return Install entry for path, or {@code null} if not previously recorded as a valid installation.
   */
  @Nullable
  public static JavaInstall getByPath(@NotNull Path path) {
    return javaInstalls.stream()
        .filter(i -> i.javaExecutable().equals(path))
        .findFirst().orElse(null);
  }

  private static void addJavaInstall(@NotNull JavaInstall install) {
    javaInstalls.add(install);
  }

  public enum AdditionResult {
    SUCCESS,
    ERR_NOT_JAVA_EXEC,
    ERR_RESOLVE_SYM_LINK,
    ERR_PARENT,
    ERR_JRE_NOT_JDK,
    ERR_UNRESOLVED_VERSION,
    ERR_TOO_OLD;

    public boolean wasSuccess() {
      return this == SUCCESS;
    }

    @NotNull
    public String message() {
      switch (this) {
        case SUCCESS:
          return "";
        case ERR_RESOLVE_SYM_LINK:
          return "The selected symbolic-link could not be resolved";
        case ERR_NOT_JAVA_EXEC:
          return "The selected file was not 'java' or 'javaw'";
        case ERR_UNRESOLVED_VERSION:
          return "The selected java executable could not have its version resolved";
        case ERR_PARENT:
          return "The selected java executable could not have its parent directories";
        case ERR_JRE_NOT_JDK:
          return "The selected java executable belongs to a JRE and not a JDK";
        case ERR_TOO_OLD:
          return "The selected java executable is from a outdated/unsupported version of Java";
      }
      return "The selected executable was not valid: " + name();
    }
  }
}
