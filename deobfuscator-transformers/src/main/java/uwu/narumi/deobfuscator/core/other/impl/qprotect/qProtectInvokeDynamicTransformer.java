package uwu.narumi.deobfuscator.core.other.impl.qprotect;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import uwu.narumi.deobfuscator.api.asm.MethodRef;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Transforms encrypted method invocations in qProtect obfuscated code. Example here: {@link qprotect.InvokeDynamicSample}
 */
public class qProtectInvokeDynamicTransformer extends Transformer {

  @Override
  protected void transform() throws Exception {
    Map<MethodRef, DecryptionInfo> decryptionMethods = new HashMap<>();

    scopedClasses().forEach(classWrapper -> {
      classWrapper.methods().forEach(methodNode -> {

        // Decrypt encrypted method invocations
        for (AbstractInsnNode insn : methodNode.instructions.toArray()) {
          // Check if an instruction is an encrypted method invocation
          if (insn instanceof InvokeDynamicInsnNode invokeDynamicInsn &&
              insn.getOpcode() == INVOKEDYNAMIC &&
              invokeDynamicInsn.bsm.getDesc().equals("(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
          ) {
            // Get decryption information
            MethodRef decryptMethodRef = MethodRef.of(invokeDynamicInsn.bsm);
            DecryptionInfo decryptionInfo = decryptionMethods.computeIfAbsent(decryptMethodRef, (k) -> {
              // Compute if absent
              return extractDecryptionInformation(invokeDynamicInsn);
            });

            String xorKey = invokeDynamicInsn.name;
            String encryptedData = (String) invokeDynamicInsn.bsmArgs[0];

            // Decrypt the method invocation
            AbstractInsnNode decryptedInsn = decryptMethodInvocation(xorKey, encryptedData, decryptionInfo);
            methodNode.instructions.set(invokeDynamicInsn, decryptedInsn);

            markChange();
          }
        }
      });
    });

    // Remove decryption methods
    decryptionMethods.keySet().forEach(methodRef -> {
      context().getClassesMap().get(methodRef.owner()).methods()
          .removeIf(methodNode -> methodNode.name.equals(methodRef.name()) && methodNode.desc.equals(methodRef.desc()));
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
        MethodRef.of(invokeDynamicInsn.bsm),
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
  private AbstractInsnNode decryptMethodInvocation(String xorKey, String encryptedData, DecryptionInfo decryptionInfo) {
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

    // Handle special case where we cannot determine if it is INVOKEVIRTUAL or INVOKEINTERFACE
    if (opcode == INVOKEVIRTUAL) {
      ClassNode classInfo = context().getFullClassProvider().getClassInfo(ownerClass);
      if (classInfo != null) {
        // Check if class is an interface
        if (isAccess(classInfo.access, ACC_INTERFACE)) {
          opcode = INVOKEINTERFACE;
        }
      } else {
        LOGGER.warn("Could not find class {} for class/interface type detection. If you want a runnable jar then add the required lib.", ownerClass);
      }
    }

    return new MethodInsnNode(
        opcode,
        opcode == INVOKESPECIAL ? specialClass : ownerClass,
        methodName,
        methodDesc,
        opcode == INVOKEINTERFACE
    );
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
