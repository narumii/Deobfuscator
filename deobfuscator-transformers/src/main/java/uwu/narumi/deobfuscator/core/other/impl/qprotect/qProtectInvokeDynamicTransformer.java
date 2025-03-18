package uwu.narumi.deobfuscator.core.other.impl.qprotect;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import uwu.narumi.deobfuscator.api.asm.MethodRef;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

import java.util.Arrays;
import java.util.Map;

/**
 * Transforms encrypted method invocations in qProtect obfuscated code. Example here: {@link qprotect.InvokeDynamicSample}
 */
public class qProtectInvokeDynamicTransformer extends Transformer {
  private DecryptionInfo decryptionInfo = null;

  @Override
  protected void transform() throws Exception {
    scopedClasses().forEach(classWrapper -> {
      classWrapper.methods().forEach(methodNode -> {

        // Decrypt encrypted method invocations
        for (AbstractInsnNode insn : methodNode.instructions.toArray()) {
          // Check if an instruction is an encrypted method invocation
          if (insn instanceof InvokeDynamicInsnNode invokeDynamicInsn &&
              insn.getOpcode() == INVOKEDYNAMIC &&
              invokeDynamicInsn.bsm.getDesc().equals("(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
          ) {
            String xorKey = invokeDynamicInsn.name;
            String encryptedData = (String) invokeDynamicInsn.bsmArgs[0];

            if (decryptionInfo == null) {
              // Lazy load decryption information
              this.decryptionInfo = extractDecryptionInformation(invokeDynamicInsn);
            }

            // Decrypt the method invocation
            AbstractInsnNode decryptedInsn = decryptMethodInvocation(xorKey, encryptedData, decryptionInfo);
            methodNode.instructions.set(invokeDynamicInsn, decryptedInsn);
          }
        }
      });
    });
  }

  /**
   * Extracts decryption information from the decryption method.
   *
   * @param invokeDynamicInsn InvokeDynamic instruction of an obfuscated call
   * @return Decryption information
   */
  private DecryptionInfo extractDecryptionInformation(InvokeDynamicInsnNode invokeDynamicInsn) {
    MethodNode decryptMethod = context().getClassesMap().get(invokeDynamicInsn.bsm.getOwner()).findMethod(invokeDynamicInsn.bsm.getName(), invokeDynamicInsn.bsm.getDesc()).orElseThrow();

    // Get data separator (method has only one LDC instruction with a string constant)
    String dataSeparator = Arrays.stream(decryptMethod.instructions.toArray())
        .filter(insn2 -> insn2.getOpcode() == Opcodes.LDC)
        .map(insn2 -> (LdcInsnNode) insn2)
        .filter(ldcInsn -> ldcInsn.cst instanceof String)
        .map(ldcInsn -> (String) ldcInsn.cst)
        .findFirst()
        .orElseThrow();

    // Get invocation types from lookup switch
    LookupSwitchInsnNode lookupSwitch = Arrays.stream(decryptMethod.instructions.toArray())
        .filter(insn2 -> insn2.getOpcode() == Opcodes.LOOKUPSWITCH)
        .map(insn2 -> (LookupSwitchInsnNode) insn2)
        .findFirst()
        .orElseThrow();

    Map<Integer, Integer> invocationTypes = Map.of(
        lookupSwitch.keys.get(0), Opcodes.INVOKESTATIC,
        lookupSwitch.keys.get(1), Opcodes.INVOKEVIRTUAL,
        lookupSwitch.keys.get(2), Opcodes.INVOKESPECIAL
    );

    // Store decryption information
    return new DecryptionInfo(
        new MethodRef(invokeDynamicInsn.bsm.getOwner(), invokeDynamicInsn.bsm.getName(), invokeDynamicInsn.bsm.getDesc()),
        dataSeparator,
        invocationTypes
    );
  }

  /**
   * Decrypts an encrypted method invocation. Extracted from the obfuscated jar.
   *
   * @param xorKey XOR key used for decryption
   * @param encryptedData Encrypted data containing method information
   * @param decryptionInfo Decryption information
   * @return Decrypted method invocation instruction
   */
  private static AbstractInsnNode decryptMethodInvocation(String xorKey, String encryptedData, DecryptionInfo decryptionInfo) {
    char[] xorKeyArr = xorKey.toCharArray();

    // Decrypt the data using XOR cipher
    char[] decryptedChars = new char[encryptedData.length()];
    char[] encryptedChars = encryptedData.toCharArray();
    for (int i = 0; i < encryptedData.length(); i++) {
      decryptedChars[i] = (char)(encryptedChars[i] ^ xorKeyArr[i % xorKeyArr.length]);
    }

    // Convert decrypted data to string and split parts
    byte[] decryptedBytes = new String(decryptedChars).getBytes();
    String decryptedString = new String(decryptedBytes);
    String[] parts = decryptedString.split(decryptionInfo.dataSeparator());

    // Extract method information from parts
    // Class containing the method
    String ownerClass = parts[0].replace(".", "/");
    // Name of the method
    String methodName = parts[1];
    // Method descriptor
    String methodDesc = parts[2];
    // Special class for super calls
    String specialClass = parts[3].replace(".", "/");
    // Type of method call
    int methodType = Integer.parseInt(parts[4]);

    //System.out.println("Owner class: " + ownerClass);
    //System.out.println("Method name: " + methodName);
    //System.out.println("Method desc: " + methodDesc);
    //System.out.println("Special class: " + specialClass);
    //System.out.println("Method type: " + methodType);

    if (!decryptionInfo.invocationTypes().containsKey(methodType)) {
      throw new RuntimeException("Invalid method type");
    }

    int opcode = decryptionInfo.invocationTypes().get(methodType);
    return new MethodInsnNode(opcode, opcode == INVOKESPECIAL ? specialClass : ownerClass, methodName, methodDesc, false);
  }

  /**
   * Decryption information for encrypted method invocations.
   *
   * @param methodRef Method reference of the decryption method
   * @param dataSeparator Separator used to split decrypted data
   * @param invocationTypes invocation type -> opcode
   */
  private record DecryptionInfo(MethodRef methodRef, String dataSeparator, Map<Integer, Integer> invocationTypes) {
  }
}
