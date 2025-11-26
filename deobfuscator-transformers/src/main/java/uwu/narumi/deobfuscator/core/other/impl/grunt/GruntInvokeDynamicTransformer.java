package uwu.narumi.deobfuscator.core.other.impl.grunt;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.MethodInsnNode;
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.asm.MethodContext;
import uwu.narumi.deobfuscator.api.asm.MethodRef;
import uwu.narumi.deobfuscator.api.asm.matcher.Match;
import uwu.narumi.deobfuscator.api.asm.matcher.group.SequenceMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.*;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

// TODO: heavy protection support idk
public class GruntInvokeDynamicTransformer extends Transformer {

  // same decryption used in the constant pool encryption thing :wilted_rose:

  private static final Match decryptionMethodMatch = SequenceMatch.of(
      OpcodeMatch.of(NEW),
      OpcodeMatch.of(DUP),
      MethodMatch.invokeSpecial().owner("java/lang/StringBuilder").name("<init>").desc("()V"),
      OpcodeMatch.of(ASTORE),
      NumberMatch.of(0),
      OpcodeMatch.of(ISTORE),
      JumpMatch.of(),
      VarLoadMatch.of(),
      VarLoadMatch.of(),
      VarLoadMatch.of(),
      MethodMatch.invokeVirtual().owner("java/lang/String").name("charAt").desc("(I)C"),
      NumberMatch.numInteger().capture("key"),
      OpcodeMatch.of(IXOR),
      OpcodeMatch.of(I2C),
      MethodMatch.invokeVirtual().owner("java/lang/StringBuilder").name("append").desc("(C)Ljava/lang/StringBuilder;"),
      OpcodeMatch.of(POP),
      OpcodeMatch.of(IINC)
  );
  /*
        aload v3
        checkcast java/lang/String
        astore v7
        aload v4
        checkcast java/lang/String
        astore v8
        aload v5
        checkcast java/lang/String
        astore v9
        aload v6
        checkcast java/lang/Integer
        invokevirtual java/lang/Integer.intValue ()I
        istore i10
        aload v9
        invokestatic Main.decryptStr (Ljava/lang/String;)Ljava/lang/String;
        ldc LMain;
        invokevirtual java/lang/Class.getClassLoader ()Ljava/lang/ClassLoader;
        invokestatic java/lang/invoke/MethodType.fromMethodDescriptorString (Ljava/lang/String;Ljava/lang/ClassLoader;)Ljava/lang/invoke/MethodType;
        astore v11
   */
  private static final Match invokeDynamicHandlerMatch = SequenceMatch.of(
      // a bunch of aliases LOL
      VarLoadMatch.of(),
      OpcodeMatch.of(CHECKCAST),
      OpcodeMatch.of(ASTORE),
      VarLoadMatch.of(),
      OpcodeMatch.of(CHECKCAST),
      OpcodeMatch.of(ASTORE),
      VarLoadMatch.of(),
      OpcodeMatch.of(CHECKCAST),
      MethodMatch.invokeVirtual().owner("java/lang/Integer").name("intValue").desc("()I"), // unbox
      OpcodeMatch.of(ISTORE),
      VarLoadMatch.of(),
      MethodMatch.invokeStatic().desc("(Ljava/lang/String;)Ljava/lang/String;").capture("decryptMethod")
  );
  private static final Match asd = InvokeDynamicMatch.create()
      .bsmTag(H_INVOKESTATIC);

  private static @NotNull String decrypt(@NotNull String e, int key) {
    StringBuilder b = new StringBuilder();
    int n = 0;
    while (n < e.length()) {
      b.append((char)(e.charAt(n) ^ key));
      ++n;
    }
    return b.toString();
  }

  @Override
  protected void transform() throws Exception {
    scopedClasses().forEach(this::handleClass);
  }

  private void handleClass(ClassWrapper cw) {
    cw.methods().forEach(melod -> {
      if (isAccess(melod.access, ACC_NATIVE) || isAccess(melod.access, ACC_ABSTRACT))
        return;
      final var mt = asd.findAllMatches(MethodContext.of(cw, melod));
      mt.forEach(ctx -> {
        final var indy = ctx.insn().asInvokeDynamicInsn();
        final var m = context().resolveMethodCtx(MethodRef.of(indy.bsm));
        if (m.isEmpty()) {
          LOGGER.warn("Failed to resolve invoke dynamic bsm handler!");
          return;
        }
        final var handler = m.orElseThrow();
        final var match = invokeDynamicHandlerMatch.findFirstMatch(handler);
        if (match == null) {
          LOGGER.warn(
              "Failed to match invokedynamic handler in {}.{} {}",
              handler.classWrapper().name(), handler.methodNode().name,
              handler.methodNode().desc
          );
          return;
        }
//        LOGGER.info("match {}", match);
        final var decryptMethod = match.captures().get("decryptMethod").insn().asMethodInsn();
        final var mCtx = context().resolveMethodCtx(decryptMethod);
        if (mCtx.isEmpty()) {
          LOGGER.warn(
              "Failed to resolve decrypt method ({}.{} {})",
              decryptMethod.owner,
              decryptMethod.name,
              decryptMethod.desc
          );
          return;
        }
        final var methodCtx = mCtx.orElseThrow();
        final var ffm = decryptionMethodMatch.findFirstMatch(methodCtx);
        if (ffm == null) {
          LOGGER.warn(
              "Failed to match decrypt method ({}.{} {})",
              decryptMethod.owner,
              decryptMethod.name,
              decryptMethod.desc
          );
          return;
        }
        final var decKey = ffm.captures().get("key").insn().asInteger();

        // [0] JVM provided
        // [1] JVM provided
        // [2] JVM provided
        //MethodHandles.Lookup lookup, String string, MethodType methodType, String string2, String string3, String string4, Integer n
        //String string2, String string3, String string4, Integer n
        final var eOwner = (String) indy.bsmArgs[0];
        final var eName = (String) indy.bsmArgs[1];
        final var eDesc = (String) indy.bsmArgs[2];
        // 1 = virtual, otherwise (e.g. 0) static.
        final var eType = (int) indy.bsmArgs[3];
        final var owner = decrypt(eOwner, decKey);
        final var name = decrypt(eName, decKey);
        final var desc = decrypt(eDesc, decKey);

        melod.instructions.insertBefore(indy, new MethodInsnNode(
            eType == 1 ? INVOKEVIRTUAL : INVOKESTATIC,
            owner,
            name,
            desc
        ));
        melod.instructions.remove(indy);
        markChange();
      });
    });
  }
}
