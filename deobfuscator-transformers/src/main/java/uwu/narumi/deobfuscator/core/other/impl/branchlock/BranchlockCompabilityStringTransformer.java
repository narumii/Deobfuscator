package uwu.narumi.deobfuscator.core.other.impl.branchlock;

import org.objectweb.asm.tree.*;
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.asm.MethodContext;
import uwu.narumi.deobfuscator.api.asm.matcher.Match;
import uwu.narumi.deobfuscator.api.asm.matcher.MatchContext;
import uwu.narumi.deobfuscator.api.asm.matcher.group.SequenceMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.*;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class BranchlockCompabilityStringTransformer extends Transformer {

  private String[] decryptedStrings;
  private FieldInsnNode stringArray;

  private record DecryptedStringData(FieldInsnNode fieldInsnNode, String[] decryptedStrings) {}

  private static final Map<ClassWrapper, DecryptedStringData> decryptedDataMap = new HashMap<>();

  @Override
  protected void transform() throws Exception {
    scopedClasses().forEach(classWrapper -> {
      decryptedStrings = null;
      stringArray = null;
      if (decryptedDataMap.containsKey(classWrapper)) {
        decryptedStrings = decryptedDataMap.get(classWrapper).decryptedStrings();
        stringArray = decryptedDataMap.get(classWrapper).fieldInsnNode();
      } else {
        classWrapper.findClInit().ifPresent(clinit -> {
          MethodContext methodContext = MethodContext.of(classWrapper, clinit);
          String className = classWrapper.name().replace("/", ".");
          String methodName = clinit.name;
          Match stringEncryptionMatch = SequenceMatch.of(
              StringMatch.of().capture("encrypted-string"),
              MethodMatch.invokeVirtual().name("toCharArray").owner("java/lang/String"),
              OpcodeMatch.of(ASTORE)
          );
          MatchContext stringEncryption = stringEncryptionMatch.findFirstMatch(methodContext);
          if (stringEncryption != null) {
              Match stringArr = OpcodeMatch.of(PUTSTATIC).capture("string-arr");
              stringArray = stringArr.findFirstMatch(methodContext).insn().asFieldInsn();

              LdcInsnNode encryptedStringInsn = (LdcInsnNode) stringEncryption.captures().get("encrypted-string").insn();
              String encryptedString = encryptedStringInsn.asString();

              char[] encryptedStringArray = encryptedString.toCharArray();
              Match match = SequenceMatch.of(OpcodeMatch.of(DUP), NumberMatch.numInteger().capture("array-to"), OpcodeMatch.of(SWAP), NumberMatch.numInteger().capture("array-from"), OpcodeMatch.of(CALOAD), OpcodeMatch.of(CASTORE), OpcodeMatch.of(CASTORE));

              /* First "char swapper" salting */
              for (MatchContext allMatch : match.findAllMatches(methodContext)) {
                int arrayFrom = allMatch.captures().get("array-from").insn().asInteger();
                int arrayTo = allMatch.captures().get("array-to").insn().asInteger();
                try {
                  char store = encryptedStringArray[arrayFrom];
                  encryptedStringArray[arrayFrom] = encryptedStringArray[arrayTo];
                  encryptedStringArray[arrayTo] = store;
                } catch (Exception e) {
                  break;
                }
              }

              int encCharIndex = 0; // Under LDC

              int decStrIndex = 0; // Under new StringArr

              /* Finding new String Array creator for decrypted Strings */
              Match newArray = SequenceMatch.of(NumberMatch.numInteger().capture("salt"), OpcodeMatch.of(IXOR), OpcodeMatch.of(ILOAD), OpcodeMatch.of(IXOR), OpcodeMatch.of(ANEWARRAY));
              int salt = newArray.findFirstMatch(methodContext).captures().get("salt").insn().asInteger();
              int methodSalt = (methodName.hashCode() & 0xFFFF);

              char[] classNameArray = className.toCharArray();

              decryptedStrings = new String[encryptedStringArray[encCharIndex++] ^ salt ^ methodSalt];

              Match saltOfStrLen = SequenceMatch.of(OpcodeMatch.of(IINC), OpcodeMatch.of(CALOAD), NumberMatch.numInteger().capture("salt"), OpcodeMatch.of(IXOR), OpcodeMatch.of(ILOAD), OpcodeMatch.of(IXOR), OpcodeMatch.of(ISTORE));
              int salt2 = saltOfStrLen.findFirstMatch(methodContext).captures().get("salt").insn().asInteger();

              Match switchMatch = SequenceMatch.of(NumberMatch.numInteger().capture("switch-salt"), OpcodeMatch.of(IXOR), OpcodeMatch.of(LOOKUPSWITCH).capture("switch-table"));
              MatchContext switchContext = switchMatch.findFirstMatch(methodContext);
              int switchSalt = switchContext.captures().get("switch-salt").insn().asInteger();
              LookupSwitchInsnNode switchInsnNode = (LookupSwitchInsnNode) switchContext.captures().get("switch-table").insn();

              Match switch2Match = SequenceMatch.of(OpcodeMatch.of(ILOAD), OpcodeMatch.of(TABLESWITCH).capture("switch-table"));
              MatchContext switch2Context = switch2Match.findFirstMatch(methodContext);
              TableSwitchInsnNode tableSwitchInsnNode = (TableSwitchInsnNode) switch2Context.captures().get("switch-table").insn();


              /* Creating same simulation */
              while (encCharIndex < encryptedStringArray.length) {
                int strLength = encryptedStringArray[encCharIndex++] ^ salt2 ^ methodSalt;
                char[] toDecrypt = new char[strLength];
                int decCharIndex = 0; // Under var9_8 = new char[var2_2];

                while (strLength > 0) {
                  char nowDecrypted = encryptedStringArray[encCharIndex];
                  int switch2Value = 0;

                  Match swapKey = SequenceMatch.of(NumberMatch.numInteger().capture("swap-key"), OpcodeMatch.of(ISTORE), OpcodeMatch.of(GOTO));
                  Match xorKey = SequenceMatch.of(OpcodeMatch.of(ILOAD), NumberMatch.numInteger().capture("xor-key"), OpcodeMatch.of(IXOR));

                  int switchValue = classNameArray[encCharIndex % classNameArray.length] ^ switchSalt;
                  LabelNode switchCase = getLabelByKey(switchInsnNode, switchValue);
                  AtomicInteger s2v = new AtomicInteger(-1337);
                  swapKey.findAllMatches(methodContext).forEach(matchContext -> {
                    MatchContext capturedSwapKey = matchContext.captures().get("swap-key");
                    if (isInsnInLabelRange(clinit, switchCase, capturedSwapKey.insn())) {
                      s2v.set(capturedSwapKey.insn().asInteger());
                    }
                  });

                  if (s2v.get() != -1337) {
                    switch2Value = s2v.get();
                  }

                  AtomicInteger xor = new AtomicInteger(-1337);
                  xorKey.findAllMatches(methodContext).forEach(matchContext -> {
                    MatchContext capturedXorKey = matchContext.captures().get("xor-key");
                    if (isInsnInLabelRange(clinit, switchCase, capturedXorKey.insn())) {
                      xor.set(capturedXorKey.insn().asInteger());
                    }
                  });

                  if (xor.get() != -1337) {
                    nowDecrypted ^= xor.get();
                  }

                  if (switch2Value == 0 && xor.get() == -1337) {
                    toDecrypt[decCharIndex] = nowDecrypted;
                    ++decCharIndex;
                    ++encCharIndex;
                    --strLength;
                    switch2Value = 0;
                    continue;
                  }

                  if (switch2Value == 1) {
                    toDecrypt[decCharIndex] = nowDecrypted;
                    ++decCharIndex;
                    ++encCharIndex;
                    --strLength;
                    switch2Value = 0;
                    continue;
                  }

                  while (true) {
                    LabelNode tableCase = getLabelByKey(tableSwitchInsnNode, switch2Value);

                    AtomicInteger s2v2 = new AtomicInteger(-1337);
                    swapKey.findAllMatches(methodContext).forEach(matchContext -> {
                      MatchContext capturedSwapKey = matchContext.captures().get("swap-key");
                      if (isInsnInLabelRange(clinit, tableCase, capturedSwapKey.insn())) {
                        s2v2.set(capturedSwapKey.insn().asInteger());
                      }
                    });

                    switch2Value = s2v2.get();

                    AtomicInteger xor2 = new AtomicInteger(-1337);
                    xorKey.findAllMatches(methodContext).forEach(matchContext -> {
                      MatchContext capturedXorKey = matchContext.captures().get("xor-key");
                      if (isInsnInLabelRange(clinit, tableCase, capturedXorKey.insn())) {
                        xor2.set(capturedXorKey.insn().asInteger());
                      }
                    });

                    if (xor2.get() != -1337) {
                      nowDecrypted ^= xor2.get();
                    }

                    if (switch2Value == 1) {
                      toDecrypt[decCharIndex] = nowDecrypted;
                      ++decCharIndex;
                      ++encCharIndex;
                      --strLength;
                      switch2Value = 0;
                      break;
                    }
                  }
                }
                decryptedStrings[decStrIndex++] = new String(toDecrypt).intern();
              }

              decryptedDataMap.put(classWrapper, new DecryptedStringData(stringArray, decryptedStrings));

              Set<LabelNode> labelsInStringDecryption = new HashSet<>();
              Set<AbstractInsnNode> toRemove = new HashSet<>();
              AbstractInsnNode firstNode = encryptedStringInsn;
              while (firstNode != null) {
                if (firstNode instanceof LabelNode label) {
                  labelsInStringDecryption.add(label);
                } else {
                  toRemove.add(firstNode);
                }
                if (firstNode instanceof TableSwitchInsnNode) {
                  break;
                }
                firstNode = firstNode.getNext();
              }
              toRemove.forEach(clinit.instructions::remove);
              clinit.tryCatchBlocks.removeIf(tryCatchBlockNode -> labelsInStringDecryption.contains(tryCatchBlockNode.start) || labelsInStringDecryption.contains(tryCatchBlockNode.handler) || labelsInStringDecryption.contains(tryCatchBlockNode.end));
            }
        });
      }

      if (stringArray != null) {
        classWrapper.fields().removeIf(fieldNode -> fieldNode.name.equals(stringArray.name) && fieldNode.desc.equals(stringArray.desc));

        classWrapper.methods().forEach(methodNode -> {
          MethodContext methodContext = MethodContext.of(classWrapper, methodNode);
          methodNode.instructions.forEach(abstractInsnNode -> {
            Match getString = SequenceMatch.of(FieldMatch.getStatic().owner(stringArray.owner).name(stringArray.name).desc(stringArray.desc), NumberMatch.numInteger().capture("array-index"), OpcodeMatch.of(AALOAD));
            getString.findAllMatches(methodContext).forEach(matchContext -> {
              int arrayIndex = matchContext.captures().get("array-index").insn().asInteger();
              if (decryptedStrings.length <= arrayIndex) {
                LOGGER.error("Number on GETSTATIC isnt properly deobfuscated");
                return;

              }
              methodNode.instructions.insert(matchContext.insn(), new LdcInsnNode(decryptedStrings[arrayIndex]));
              matchContext.removeAll();
              markChange();
            });
          });
        });
      }
    });
  }

  public LabelNode getLabelByKey(LookupSwitchInsnNode lsi, int key) {
    int index = lsi.keys.indexOf(key);
    if (index == -1) {
      return lsi.dflt;
    }
    return lsi.labels.get(index);
  }

  public LabelNode getLabelByKey(TableSwitchInsnNode tsi, int key) {
    if (key < tsi.min || key > tsi.max) {
      return tsi.dflt;
    }
    int index = key - tsi.min;
    return tsi.labels.get(index);
  }

  public boolean isInsnInLabelRange(MethodNode method, LabelNode startLabel, AbstractInsnNode insn) {
    InsnList instructions = method.instructions;

    int startIndex = instructions.indexOf(startLabel);
    if (startIndex == -1) return false;

    int insnIndex = instructions.indexOf(insn);
    if (insnIndex == -1) return false;

    int endIndex = instructions.size();
    for (int i = startIndex + 1; i < instructions.size(); i++) {
      if (instructions.get(i) instanceof LabelNode) {
        endIndex = i;
        break;
      }
    }

    return insnIndex > startIndex && insnIndex < endIndex;
  }
}
