package uwu.narumi.deobfuscator.core.other.impl.qprotect;

import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import uwu.narumi.deobfuscator.api.asm.MethodContext;
import uwu.narumi.deobfuscator.api.asm.MethodRef;
import uwu.narumi.deobfuscator.api.asm.matcher.Match;
import uwu.narumi.deobfuscator.api.asm.matcher.MatchContext;
import uwu.narumi.deobfuscator.api.asm.matcher.group.SequenceMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.*;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Transforms encrypted strings in qProtect obfuscated code. Example here: {@link qprotect.StringStrongEncryption}
 */
public class qProtectStrongStringTransformer extends Transformer {

  private static final Match DECRYPT_STRING_MATCH = MethodMatch.invokeStatic().desc("(II)Ljava/lang/String;")
      .and(FrameMatch.stack(0, NumberMatch.numInteger().capture("salt2")))
      .and(FrameMatch.stack(1, NumberMatch.numInteger().capture("salt1")));

  @Override
  protected void transform() throws Exception {
    scopedClasses().forEach(classWrapper -> {
      AtomicReference<MethodRef> decryptionMethodRef = new AtomicReference<>();

      Map<Integer, String> encryptedArray = new HashMap<>();

      classWrapper.findClInit().ifPresent(clinit -> {
        if (clinit.instructions.size() > 3 && clinit.instructions.get(0).isNumber()) {
          MethodContext clinitContext = MethodContext.of(classWrapper, clinit);
          SequenceMatch.of(OpcodeMatch.of(DUP), NumberMatch.of().capture("array-index"), StringMatch.of().capture("encrypted-value"), OpcodeMatch.of(AASTORE)).findAllMatches(clinitContext).forEach(matchContext -> {
            encryptedArray.put(matchContext.captures().get("array-index").insn().asInteger(), matchContext.captures().get("encrypted-value").insn().asString());
            matchContext.removeAll();
          });
        }
      });
      if (encryptedArray.isEmpty()) return;

      String[] decryptedArray = new String[encryptedArray.size()];

      classWrapper.methods().forEach(methodNode -> {
        DECRYPT_STRING_MATCH.findAllMatches(MethodContext.of(classWrapper, methodNode)).forEach(matchContext -> {
          int salt1 = matchContext.captures().get("salt1").insn().asInteger();
          int salt2 = matchContext.captures().get("salt2").insn().asInteger();

          MethodInsnNode decryptMethodInsn = (MethodInsnNode) matchContext.insn();
          decryptionMethodRef.set(MethodRef.of(decryptMethodInsn));

          MethodNode decryptionMethod = findMethod(classWrapper.classNode(), MethodRef.of(decryptMethodInsn)).orElseThrow();

          MethodContext decryptedMethodContext = MethodContext.of(classWrapper, decryptionMethod);
          List<MatchContext> numbers = NumberMatch.of().findAllMatches(decryptedMethodContext);

          int swappedKey = -1;

          int[] numbersArray = new int[2];
          for (int i = 0, hit = 0; i < numbers.size() && hit < 2; i++) {
            if (numbers.get(i).insn().asInteger() != -1) {
              numbersArray[hit] = numbers.get(i).insn().asInteger();
              hit++;
            }
          }
          int number1 = numbersArray[0];
          int number2 = numbersArray[1];

          if (!encryptedArray.containsKey(swappedKey)) {
            int key = salt1;
            swappedKey = (~(key = (key | number1) & (~key | number2)) | 0xFFFF) - ~key;
          }
          if (!encryptedArray.containsKey(swappedKey)) {
            int key = salt1;
            swappedKey = (~(key = key & number1 | ~key & number2) | 0xFFFF) - ~key;
          }
          if (!encryptedArray.containsKey(swappedKey)) {
            int key = salt1;
            swappedKey = (~(key = (key | number1) & ~(key & number2)) | 0xFFFF) - ~key;
          }

          if (!encryptedArray.containsKey(swappedKey)) {
            int key = salt1;
            swappedKey = (~(key = (key | number1) - (key & number1)) | 0xFFFF) - ~key;
          }


          if (!encryptedArray.containsKey(swappedKey)) {
            System.out.println(swappedKey);
            return;
          }


          int n3 = encryptedArray.get(swappedKey).toCharArray()[0];
          int switchCaseKey = (~n3 | 0xFF) - ~n3;
          int switchReturnKey;
          TableSwitchInsnNode table = (TableSwitchInsnNode) Match.of(ctx -> ctx.insn() instanceof TableSwitchInsnNode).findFirstMatch(decryptedMethodContext).insn();
          if (switchCaseKey >= table.min && switchCaseKey <= table.max) {
            switchReturnKey = table.labels.get(switchCaseKey).getNext().asNumber().intValue();
          } else {
            switchReturnKey = table.dflt.getNext().asNumber().intValue();
          }

          String decryptedString = decrypt(encryptedArray.get(swappedKey), decryptedArray, salt1, salt2, swappedKey, switchReturnKey);
          methodNode.instructions.insert(matchContext.insn(), new LdcInsnNode(decryptedString));
          matchContext.removeAll();

          markChange();
        });
      });

      // Remove decryption method
      if (decryptionMethodRef.get() != null) {
        classWrapper.methods().removeIf(methodNode -> methodNode.name.equals(decryptionMethodRef.get().name()) && methodNode.desc.equals(decryptionMethodRef.get().desc()));
      }
    });
  }
  /*
  * n pierwszy klucz
  * n2 drugi klucz
  * n4 klucz rozszyfrowany po pierwszej matematyce losowej
  * n5 zwrocony przez switch klucz
  * */
  private static String decrypt(String encryptedString, String[] decryptedArray, int n, int n2, int n4, int n5) {
    int n3 = n;
    if (decryptedArray[n4] == null) {
      char[] cArray = encryptedString.toCharArray();

      n3 = (short)n2;
      int n6 = (~n3 | 0xFF) - ~n3 + ~n5 + 1;
      if (n6 < 0) {
        n6 += 256;
      }
      n3 = (short)n2;
      int n7 = n5;
      int n8 = ((n3 = (~n3 | 0xFFFF) - ~n3 >>> 8) & ~n7) - (~n3 & n7);
      if (n8 < 0) {
        n8 += 256;
      }
      int n9 = 0;
      while (n9 < cArray.length) {
        int n10 = n9 % 2;
        int n11 = n9;
        char[] cArray2 = cArray;
        int n12 = cArray[n11];
        if (n10 == 0) {
          n7 = n6;
          n3 = n12;
          cArray2[n11] = (char)(n3 & ~n7 | ~n3 & n7);
          n7 = n6 << 5;
          n3 = n6 >>> 3;
          int n13 = (n3 & ~n7) + n7;
          n7 = cArray[n9];
          n3 = n13;
          n3 = (n3 | n7) & (~n3 | ~n7);
          n6 = (~n3 | 0xFF) - ~n3;
        } else {
          n7 = n8;
          n3 = n12;
          cArray2[n11] = (char)(n3 & ~n7 | ~n3 & n7);
          n7 = n8 << 5;
          n3 = n8 >>> 3;
          int n14 = (n3 & ~n7) + n7;
          n7 = cArray[n9];
          n3 = n14;
          n3 = (n3 | n7) - (n3 & n7);
          n8 = (~n3 | 0xFF) - ~n3;
        }
        ++n9;
      }
      decryptedArray[n4] = new String(cArray).intern();
    }
    return decryptedArray[n4];
  }
}
