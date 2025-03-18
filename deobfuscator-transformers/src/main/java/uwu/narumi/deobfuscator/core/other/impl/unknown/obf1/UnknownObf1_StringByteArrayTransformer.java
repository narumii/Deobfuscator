package uwu.narumi.deobfuscator.core.other.impl.unknown.obf1;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import uwu.narumi.deobfuscator.api.asm.MethodContext;
import uwu.narumi.deobfuscator.api.asm.matcher.Match;
import uwu.narumi.deobfuscator.api.asm.matcher.group.SequenceMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.InsnMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.MethodMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.NumberMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.OpcodeMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.FrameMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.VarLoadMatch;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

import java.util.HashSet;
import java.util.Set;

/**
 * Decrypts strings from byte arrays.
 * <p>
 * Transforms this:
 * <pre>
 * byte[] var6 = new byte[16];
 * var6[5] = -38;
 * var6[5] = 115;
 * var6[8] = 110;
 * var6[4] = 97;
 * var6[15] = 110;
 * var6[10] = 101;
 * var6[9] = 82;
 * var6[12] = 112;
 * var6[11] = 115;
 * var6[6] = 115;
 * var6[14] = 119;
 * var6[0] = 99;
 * var6[3] = 112;
 * var6[7] = 79;
 * var6[1] = 111;
 * var6[2] = 109;
 * var6[13] = 97;
 * String string = new String(var6);
 * </pre>
 *
 * Into this:
 * <pre>
 * String string = "compassOnRespawn";
 * </pre>
 */
public class UnknownObf1_StringByteArrayTransformer extends Transformer {

  private static final Match STRING_BYTE_ARRAY_CONSTRUCTOR = MethodMatch.invokeSpecial()
      // INVOKESPECIAL - new String(byte[])
      .owner("java/lang/String").name("<init>").desc("([B)V").capture("string-constructor")
      // ALOAD - Load variable
      .and(FrameMatch.stack(0, VarLoadMatch.of()
          // ASTORE - Store array into variable
          .localStoreMatch(OpcodeMatch.of(ASTORE).capture("store-array")
              // NEWARRAY
              .and(FrameMatch.stack(0, OpcodeMatch.of(NEWARRAY)
                  // Number - Array length
                  .and(FrameMatch.stack(0, NumberMatch.numInteger().capture("array-length")))
              ))
          )
          .and(FrameMatch.stack(0, OpcodeMatch.of(DUP)
              .and(FrameMatch.stack(0, OpcodeMatch.of(NEW)))
          ))
      ));

  @Override
  protected void transform() throws Exception {
    scopedClasses().forEach(classWrapper -> classWrapper.methods().forEach(methodNode -> {
      MethodContext methodContext = MethodContext.of(classWrapper, methodNode);

      STRING_BYTE_ARRAY_CONSTRUCTOR.findAllMatches(methodContext).forEach(matchContext -> {
        Set<AbstractInsnNode> collectedInsns = new HashSet<>(matchContext.collectedInsns());

        AbstractInsnNode stringConstructorInsn = matchContext.captures().get("string-constructor").insn();
        VarInsnNode storeArrayInsn = (VarInsnNode) matchContext.captures().get("store-array").insn();
        int arrayLength = matchContext.captures().get("array-length").insn().asInteger();

        // Get byte array
        byte[] bytes = getByteArray(storeArrayInsn, arrayLength, collectedInsns, methodContext);

        // Pass byte array to build a string
        String deobfString = new String(bytes);

        // Place deobfuscated string into LDC instruction
        methodNode.instructions.insert(stringConstructorInsn, new LdcInsnNode(deobfString));

        // Remove all obfuscated instructions
        collectedInsns.forEach(methodNode.instructions::remove);

        markChange();
      });
    }));
  }

  /**
   * Build a byte array from instructions
   */
  private byte[] getByteArray(VarInsnNode storeArrayInsn, int arrayLength, Set<AbstractInsnNode> collectedInsns, MethodContext methodContext) {
    // TODO: Maybe convert this to use FrameMatch?
    Match storeByteToArray = SequenceMatch.of(
        VarLoadMatch.of().localStoreMatch(InsnMatch.of(storeArrayInsn)),
        NumberMatch.numInteger().capture("index"),
        NumberMatch.of().capture("value"),
        OpcodeMatch.of(BASTORE)
    );

    // Build byte array
    byte[] bytes = new byte[arrayLength];
    storeByteToArray.findAllMatches(methodContext).forEach(matchContext -> {
      int index = matchContext.captures().get("index").insn().asInteger();
      byte value = (byte) matchContext.captures().get("value").insn().asInteger();

      bytes[index] = value;
      collectedInsns.addAll(matchContext.collectedInsns());
    });

    return bytes;
  }
}
