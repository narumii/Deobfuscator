package uwu.narumi.deobfuscator.core.other.impl.hp888;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.asm.MethodContext;
import uwu.narumi.deobfuscator.api.asm.MethodRef;
import uwu.narumi.deobfuscator.api.asm.matcher.Match;
import uwu.narumi.deobfuscator.api.asm.matcher.group.SequenceMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.MethodMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.OpcodeMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.StringMatch;
import uwu.narumi.deobfuscator.api.context.Context;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

import java.util.ArrayList;
import java.util.List;

/**
 * Strings are encrypted using a constant pool size of a provided class.
 */
public class HP888StringTransformer extends Transformer {

  private static final Match ENCRYPTED_STRING = SequenceMatch.of(
      StringMatch.of().capture("key"),
      MethodMatch.invokeStatic().desc("(Ljava/lang/String;)Ljava/lang/String;").capture("decrypt-method")
  );

  private static final Match CLASS_FOR_CONSTANT_POOL = SequenceMatch.of(
      MethodMatch.invokeStatic(),
      OpcodeMatch.of(LDC).and(Match.of(ctx -> ((LdcInsnNode) ctx.insn()).cst instanceof Type)).capture("class"),
      MethodMatch.invokeInterface()
  );

  @Override
  protected void transform(ClassWrapper scope, Context context) throws Exception {
    context.classes().forEach(classWrapper -> {
      List<MethodNode> toRemove = new ArrayList<>();

      // Find all encrypted strings
      classWrapper.methods().forEach(methodNode -> {
        MethodContext methodContext = MethodContext.framed(classWrapper, methodNode);

        // Find encrypted strings
        ENCRYPTED_STRING.findAllMatches(methodContext).forEach(matchContext -> {
          AbstractInsnNode keyInsn = matchContext.captures().get("key").insn();
          MethodInsnNode decryptMethodInsn = (MethodInsnNode) matchContext.captures().get("decrypt-method").insn();
          MethodRef methodRef = MethodRef.of(decryptMethodInsn);

          // Get decrypt method
          findMethod(classWrapper.classNode(), methodRef).ifPresent(decryptMethod -> {
            String key = keyInsn.asString();

            MethodContext decryptMethodContext = MethodContext.framed(classWrapper, decryptMethod);

            // Find class for constant pool
            LdcInsnNode constantPoolClassLdc = (LdcInsnNode) CLASS_FOR_CONSTANT_POOL.findAllMatches(decryptMethodContext)
                .get(0).captures().get("class").insn();
            Type classForConstantPoolType = (Type) constantPoolClassLdc.cst;

            // Prepare data for decryption
            ClassWrapper classForConstantPool = context.getClasses().get(classForConstantPoolType.getInternalName());
            int constantPoolSize = classForConstantPool.getConstantPool().getSize();
            String class0 = classWrapper.name();
            String class1 = classWrapper.name();

            // Decrypt!
            String decryptedString = decrypt(key, constantPoolSize, class0.hashCode(), class1.hashCode());

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

  private String decrypt(String string, int constantPoolSize, int className0HashCode, int className1HashCode) {
    char[] charArray = string.toCharArray();
    int i = 0;
    for (char character : charArray) {
      character = (char) (character ^ constantPoolSize);
      character = (char) (character ^ className0HashCode);
      charArray[i] = (char) (character ^ className1HashCode);
      ++i;
    }
    return new String(charArray);
  }
}
