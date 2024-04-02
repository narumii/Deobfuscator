package uwu.narumi.deobfuscator.api.helper;

import java.nio.file.Path;
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
                  consumer.accept(
                      zipEntry.getName(), zipFile.getInputStream(zipEntry).readAllBytes());
                } catch (Exception e) {
                  LOGGER.error("Could not load ZipEntry: {}", zipEntry.getName());
                  LOGGER.debug("Error", e);
                }
              });
    } catch (Exception e) {
      LOGGER.error("Could not load file: {}", path);
      LOGGER.debug("Error", e);
    }
  }
}
