package dev.xdark.ssvm.util;

import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import uwu.narumi.deobfuscator.helper.ReflectionHelper;

/**
 * Hacks and utils for io java classes
 *
 * @author Justus Garbe
 */
public final class IOUtil {

  /**
   * Returns either the file descriptor of a {@link FileDescriptor} object or a handle depending on
   * the platform.
   *
   * @param fd the file descriptor
   * @return the file descriptor or handle
   */
  public static long getHandleOrFd(FileDescriptor fd) {
    try {
      //            return fdHandle == null ? fdField.getInt(fd) : fdHandle.getLong(fd);
      return fdHandleOffset == -1
          ? ReflectionHelper.getUnsafe().getInt(fd, fdFieldOffset)
          : ReflectionHelper.getUnsafe().getLong(fd, fdHandleOffset);
    } catch (Exception e) {
      throw new IllegalStateException("Unable to get handle from FileDescriptor", e);
    }
  }

  private static final Field fdField;
  private static final Field fdHandle;

  private static final long fdFieldOffset;
  private static final long fdHandleOffset;

  static {
    Field fd = null;
    Field handle = null;
    long fdOffset = -1;
    long handleOffset = -1;
    try {
      fd = FileDescriptor.class.getDeclaredField("fd");
      //            fd.setAccessible(true);
      fdOffset = ReflectionHelper.getUnsafe().objectFieldOffset(fd);
    } catch (NoSuchFieldException e) {
      // ignore
    }
    try {
      handle = FileDescriptor.class.getDeclaredField("handle");
      //            handle.setAccessible(true);
      handleOffset = ReflectionHelper.getUnsafe().objectFieldOffset(handle);
    } catch (NoSuchFieldException e) {
      // ignore
    }

    fdField = fd;
    fdHandle = handle;
    fdFieldOffset = fdOffset;
    fdHandleOffset = handleOffset;
  }

  /**
   * @param inputStream Input stream, may be {@code null}.
   * @return Bytes of stream, or {@code null} if the stream was {@code null}
   * @throws IOException When the stream cannot be read from.
   */
  public static byte[] readAll(InputStream inputStream) throws IOException {
    if (inputStream == null) return null;
    int bufferSize = 2048;
    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      byte[] data = new byte[bufferSize];
      int bytesRead;
      int readCount = 0;
      while ((bytesRead = inputStream.read(data, 0, bufferSize)) != -1) {
        outputStream.write(data, 0, bytesRead);
        readCount++;
      }
      outputStream.flush();
      if (readCount == 1) {
        return data;
      }
      return outputStream.toByteArray();
    } finally {
      inputStream.close();
    }
  }
}
