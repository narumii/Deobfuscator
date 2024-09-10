package uwu.narumi.deobfuscator.api.context;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import dev.xdark.ssvm.VirtualMachine;
import dev.xdark.ssvm.execution.VMException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.execution.SandBox;
import uwu.narumi.deobfuscator.api.library.LibraryClassLoader;

public class Context {

  private static final Logger LOGGER = LogManager.getLogger(Context.class);

  private final Map<String, ClassWrapper> classes = new ConcurrentHashMap<>();
  private final Map<String, byte[]> originalClasses = new ConcurrentHashMap<>();
  private final Map<String, byte[]> files = new ConcurrentHashMap<>();

  private final DeobfuscatorOptions options;
  private final LibraryClassLoader libraryLoader;

  private SandBox sandBox = null;

  public Context(DeobfuscatorOptions options, LibraryClassLoader libraryLoader) {
    this.options = options;
    this.libraryLoader = libraryLoader;
  }

  /**
   * Gets sandbox or creates if it does not exist.
   */
  public SandBox getSandBox() {
    if (this.sandBox == null) {
      // Lazily load sandbox
      VirtualMachine vm = options.virtualMachine() == null ? new VirtualMachine() : options.virtualMachine();
      try {
        this.sandBox = new SandBox(this.libraryLoader, vm);
      } catch (VMException ex) {
        LOGGER.error("SSVM bootstrap failed. Make sure that you run this deobfuscator on java 17");
        SandBox.logVMException(ex, vm);

        throw new RuntimeException(ex);
      }
    }
    return this.sandBox;
  }

  public DeobfuscatorOptions getOptions() {
    return options;
  }

  public LibraryClassLoader getLibraryLoader() {
    return libraryLoader;
  }

  public Collection<ClassWrapper> classes() {
    return classes.values();
  }

  public Stream<ClassWrapper> stream() {
    return classes.values().stream();
  }

  public Stream<ClassWrapper> stream(ClassWrapper scope) {
    return classes.values().stream()
        .filter(classWrapper -> scope == null || classWrapper.name().equals(scope.name()));
  }

  public List<ClassWrapper> classes(ClassWrapper scope) {
    return classes.values().stream()
        .filter(classWrapper -> scope == null || classWrapper.name().equals(scope.name()))
        .collect(Collectors.toList());
  }

  public Optional<ClassWrapper> get(String name) {
    return Optional.ofNullable(classes.get(name));
  }

  public Optional<ClassWrapper> remove(ClassWrapper classWrapper) {
    return remove(classWrapper.name());
  }

  public Optional<ClassWrapper> remove(String name) {
    return Optional.ofNullable(classes.remove(name));
  }

  public Map<String, ClassWrapper> getClasses() {
    return classes;
  }

  public Map<String, byte[]> getOriginalClasses() {
    return originalClasses;
  }

  public Map<String, byte[]> getFiles() {
    return files;
  }
}
