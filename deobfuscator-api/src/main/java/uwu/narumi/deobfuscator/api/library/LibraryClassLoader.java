package uwu.narumi.deobfuscator.api.library;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class LibraryClassLoader extends URLClassLoader {

  private final List<Library> libraries = new ArrayList<>();

  public LibraryClassLoader(ClassLoader parent, List<Library> libraries) {
    super(
        libraries.stream()
            .map(
                library -> {
                  try {
                    return library.getPath().toUri().toURL();
                  } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                  }
                })
            .distinct()
            .toArray(URL[]::new),
        parent);

    this.libraries.addAll(libraries);
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
