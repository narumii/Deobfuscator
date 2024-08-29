package uwu.narumi.deobfuscator.api.helper;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.function.BiConsumer;
import java.util.jar.JarFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class FileHelper {

  private static final Logger LOGGER = LogManager.getLogger(FileHelper.class);

  private FileHelper() {}

  public static void loadFilesFromZip(Path path, BiConsumer<String, byte[]> consumer) {
    try (JarFile zipFile = new JarFile(path.toFile())) {
      zipFile
          .entries()
          .asIterator()
          .forEachRemaining(
              zipEntry -> {
                try {
                  consumer.accept(zipEntry.getName(), zipFile.getInputStream(zipEntry).readAllBytes());
                } catch (IOException e) {
                  LOGGER.error("Could not load ZipEntry: {}", zipEntry.getName());
                  throw new RuntimeException(e);
                }
              });
    } catch (Exception e) {
      LOGGER.error("Could not load file: {}", path);
      throw new RuntimeException(e);
    }
  }

  public static void deleteDirectory(Path dir) {
    try {
      if (Files.notExists(dir))
        return;

      Files.walkFileTree(dir, new SimpleFileVisitor<>() {
        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
          Files.delete(dir);
          return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
          Files.delete(file);
          return FileVisitResult.CONTINUE;
        }
      });
    } catch (Exception e) {
      throw new RuntimeException("Can't delete directory", e);
    }
  }
}
