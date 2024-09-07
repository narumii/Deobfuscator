package uwu.narumi.deobfuscator.api.library;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class LibraryClassLoader extends URLClassLoader {

  private final List<Library> libraries = new ArrayList<>();
  private final Map<String, byte[]> rawClassFiles = new HashMap<>();

  public LibraryClassLoader(ClassLoader parent, List<Library> libraries) {
    super(
        libraries.stream()
            .map(
                library -> {
                  // Check if the path is null. Raw class files don't have path
                  if (library.getPath() == null) {
                    return null;
                  }

                  try {
                    return library.getPath().toUri().toURL();
                  } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                  }
                })
            .filter(Objects::nonNull)
            .distinct()
            .toArray(URL[]::new),
        parent);

    this.libraries.addAll(libraries);

    // We need to specially handle this case
    for (Library library : libraries) {
      if (library.getPath() != null) continue;

      this.rawClassFiles.putAll(library.getClassFiles());
    }
  }

  @Override
  protected Class<?> findClass(String name) throws ClassNotFoundException {
    String internalName = name.replace('.', '/');
    if (this.rawClassFiles.containsKey(internalName)) {
      byte[] classBytes = this.rawClassFiles.get(internalName);
      return defineClass(name, classBytes, 0, classBytes.length);
    }
    return super.findClass(name);
  }

  public Optional<byte[]> fetchRaw(String name) {
    return libraries.stream()
        .map(Library::getClassFiles)
        .filter(map -> map.containsKey(name))
        .map(map -> map.get(name))
        .findFirst();
  }

  public Optional<byte[]> fetchFile(String name) {
    return libraries.stream()
        .map(Library::getFiles)
        .filter(map -> map.containsKey(name))
        .map(map -> map.get(name))
        .findFirst();
  }
}
