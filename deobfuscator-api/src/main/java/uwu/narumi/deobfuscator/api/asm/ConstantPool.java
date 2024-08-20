package uwu.narumi.deobfuscator.api.asm;

import static org.objectweb.asm.Symbol.*;

import java.util.Arrays;
import org.objectweb.asm.ClassReader;

// Fuck asm...
// TODO: Proper implementation in future maybe?
public class ConstantPool implements Cloneable {

  private final int[] cpInfoOffsets;
  private final int[] tags;
  private final Object[] values;

  public ConstantPool(ClassReader classReader) {
    this.cpInfoOffsets = new int[classReader.getItemCount()];
    this.tags = new int[classReader.getItemCount()];
    this.values = new Object[classReader.getItemCount()];
    for (int i = 0; i < this.cpInfoOffsets.length; i++) {
      this.cpInfoOffsets[i] = classReader.getItem(i);
    }

    char[] charBuffer = new char[classReader.getMaxStringLength() * 2]; // fuck you asm
    for (int i = 1; i < cpInfoOffsets.length; i++) {
      int itemOffset = cpInfoOffsets[i];
      int itemTagOffset = itemOffset - 1;
      int itemTag = classReader.readByte(itemTagOffset);

      switch (itemTag) {
        case CONSTANT_FIELDREF_TAG, CONSTANT_METHODREF_TAG, CONSTANT_INTERFACE_METHODREF_TAG -> {
          int nameAndTypeItemOffset =
              classReader.getItem(classReader.readUnsignedShort(itemOffset + 2));
          this.values[i] =
              new Reference(
                  classReader.readClass(itemOffset, charBuffer),
                  classReader.readUTF8(nameAndTypeItemOffset, charBuffer),
                  classReader.readUTF8(nameAndTypeItemOffset + 2, charBuffer));
        }
        case CONSTANT_INTEGER_TAG -> this.values[i] = classReader.readInt(itemOffset);
        case CONSTANT_FLOAT_TAG ->
            this.values[i] = Float.intBitsToFloat(classReader.readInt(itemOffset));
        case CONSTANT_NAME_AND_TYPE_TAG ->
            this.values[i] =
                new NameAndType(
                    classReader.readUTF8(itemOffset, charBuffer),
                    classReader.readUTF8(itemOffset + 2, charBuffer));
        case CONSTANT_LONG_TAG -> this.values[i] = classReader.readLong(itemOffset);
        case CONSTANT_DOUBLE_TAG ->
            this.values[i] = Double.longBitsToDouble(classReader.readLong(itemOffset));
        case CONSTANT_UTF8_TAG ->
            this.values[i] =
                readUtf(
                    classReader,
                    itemOffset + 2,
                    classReader.readUnsignedShort(itemOffset),
                    charBuffer);
        case CONSTANT_METHOD_HANDLE_TAG -> {
          int memberRefItemOffset =
              classReader.getItem(classReader.readUnsignedShort(itemOffset + 1));
          int nameAndTypeItemOffset =
              classReader.getItem(classReader.readUnsignedShort(memberRefItemOffset + 2));

          this.values[i] =
              new MethodHandle(
                  classReader.readByte(itemOffset),
                  classReader.readClass(memberRefItemOffset, charBuffer),
                  classReader.readUTF8(nameAndTypeItemOffset, charBuffer),
                  classReader.readUTF8(nameAndTypeItemOffset + 2, charBuffer));
        }
        case CONSTANT_DYNAMIC_TAG, CONSTANT_INVOKE_DYNAMIC_TAG -> {
          int nameAndTypeItemOffset =
              classReader.getItem(classReader.readUnsignedShort(itemOffset + 2));
          this.values[i] =
              new DynamicReference(
                  classReader.readUTF8(nameAndTypeItemOffset, charBuffer),
                  classReader.readUTF8(nameAndTypeItemOffset + 2, charBuffer),
                  classReader.readUnsignedShort(itemOffset));
        }

        case CONSTANT_STRING_TAG,
                CONSTANT_CLASS_TAG,
                CONSTANT_METHOD_TYPE_TAG,
                CONSTANT_MODULE_TAG,
                CONSTANT_PACKAGE_TAG ->
            this.values[i] = classReader.readUTF8(itemOffset, charBuffer);
      }

      this.tags[i] = itemTag;
      i += (itemTag == CONSTANT_LONG_TAG || itemTag == CONSTANT_DOUBLE_TAG) ? 1 : 0;
    }
  }

  private ConstantPool(int[] cpInfoOffsets, int[] tags, Object[] values) {
    this.cpInfoOffsets = cpInfoOffsets;
    this.tags = tags;
    this.values = values;
  }

  public int getConstantPoolInfoIndex(int index) {
    return cpInfoOffsets[index];
  }

  public int getTag(int index) {
    return tags[index];
  }

  public Object getValue(int index) {
    return values[index];
  }

  public int getSize() {
    return cpInfoOffsets.length;
  }

  // Im actually gonna shoot myself
  private String readUtf(
      ClassReader classReader, final int utfOffset, final int utfLength, final char[] charBuffer) {
    int currentOffset = utfOffset;
    int endOffset = currentOffset + utfLength;
    int strLength = 0;
    byte[] classBuffer = classReader.b;
    while (currentOffset < endOffset) {
      int currentByte = classBuffer[currentOffset++];
      if ((currentByte & 0x80) == 0) {
        charBuffer[strLength++] = (char) (currentByte & 0x7F);
      } else if ((currentByte & 0xE0) == 0xC0) {
        charBuffer[strLength++] =
            (char) (((currentByte & 0x1F) << 6) + (classBuffer[currentOffset++] & 0x3F));
      } else {
        charBuffer[strLength++] =
            (char)
                (((currentByte & 0xF) << 12)
                    + ((classBuffer[currentOffset++] & 0x3F) << 6)
                    + (classBuffer[currentOffset++] & 0x3F));
      }
    }
    return new String(charBuffer, 0, strLength);
  }

  @Override
  protected ConstantPool clone() {
    return new ConstantPool(
        Arrays.copyOf(cpInfoOffsets, cpInfoOffsets.length),
        Arrays.copyOf(tags, tags.length),
        Arrays.copyOf(values, values.length));
  }

  public record Reference(String owner, String name, String descriptor) {}

  public record MethodHandle(int kind, String owner, String name, String descriptor) {}

  public record DynamicReference(String name, String descriptor, int bootstrapMethodIndex) {}

  public record NameAndType(String name, String descriptor) {}
}
