package uwu.narumi.deobfuscator.core.other.impl.guardprotector;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import uwu.narumi.deobfuscator.api.asm.MethodContext;
import uwu.narumi.deobfuscator.api.asm.matcher.Match;
import uwu.narumi.deobfuscator.api.asm.matcher.group.SequenceMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.MethodMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.NumberMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.OpcodeMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.StringMatch;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

import java.util.ArrayList;
import java.util.List;

public class GuardProtectorStringTransformer extends Transformer {

  private static final Match ENCRYPTED_STRING = SequenceMatch.of(
      StringMatch.of().capture("key"),
      MethodMatch.invokeStatic().desc("(Ljava/lang/String;)Ljava/lang/String;").capture("decrypt-method")
  );

  private static final Match ENCRYPTED_STRING_HASH = SequenceMatch.of(
      OpcodeMatch.of(IXOR),
      NumberMatch.of().capture("hash"),
      OpcodeMatch.of(IXOR)
  );

  @Override
  protected void transform() throws Exception {
    scopedClasses().forEach(classWrapper -> {
      List<MethodNode> toRemove = new ArrayList<>();

      // Find all encrypted strings
      classWrapper.methods().forEach(methodNode -> {
        MethodContext methodContext = MethodContext.of(classWrapper, methodNode);

        // Find encrypted strings
        ENCRYPTED_STRING.findAllMatches(methodContext).forEach(matchContext -> {
          AbstractInsnNode keyInsn = matchContext.captures().get("key").insn();
          MethodInsnNode decryptMethodInsn = matchContext.captures().get("decrypt-method").insn().asMethodInsn();

          // Get decrypt method
          findMethod(classWrapper.classNode(), method -> method.name.equals(decryptMethodInsn.name) && method.desc.equals(decryptMethodInsn.desc)).ifPresent(decryptMethod -> {
            String key = keyInsn.asString();

            MethodContext decryptMethodContext = MethodContext.of(classWrapper, decryptMethod);
            int hash = ENCRYPTED_STRING_HASH.findFirstMatch(decryptMethodContext).captures().get("hash").insn().asNumber().intValue();

            String methodName = decryptMethod.name;

            String decryptedString = decrypt(key, methodName, hash);

            methodNode.instructions.remove(keyInsn);
            methodNode.instructions.set(decryptMethodInsn, new LdcInsnNode(decryptedString));
            markChange();

            toRemove.add(decryptMethod);
          });
        });
      });
      classWrapper.methods().removeAll(toRemove);
    });
  }

  private String decrypt(String string, String methodName, int key) {
    char[] chars = string.toCharArray();

    for (int i = 0; i < chars.length; ++i) {
      chars[i] = (char)(chars [i] ^ methodName.hashCode() ^ key);
    }

    return String.valueOf(chars);
  }
}
