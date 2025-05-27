package qprotect;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Arrays;
import java.util.Base64;

public class InvokeDynamicStrongSample {
  public static void main(String[] args) {
    invokeEncryptedMethod("4b377678736f31317239332f5549356f3466354a435771352b7632575a626159786b6a4264504c7062684d77724f54396a48534277504e4b314872767245304a665a6a642b3470306c4e72335863562f344f4d51467a4477783966484b596a5a35454f50622b4c7754425177357650796d326d777a376c307a477a752f6c56474d374768397073356c66326e45706b3972773d3d");
  }

  public static Object invokeEncryptedMethod(String hexEncodedBase64String) {
    // Step 1: Convert obfuscated hex string into a Base64 string
    StringBuilder base64Builder = new StringBuilder();
    for (int i = 0; i < hexEncodedBase64String.length(); i += 2) {
      int end = Math.min((i ^ 2) + 2 * (i & 2), hexEncodedBase64String.length());
      String hexByte = hexEncodedBase64String.substring(i, end);
      base64Builder.append((char) Integer.parseInt(hexByte, 16));
    }

    // Step 2: Decode the Base64 string
    byte[] decodedBytes = Base64.getDecoder().decode(base64Builder.toString());

    // Step 3: Apply a custom byte-wise transformation using a key
    byte[] transformedBytes = new byte[decodedBytes.length];
    byte[] xorKey = new byte[]{68, -55, -106, -100, -17, 0, -60, -74, -106, 36, -96, 13, -105, -101, 39, 125}; // Dynamic

    for (int i = 0; i < decodedBytes.length; i++) {
      byte keyByte = xorKey[i % xorKey.length];
      byte dataByte = decodedBytes[i];
      // Custom bitwise operation (equivalent to dataByte ^ keyByte if the key was obfuscated differently)
      transformedBytes[i] = (byte)((dataByte | keyByte) & (~dataByte | ~keyByte));
    }

    // Step 4: Split the result string to extract method info
    Object[] methodData = new Object[6];
    String[] parts = new String(transformedBytes).split("wx7jt9QK", 4); // Dynamic - data separator
    System.arraycopy(parts, 0, methodData, 0, 4);
    //methodData[4] = lookup;
    //methodData[5] = methodType;
    System.out.println(Arrays.toString(methodData));
    //resolveHandle(methodData);
    //return new ConstantCallSite((MethodHandle)resolveHandle(var15));

    return null;
  }

  public static Object resolveHandle(Object[] methodData) {
    try {
      int opcode = Integer.parseInt((String) methodData[3]);
      MethodHandles.Lookup lookup = (MethodHandles.Lookup) methodData[4];
      Class<?> ownerClass = Class.forName((String) methodData[0]);
      String methodName = (String) methodData[1];
      MethodType type = MethodType.fromMethodDescriptorString(
          (String) methodData[2],
          Class.forName(methodData[4].toString()).getClassLoader()
      );
      Class<?> specialCaller = Class.forName(methodData[4].toString()); // It is a class that calls this method

      // Get the MethodHandle based on the opcode (e.g. static, virtual, special)
      MethodHandle handle = (MethodHandle) findMethodHandle(
          opcode,
          lookup,
          ownerClass,
          methodName,
          type,
          specialCaller
      );

      // Adapt handle to the expected type
      return handle.asType((MethodType) methodData[5]);
    } catch (Throwable var2) {
      //throw var2;
      return null;
    }
  }

  public static Object findMethodHandle(Integer opcodeCode, MethodHandles.Lookup lookup, Class<?> ownerClass, String methodName, MethodType methodType, Class<?> specialCaller) throws NoSuchMethodException, IllegalAccessException {
    return opcodeCode == 588 // Dynamic
        ? lookup.findStatic(ownerClass, methodName, methodType)
        : (
        opcodeCode == 16908 // Dynamic
            ? lookup.findVirtual(ownerClass, methodName, methodType)
            : lookup.findSpecial(ownerClass, methodName, methodType, specialCaller)
    );
  }
}
