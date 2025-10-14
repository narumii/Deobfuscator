package uwu.narumi.deobfuscator.core.other.impl.skidfuscator;

import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;
import uwu.narumi.deobfuscator.api.asm.MethodContext;
import uwu.narumi.deobfuscator.api.asm.MethodRef;
import uwu.narumi.deobfuscator.api.asm.matcher.Match;
import uwu.narumi.deobfuscator.api.asm.matcher.MatchContext;
import uwu.narumi.deobfuscator.api.asm.matcher.group.SequenceMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.FieldMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.MethodMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.NumberMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.OpcodeMatch;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

public class SkidStringTransformer extends Transformer {

  Match fieldPutStaticByteArray = SequenceMatch.of(
      MethodMatch.invokeStatic().desc("()[B").capture("method"),
      FieldMatch.putStatic().desc("[B").capture("field")
  );

  Match fieldPutStaticString = SequenceMatch.of(
      MethodMatch.invokeStatic().desc("()[B").capture("method"),
      MethodMatch.invokeStatic().owner("java/nio/ByteBuffer").name("wrap").desc("([B)Ljava/nio/ByteBuffer;"),
      MethodMatch.invokeVirtual().owner("java/nio/ByteBuffer").name("asCharBuffer").desc("()Ljava/nio/CharBuffer;"),
      MethodMatch.invokeVirtual().owner("java/nio/CharBuffer").name("toString").desc("()Ljava/lang/String;"),
      FieldMatch.putStatic().desc("Ljava/lang/String;").capture("field")
  );

  @Override
  protected void transform() throws Exception {
    scopedClasses().forEach(classWrapper -> {
      Set<MethodNode> trashMethods = new HashSet<>();
      classWrapper.findClInit().ifPresent(clinitNode -> {
        MethodContext clinitContext = MethodContext.of(classWrapper, clinitNode);
        MethodNode decryptionMethod = classWrapper.methods().stream().filter(methodNode -> methodNode.name.length() == 10 && methodNode.desc.endsWith("I)Ljava/lang/String;")).findFirst().orElse(null);
        if (decryptionMethod != null) {
          MethodContext methodContext = MethodContext.of(classWrapper, decryptionMethod);
          int methodSize = Match.of(ctx -> ctx.insn().isMathOperator()).findAllMatches(methodContext).size();
          if (methodSize == 24) {
            MatchContext byteBufferMatch = fieldPutStaticString.findFirstMatch(clinitContext);
            MethodNode toRemove;
            String decryptHash = ByteBuffer.wrap(
                resolveByteArrayFromMethod(
                    MethodContext.of(
                        classWrapper,
                        toRemove = findMethod(
                            classWrapper.classNode(),
                            MethodRef.of(
                                byteBufferMatch.captures().get("method").insn().asMethodInsn()
                            )
                        ).get()
                    )
                )
            ).asCharBuffer().toString();
            trashMethods.add(toRemove);
            byteBufferMatch.removeAll();
            classWrapper.methods().forEach(methodNode -> {
              MethodContext methodContext1 = MethodContext.of(classWrapper, methodNode);
              SequenceMatch.of(
                  MethodMatch.invokeStatic().desc("()[B").capture("byte-array"),
                  NumberMatch.numInteger().capture("salt"),
                  MethodMatch.invokeStatic().name(decryptionMethod.name)
              ).findAllMatches(methodContext1).forEach(matchContext -> {
                int salt = matchContext.captures().get("salt").insn().asInteger();
                MethodNode toRemove1;
                byte[] stringBytes = resolveByteArrayFromMethod(
                    MethodContext.of(
                        classWrapper,
                        toRemove1 = findMethod(
                            classWrapper.classNode(),
                            MethodRef.of(
                                matchContext.captures().get("byte-array").insn().asMethodInsn()
                            )
                        ).get()
                    )
                );
                trashMethods.add(toRemove1);
                methodNode.instructions.insertBefore(matchContext.insn(), new LdcInsnNode(method25(stringBytes, decryptHash, salt)));
                markChange();
                matchContext.removeAll();
              });
            });
          } else if (methodSize == 5) {
            if (decryptionMethod.desc.equals("([B[BI)Ljava/lang/String;")) {
              classWrapper.methods().forEach(methodNode -> {
                MethodContext methodContext1 = MethodContext.of(classWrapper, methodNode);
                SequenceMatch.of(
                    MethodMatch.invokeStatic().desc("()[B").capture("byte-array1"),
                    MethodMatch.invokeStatic().desc("()[B").capture("byte-array2"),
                    NumberMatch.numInteger().capture("salt"),
                    MethodMatch.invokeStatic().name(decryptionMethod.name)
                ).findAllMatches(methodContext1).forEach(matchContext -> {
                  int salt = matchContext.captures().get("salt").insn().asInteger();
                  MethodNode toRemove1;
                  byte[] stringBytes = resolveByteArrayFromMethod(
                      MethodContext.of(
                          classWrapper,
                          toRemove1 = findMethod(
                              classWrapper.classNode(),
                              MethodRef.of(
                                  matchContext.captures().get("byte-array1").insn().asMethodInsn()
                              )
                          ).get()
                      )
                  );
                  trashMethods.add(toRemove1);
                  byte[] decryptBytes = resolveByteArrayFromMethod(
                      MethodContext.of(
                          classWrapper,
                          toRemove1 = findMethod(
                              classWrapper.classNode(),
                              MethodRef.of(
                                  matchContext.captures().get("byte-array2").insn().asMethodInsn()
                              )
                          ).get()
                      )
                  );
                  trashMethods.add(toRemove1);
                  methodNode.instructions.insertBefore(matchContext.insn(), new LdcInsnNode(method5(stringBytes, decryptBytes, salt)));
                  markChange();
                  matchContext.removeAll();
                });
              });
            } else if (decryptionMethod.desc.equals("([BI)Ljava/lang/String;")) {
              MatchContext bytesMatch = fieldPutStaticByteArray.findFirstMatch(clinitContext);
              MethodNode toRemove;
              byte[] decryptBytes = resolveByteArrayFromMethod(
                      MethodContext.of(
                          classWrapper,
                          toRemove = findMethod(
                              classWrapper.classNode(),
                              MethodRef.of(
                                  bytesMatch.captures().get("method").insn().asMethodInsn()
                              )
                          ).get()
                      )
                  );
              classWrapper.methods().forEach(methodNode -> {
                MethodContext methodContext1 = MethodContext.of(classWrapper, methodNode);
                SequenceMatch.of(
                    MethodMatch.invokeStatic().desc("()[B").capture("byte-array"),
                    NumberMatch.numInteger().capture("salt"),
                    MethodMatch.invokeStatic().name(decryptionMethod.name)
                ).findAllMatches(methodContext1).forEach(matchContext -> {
                  int salt = matchContext.captures().get("salt").insn().asInteger();
                  MethodNode toRemove1;
                  byte[] stringBytes = resolveByteArrayFromMethod(
                      MethodContext.of(
                          classWrapper,
                          toRemove1 = findMethod(
                              classWrapper.classNode(),
                              MethodRef.of(
                                  matchContext.captures().get("byte-array").insn().asMethodInsn()
                              )
                          ).get()
                      )
                  );
                  trashMethods.add(toRemove1);
                  methodNode.instructions.insertBefore(matchContext.insn(), new LdcInsnNode(method5(stringBytes, decryptBytes, salt)));
                  markChange();
                  matchContext.removeAll();
                });
              });
              trashMethods.add(toRemove);
              bytesMatch.removeAll();
            }
          }
          trashMethods.add(decryptionMethod);
        }
      });
      classWrapper.methods().removeAll(trashMethods);
    });
  }

  public static String method5(byte[] var0, byte[] var1, int var2) {
    byte[] var9 = Integer.toString(var2).getBytes();
    int var10 = 0;

    while (true) {
      int var18 = var0.length;
      if (var10 >= var18) {
        Charset var6 = StandardCharsets.UTF_16;
        return new String(var0, var6);
      }

      byte var21 = var0[var10];
      int var34 = var9.length;
      int var31 = var10 % var34;
      byte var28 = var9[var31];
      byte var23 = (byte)(var21 ^ var28);
      var0[var10] = var23;
      byte var24 = var0[var10];
      int var36 = var1.length;
      int var33 = var10 % var36;
      byte var30 = var1[var33];
      byte var26 = (byte)(var24 ^ var30);
      var0[var10] = var26;
      var10++;
    }
  }

  public static String method25(byte[] var0, String var1, int var2) {
    byte[] var8 = Integer.toString(var2).getBytes();
    int var18 = (var0[0] & 255) << 24;
    int var41 = (var0[1] & 255) << 16;
    int var19 = var18 | var41;
    int var45 = (var0[2] & 255) << 8;
    int var20 = var19 | var45;
    int var48 = var0[3] & 255;
    int var9 = var20 | var48;
    int var25 = (var0[4] & 255) << 24;
    int var55 = (var0[5] & 255) << 16;
    int var26 = var25 | var55;
    int var59 = (var0[6] & 255) << 8;
    int var27 = var26 | var59;
    int var62 = var0[7] & 255;
    int var10 = var27 | var62;
    String var29 = var1;
    int var84 = var10 + var9;
    String var30 = var29.substring(var10, var84);
    Charset var64 = StandardCharsets.UTF_16BE;
    byte[] var11 = var30.getBytes(var64);
    int var12 = 0;

    while (true) {
      int var66 = var11.length;
      if (var12 >= var66) {
        Charset var91 = StandardCharsets.UTF_16BE;
        return new String(var11, var91);
      }

      byte var85 = var11[var12];
      int var93 = var8.length;
      int var92 = var12 % var93;
      byte var90 = var8[var92];
      byte var87 = (byte)(var85 ^ var90);
      var11[var12] = var87;
      var12++;
    }
  }

  public byte[] resolveByteArrayFromMethod(MethodContext methodContext) {
    MatchContext decryptMethod = SequenceMatch.of(
        NumberMatch.numInteger(),
        OpcodeMatch.of(NEWARRAY)
    ).findFirstMatch(methodContext);
    if (decryptMethod != null) {
      byte[] bytes = new byte[decryptMethod.insn().asInteger()];
      SequenceMatch.of(
          OpcodeMatch.of(DUP),
          NumberMatch.numInteger().capture("array-index"),
          NumberMatch.of().capture("array-value"),
          OpcodeMatch.of(BASTORE)
      ).findAllMatches(methodContext).forEach(matchContext -> {
        bytes[matchContext.captures().get("array-index").insn().asInteger()] = matchContext.captures().get("array-value").insn().asNumber().byteValue();
      });
      return bytes;
    }
    return null;
  }
}
