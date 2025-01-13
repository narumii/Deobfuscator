package uwu.narumi.deobfuscator.api.helper;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class FileHelper {

  private static final Logger LOGGER = LogManager.getLogger(FileHelper.class);

  private FileHelper() {
  }

  /**
   * Load all files from a zip file asynchronously
   *
   * @param path     Path to the zip file
   * @param consumer Consumer that accepts the file path and the file bytes. This method is called asynchronously
   */
  public static void loadFilesFromZip(Path path, BiConsumer<String, byte[]> consumer) {
    try (JarFile zipFile = new JarFile(path.toFile())) {
      ExecutorService executorService = Executors.newFixedThreadPool(5);
      List<CompletableFuture<Void>> futures = new ArrayList<>();

      Iterator<JarEntry> it = zipFile.entries().asIterator();

      while (it.hasNext()) {
        JarEntry zipEntry = it.next();

        String name = zipEntry.getName();
        byte[] bytes = zipFile.getInputStream(zipEntry).readAllBytes();

        // Skip directories
        boolean isDirectory = zipEntry.isDirectory() && bytes.length == 0;
        if (isDirectory) continue;

        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
          try {
            consumer.accept(name, bytes);
          } catch (Exception e) {
            LOGGER.error("Could not load ZipEntry: {}", zipEntry.getName(), e);
          }
        }, executorService);
        futures.add(future);
      }

      // Wait for all futures to complete
      CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
      executorService.shutdown();
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
