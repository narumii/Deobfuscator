package uwu.narumi.deobfuscator.core.other.impl.qprotect;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.asm.MethodContext;
import uwu.narumi.deobfuscator.api.asm.MethodRef;
import uwu.narumi.deobfuscator.api.asm.matcher.Match;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.FrameMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.MethodMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.NewArrayMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.NumberMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.OpcodeMatch;
import uwu.narumi.deobfuscator.api.transformer.Transformer;
import uwu.narumi.deobfuscator.core.other.impl.universal.pool.UniversalNumberPoolTransformer;

import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Transforms strongly encrypted method invocations in qProtect obfuscated code. Example here: {@link qprotect.InvokeDynamicStrongSample}
 */
public class qProtectInvokeDynamicStrongTransformer extends Transformer {

  private static final int XOR_KEY_ARRAY_SIZE = 16;

  private static final Match RESOLVE_HANDLE_MATCH = MethodMatch.invokeSpecial().owner("java/lang/invoke/ConstantCallSite").name("<init>").desc("(Ljava/lang/invoke/MethodHandle;)V")
      .and(FrameMatch.stack(0, OpcodeMatch.of(CHECKCAST)
          .and(FrameMatch.stack(0, MethodMatch.invokeStatic().desc("([Ljava/lang/Object;)Ljava/lang/Object;").capture("resolveHandleMethod")))));

  private static final Match FIND_METHOD_HANDLE_MATCH = MethodMatch.invokeStatic().desc("(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");

  private static final Match XOR_KEY_ARRAY_MATCH = OpcodeMatch.of(ASTORE).capture("varStore")
      .and(FrameMatch.stack(0, NewArrayMatch.of(T_BYTE)
          .and(FrameMatch.stack(0, NumberMatch.of(XOR_KEY_ARRAY_SIZE))))); // Size

  private static final Match INVOCATION_TYPE_COMPARE_MATCH = OpcodeMatch.of(IF_ICMPNE)
      .and(FrameMatch.stack(0, NumberMatch.of().capture("invocationType")))
      .and(FrameMatch.stack(1, MethodMatch.invokeVirtual()
          .and(FrameMatch.stack(0, OpcodeMatch.of(CHECKCAST)
              .and(FrameMatch.stack(0, OpcodeMatch.of(ALOAD)))))));

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
              invokeDynamicInsn.bsm.getDesc().equals("(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
          ) {
            // Get decryption information
            MethodRef decryptMethodRef = MethodRef.of(invokeDynamicInsn.bsm);
            DecryptionInfo decryptionInfo = decryptionMethods.computeIfAbsent(decryptMethodRef, (k) -> {
              // Compute if absent
              return extractDecryptionInformation(invokeDynamicInsn);
            });

            //System.out.println(decryptionInfo);

            String data = invokeDynamicInsn.name;
            //System.out.println(data);

            // Decrypt the method invocation
            MethodInsnNode decryptedInsn = decryptMethodInvocation(data, decryptionInfo, classWrapper);
            methodNode.instructions.set(invokeDynamicInsn, decryptedInsn);

            markChange();
          }
        }
      });
    });

    // Remove decryption methods
    decryptionMethods.values().forEach(decryptionInfo -> {
      context().removeMethod(decryptionInfo.bootstrapMethodRef);
      context().removeMethod(decryptionInfo.resolveHandleMethodRef);
      context().removeMethod(decryptionInfo.findMethodHandleMethodRef);
    });
  }

  private MethodInsnNode decryptMethodInvocation(String hexEncodedBase64String, DecryptionInfo decryptionInfo, ClassWrapper callerClass) {
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
    byte[] xorKey = decryptionInfo.xorKey(); // Dynamic

    for (int i = 0; i < decodedBytes.length; i++) {
      byte keyByte = xorKey[i % xorKey.length];
      byte dataByte = decodedBytes[i];
      // Custom bitwise operation (equivalent to dataByte ^ keyByte if the key was obfuscated differently)
      transformedBytes[i] = (byte)((dataByte | keyByte) & (~dataByte | ~keyByte));
    }

    // Step 4: Split the result string to extract method info
    //Object[] methodData = new Object[6];
    String[] parts = new String(transformedBytes).split(decryptionInfo.dataSeparator(), 4); // Dynamic - data separator

    //System.out.println(Arrays.toString(parts));

    String ownerClass = parts[0].replace(".", "/");
    String methodName = parts[1];
    String methodDesc = parts[2];
    int invocationType = Integer.parseInt(parts[3]);
    String specialClass = callerClass.name(); // It is a class that calls this method

    // Otherwise, the third invocation type is INVOKESPECIAL
    int opcode = decryptionInfo.invocationTypes().getOrDefault(invocationType, INVOKESPECIAL);

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

  private DecryptionInfo extractDecryptionInformation(InvokeDynamicInsnNode invokeDynamicInsn) {
    MethodRef decryptMethodRef = MethodRef.of(invokeDynamicInsn.bsm);
    MethodContext decryptMethodContext = context().getMethodContext(decryptMethodRef).orElseThrow();

    // Get data separator (method has only one LDC instruction with a string constant)
    String dataSeparator = Arrays.stream(decryptMethodContext.methodNode().instructions.toArray())
        .filter(insn2 -> insn2.getOpcode() == Opcodes.LDC)
        .map(insn2 -> (LdcInsnNode) insn2)
        .filter(ldcInsn -> ldcInsn.cst instanceof String)
        .map(ldcInsn -> (String) ldcInsn.cst)
        .findFirst()
        .orElseThrow();

    // Get XOR key
    VarInsnNode varStoreInsn = (VarInsnNode) XOR_KEY_ARRAY_MATCH.findFirstMatch(decryptMethodContext).captures().get("varStore").insn();
    byte[] xorKey = new byte[XOR_KEY_ARRAY_SIZE];
    Number[] xorKeyRaw = UniversalNumberPoolTransformer.getVarNumberPool(decryptMethodContext, XOR_KEY_ARRAY_SIZE, varStoreInsn);
    for (int i = 0; i < XOR_KEY_ARRAY_SIZE; i++) {
      xorKey[i] = xorKeyRaw[i].byteValue();
    }

    // Get invocation types
    MethodRef resolveHandleMethodRef = MethodRef.of(RESOLVE_HANDLE_MATCH.findFirstMatch(decryptMethodContext).captures().get("resolveHandleMethod").insn().asMethodInsn());
    MethodContext resolveHandleMethodContext = context().getMethodContext(resolveHandleMethodRef).orElseThrow();

    MethodRef findMethodHandleMethodRef = MethodRef.of(FIND_METHOD_HANDLE_MATCH.findFirstMatch(resolveHandleMethodContext).insn().asMethodInsn());
    MethodContext findMethodHandleMethodContext = context().getMethodContext(findMethodHandleMethodRef).orElseThrow();


    // Find invocation type codes (if statements, unlike in older qprotect switches)
    Map<Integer, Integer> invocationTypes = new HashMap<>();
    AtomicInteger i = new AtomicInteger();
    // The find method handle method is basically two if statements that check the invocation type
    INVOCATION_TYPE_COMPARE_MATCH.findAllMatches(findMethodHandleMethodContext).forEach(matchCtx -> {
      Integer invocationType = matchCtx.captures().get("invocationType").insn().asInteger();
      if (i.get() == 0) {
        invocationTypes.put(invocationType, INVOKESTATIC);
      } else if (i.get() == 1) {
        invocationTypes.put(invocationType, INVOKEVIRTUAL);
      } else {
        throw new IllegalStateException("Too many cases in findMethodHandleMethod");
      }

      // Otherwise, the third invocation type is INVOKESPECIAL

      i.getAndIncrement();
    });

    return new DecryptionInfo(
        // Methods
        decryptMethodRef,
        resolveHandleMethodRef,
        findMethodHandleMethodRef,
        // Data
        dataSeparator,
        xorKey,
        invocationTypes
    );
  }

  /**
   * Decryption information for encrypted method invocations.
   *
   * @param bootstrapMethodRef Method reference of the decryption method
   * @param dataSeparator Separator used to split decrypted data
   * @param invocationTypes invocation type -> opcode
   */
  private record DecryptionInfo(
      // Methods
      MethodRef bootstrapMethodRef,
      MethodRef resolveHandleMethodRef,
      MethodRef findMethodHandleMethodRef,
      // Data
      String dataSeparator,
      byte[] xorKey,
      Map<Integer, Integer> invocationTypes
  ) {
    @Override
    public String toString() {
      return "DecryptionInfo{" +
          "bootstrapMethodRef=" + bootstrapMethodRef +
          ", resolveHandleMethodRef=" + resolveHandleMethodRef +
          ", findMethodHandleMethodRef=" + findMethodHandleMethodRef +
          ", dataSeparator='" + dataSeparator + '\'' +
          ", xorKey=" + Arrays.toString(xorKey) +
          ", invocationTypes=" + invocationTypes +
          '}';
    }
  }
}
