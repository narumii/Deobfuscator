package uwu.narumi.deobfuscator.base;

import org.jetbrains.java.decompiler.main.extern.IResultSaver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.Manifest;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Result saver that will assert against previous decompiled code
 */
public class AssertingResultSaver implements IResultSaver {

  private final Path outputDir;

  private boolean savedContent = false;

  public AssertingResultSaver(Path outputDir) {
    this.outputDir = outputDir;
  }

  @Override
  public void saveFolder(String path) {
    // Nothing
  }

  @Override
  public void copyFile(String source, String path, String entryName) {
    // Nothing
  }

  /**
   * Assert decompiled code
   */
  @Override
  public void saveClassFile(String path, String qualifiedName, String entryName, String content, int[] mapping) {
    // Replace CRLF with LF
    content = content.replace("\r\n", "\n");

    // Remove file extension
    entryName = entryName.substring(0, entryName.lastIndexOf('.'));

    Path saveTo = this.outputDir.resolve(entryName + ".dec");

    try {
      if (Files.exists(saveTo)) {
        // Assert decompiled code
        String oldCode = Files.readString(saveTo);
        // Replace CRLF with LF
        oldCode = oldCode.replace("\r\n", "\n");

        assertEquals(oldCode, content);
      } else {
        // Save content
        Files.createDirectories(saveTo.getParent());
        Files.writeString(saveTo, content);

        // Mark that result saver saved content instead of asserting it
        savedContent = true;
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void createArchive(String path, String archiveName, Manifest manifest) {
    // Nothing
  }

  @Override
  public void saveDirEntry(String path, String archiveName, String entryName) {
    // Nothing
  }

  @Override
  public void copyEntry(String source, String path, String archiveName, String entry) {
    // Nothing
  }

  @Override
  public void saveClassEntry(String path, String archiveName, String qualifiedName, String entryName, String content) {
    // Nothing
  }

  @Override
  public void closeArchive(String path, String archiveName) {
    // Nothing
  }

  public boolean savedContent() {
    return savedContent;
  }
}
