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
import uwu.narumi.deobfuscator.api.transformer.Transformer;
import uwu.narumi.deobfuscator.core.other.impl.pool.InlineStaticFieldTransformer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
  protected void transform() throws Exception {
    Set<String> classesToRemove = new HashSet<>();

    scopedClasses().forEach(classWrapper -> {
      List<MethodNode> toRemove = new ArrayList<>();

      // Find all encrypted strings
      classWrapper.methods().forEach(methodNode -> {
        MethodContext methodContext = MethodContext.of(classWrapper, methodNode);

        // Find encrypted strings
        ENCRYPTED_STRING.findAllMatches(methodContext).forEach(matchContext -> {
          AbstractInsnNode keyInsn = matchContext.captures().get("key").insn();
          MethodInsnNode decryptMethodInsn = (MethodInsnNode) matchContext.captures().get("decrypt-method").insn();
          MethodRef methodRef = MethodRef.of(decryptMethodInsn);

          // Get decrypt method
          findMethod(classWrapper.classNode(), methodRef).ifPresent(decryptMethod -> {
            String key = keyInsn.asString();

            MethodContext decryptMethodContext = MethodContext.of(classWrapper, decryptMethod);

            // Find class for constant pool
            LdcInsnNode constantPoolClassLdc = (LdcInsnNode) CLASS_FOR_CONSTANT_POOL.findAllMatches(decryptMethodContext)
                .get(0).captures().get("class").insn();
            Type classForConstantPoolType = (Type) constantPoolClassLdc.cst;

            // Prepare data for decryption
            ClassWrapper classForConstantPool = context().getClassesMap().get(classForConstantPoolType.getInternalName());
            int constantPoolSize = classForConstantPool.getConstantPool().getSize();
            String class0 = classWrapper.name();
            String class1 = classWrapper.name();

            // Decrypt!
            String decryptedString = decrypt(key, constantPoolSize, class0.hashCode(), class1.hashCode());

            methodNode.instructions.remove(keyInsn);
            methodNode.instructions.set(decryptMethodInsn, new LdcInsnNode(decryptedString));
            markChange();

            classesToRemove.add(classWrapper.name());

            toRemove.add(decryptMethod);
          });
        });
      });
      classWrapper.methods().removeAll(toRemove);
    });

    // Inline static fields
    Transformer.transform(InlineStaticFieldTransformer::new, scope(), context());

    // Cleanup
    classesToRemove.forEach(className -> context().getClassesMap().remove(className));
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
