package uwu.narumi.deobfuscator.core.other.impl.grunt;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.MethodInsnNode;
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.asm.FieldRef;
import uwu.narumi.deobfuscator.api.asm.MethodContext;
import uwu.narumi.deobfuscator.api.asm.matcher.Match;
import uwu.narumi.deobfuscator.api.asm.matcher.group.SequenceMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.*;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

// TODO: implement more than just string decryption

public class GruntConstantPoolTransformer extends Transformer {

  /*
    getstatic Main.trFSDGUqx5BioGDr [Ljava/lang/String;
    iconst_0
    getstatic Main$ConstantPool.const_z9Fqaiy9srSMb26 Ljava/lang/String;
    invokevirtual java/lang/String.toCharArray ()[C
    ldc 68656L
    ldc -1107645088
    invokestatic Main.EOvwGuirQI7GaLbN ([CJI)Ljava/lang/String;
    aastore
  */
  private static final Match constantPoolCacheAdditionMatch = SequenceMatch.of(
      FieldMatch.getStatic().desc("[Ljava/lang/String;").capture("pool"),
      NumberMatch.numInteger().capture("index"),
      FieldMatch.getStatic().capture("constantVar"),
      MethodMatch.invokeVirtual().owner("java/lang/String").name("toCharArray").desc("()[C"),
      NumberMatch.numLong().capture("k1"),
      NumberMatch.numInteger().capture("k2"),
      MethodMatch.invokeStatic().desc("([CJI)Ljava/lang/String;").capture("decryptMethod"),
      OpcodeMatch.of(AASTORE)
  );

  /*
    ldc "䡯쫦釛봂谬푀㼑왅욨移嚣牤叞᜾犸뎹議㌉젦"
    invokestatic Main$ConstantPool.oWK5cSQDnT (Ljava/lang/String;)Ljava/lang/String;
    putstatic Main$ConstantPool.const_z9Fqaiy9srSMb26 Ljava/lang/String;
  */
  private static final Match lol = SequenceMatch.of(
      StringMatch.of().capture("enc"),
      MethodMatch.invokeStatic().desc("(Ljava/lang/String;)Ljava/lang/String;").capture("decryptMethod"),
      FieldMatch.putStatic().desc("Ljava/lang/String;").capture("field")
  );

  private static final Match initDecMatch = SequenceMatch.of(
      NumberMatch.numInteger().capture("k"),
      VarLoadMatch.of(),
      OpcodeMatch.of(IXOR),
      OpcodeMatch.of(ISTORE),
      NumberMatch.of(0),
      OpcodeMatch.of(ISTORE)
  );

  /*

    A:
        new java/lang/StringBuilder
        dup
        invokespecial java/lang/StringBuilder.<init> ()V
        astore v1
        iconst_0
        istore i2
        goto C
    B:
        aload v1
        aload v0
        iload i2
        invokevirtual java/lang/String.charAt (I)C
        sipush 1668
        ixor
        i2c
        invokevirtual java/lang/StringBuilder.append (C)Ljava/lang/StringBuilder;
        pop
        iinc i2 1
   */
  private static final Match finalDecMatch = SequenceMatch.of(
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

  private static @NotNull String strDecInit(char[] e, int classKey, long k1, long k2) {
    int n2 = Math.toIntExact(classKey ^ k2);
    for (int i = 0; i < e.length; ++i) {
      n2 = n2 ^ (int)k1 ^ ~i;
      n2 = Math.toIntExact(-(n2 ^ k2 - (long) i * e.length) * k2 | i);
      e[i] = (char)(e[i] ^ n2);
      int n3 = i & 0xFF;
      k2 = k2 << n3 | k2 >>> -n3;
      k1 ^= n3;
    }
    return new String(e);
  }

  private static @NotNull String stringDecryptFinal(@NotNull String e, int key) {
    StringBuilder b = new StringBuilder();
    int n = 0;
    while (n < e.length()) {
      b.append((char)(e.charAt(n) ^ key));
      ++n;
    }
    return b.toString();
  }

  private static @NotNull Map<String, String> getConstantPoolFromConstantPoolClass(ClassWrapper constantPool) {
    final var a = constantPool.findClInit();
    if (a.isEmpty()) return Collections.emptyMap();
    final var clInit = a.get();
    final var map = new HashMap<String, String>();
    for (final var insn : clInit.instructions) {
      if (!(insn instanceof MethodInsnNode min)) continue;
      // TODO: lazy lol
    }
    return map;
  }

  @Override
  protected void transform() throws Exception {
    scopedClasses().forEach(c -> {
      final var cliOpt = c.findClInit();
      if (cliOpt.isEmpty()) return;
      final var clInit = cliOpt.get();
      for (final var insn : clInit.instructions) {
        if (!(insn instanceof MethodInsnNode min)) continue;
        final var methodOpt = context().resolveMethod(min);
        if (methodOpt.isEmpty()) return;
        final var method = methodOpt.get();
        var ms = constantPoolCacheAdditionMatch.findAllMatches(MethodContext.of(c, method));
        if (ms.isEmpty()) {
          LOGGER.warn("couldn't find any encrypted constants in {}#{}{}", min.owner, min.name, min.desc);
          continue;
        }
        for (final var m : ms) {
          final var caps = m.captures();
          final var cv = caps.get("constantVar").insn().asFieldInsn();
          final var idx = caps.get("index").insn().asInteger();
          final var k1 = caps.get("k1").insn().asLong();
          final var k2 = caps.get("k2").insn().asInteger();
          final var dmI = caps.get("decryptMethod").insn().asMethodInsn();

          final var f = context().resolveField(FieldRef.of(cv)).orElseThrow();
          final var decMethodCls = context().getClassesMap().get(dmI.owner);
          final var decMethod = decMethodCls.findMethod(dmI).orElseThrow();
          final var decMethodMatch = initDecMatch.findFirstMatch(MethodContext.of(decMethodCls, decMethod));
          if (decMethodMatch == null) {
            LOGGER.warn("Failed to find the initial decryption method on {}#{}{}.", dmI.owner, dmI.name, dmI.desc);
            continue;
          }

          final var constantPoolClass = context().getClassesMap().get(cv.owner);
          final var cli = constantPoolClass.findClInit();
          if (cli.isEmpty()) {
            LOGGER.warn("Found encrypted constant pool class without a <clinit>.");
            continue;
          }
          final var cl = cli.get();

          final var a = lol.findAllMatches(MethodContext.of(constantPoolClass, cl));

          final var classKey = decMethodMatch.captures().get("k").insn().asInteger();

          final var sdi = strDecInit(((String)f.value).toCharArray(), classKey, k1, k2);
          LOGGER.info("Initial decryption: {}", sdi);
        }
      }
    });
  }
}
