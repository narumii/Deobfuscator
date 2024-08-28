import java.io.File;

public class TestInlineStaticFieldsWithModification {
  private static final char SYSTEM_SEPARATOR = File.separatorChar;
  private static final char OTHER_SEPARATOR;

  private static int getAdsCriticalOffset(String fileName) {
    int offset1 = fileName.lastIndexOf(SYSTEM_SEPARATOR);
    int offset2 = fileName.lastIndexOf(OTHER_SEPARATOR);
    if (offset1 == -1) {
      return offset2 == -1 ? 0 : offset2 + 1;
    } else {
      return offset2 == -1 ? offset1 + 1 : Math.max(offset1, offset2) + 1;
    }
  }

  static {
    if (isSystemWindows()) {
      OTHER_SEPARATOR = '/';
    } else {
      OTHER_SEPARATOR = '\\';
    }
  }

  static boolean isSystemWindows() {
    return SYSTEM_SEPARATOR == '\\';
  }
}