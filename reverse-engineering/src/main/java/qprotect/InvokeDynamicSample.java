package qprotect;

import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Arrays;

public class InvokeDynamicSample {
  public static void main(String[] args) {
    invokeBootstrapMethod("SlAyclR2ZWV6bXBLem8waA==", "<\u001e&W\t\u001f=\\t\u00077D\u0011=0\u000f\n\u0003^\u001e\u00064O\\'\u0005.\u0017W?3\u0003l 2]+4\u000b\u0005\t\u0001q>\r\btQ\u001a\u0000u*\u0002]dE><~\u001f4l\u0011-T[O\u0013\n.OZ}\u00062\u0016\rB\u0002S($3D!7,*\f\nM\u0005\u00005TR=X\u0012\u0018RZ%V1bb\u000e");
  }

  public static Object invokeBootstrapMethod(Object xorKeyObj, Object encryptedDataObj) {
    // Extract encrypted data and XOR key
    String encryptedData = (String)encryptedDataObj;
    char[] xorKey = ((String)xorKeyObj).toCharArray();

    // Decrypt the data using XOR cipher
    char[] decryptedChars = new char[encryptedData.length()];
    char[] encryptedChars = encryptedData.toCharArray();
    for (int i = 0; i < encryptedData.length(); i++) {
      decryptedChars[i] = (char)(encryptedChars[i] ^ xorKey[i % xorKey.length]);
    }

    // Convert decrypted data to string and split parts
    byte[] decryptedBytes = new String(decryptedChars).getBytes();
    String decryptedString = new String(decryptedBytes);
    String[] parts = decryptedString.split("4Sa16wdk"); // Dynamic

    System.out.println(Arrays.toString(parts));

    // Extract method information from parts
    String ownerClass = parts[0];      // Class containing the method
    String methodName = parts[1];      // Name of the method
    String methodDesc = parts[2];      // Method descriptor
    String specialClass = parts[3];    // Special class for super calls
    int methodType = Integer.parseInt(parts[4]); // Type of method call

    System.out.println("Owner class: " + ownerClass);
    System.out.println("Method name: " + methodName);
    System.out.println("Method desc: " + methodDesc);
    System.out.println("Special class: " + specialClass);
    System.out.println("Method type: " + methodType);

    return null;

    /*MethodHandle handle;
    try {
      // Load required classes
      Class<?> ownerClassType = Class.forName(ownerClass);
      Class<?> specialClassType = Class.forName(specialClass);
      ClassLoader loader = InvokeDynamicSample.class.getClassLoader();
      MethodType type = MethodType.fromMethodDescriptorString(methodDesc, loader);

      // Create appropriate method handle based on call type
      switch (methodType) {
        case 548:    // Static method call // Dynamic
          handle = ((MethodHandles.Lookup)lookupObj).findStatic(ownerClassType, methodName, type);
          break;
        case 10116:  // Virtual method call // Dynamic
          handle = ((MethodHandles.Lookup)lookupObj).findVirtual(ownerClassType, methodName, type);
          break;
        case 25620:  // Special method call (super) // Dynamic
          handle = ((MethodHandles.Lookup)lookupObj).findSpecial(ownerClassType, methodName, type, specialClassType);
          break;
        default:
          throw new BootstrapMethodError();
      }

      // Adapt the method handle to the target type
      handle = handle.asType((MethodType)targetMethodTypeObj);
    } catch (Exception e) {
      e.printStackTrace();
      throw new BootstrapMethodError();
    }

    return new ConstantCallSite(handle);*/
  }
}
