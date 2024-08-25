package uwu.narumii.deobfuscator.base;

import java.nio.charset.StandardCharsets;
import org.jetbrains.java.decompiler.main.extern.IResultSaver;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.Manifest;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Result saver that will assert against previous decompiled code
 */
public class AssertingResultSaver implements IResultSaver {

  private final TestDeobfuscationBase.InputType inputType;
  private final String jarRelativePath;

  private boolean savedContent = false;

  public AssertingResultSaver(TestDeobfuscationBase.InputType inputType, String jarRelativePath) {
    this.inputType = inputType;
    this.jarRelativePath = jarRelativePath;
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
    Path saveTo = this.inputType == TestDeobfuscationBase.InputType.CUSTOM_JAR
        ? TestDeobfuscationBase.RESULTS_CLASSES_PATH.resolve(inputType.directory()).resolve(this.jarRelativePath).resolve(entryName + ".dec")
        : TestDeobfuscationBase.RESULTS_CLASSES_PATH.resolve(inputType.directory()).resolve(entryName + ".dec");

    try {
      if (Files.exists(saveTo)) {
        // Assert decompiled code
        String oldCode = Files.readString(saveTo);
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
