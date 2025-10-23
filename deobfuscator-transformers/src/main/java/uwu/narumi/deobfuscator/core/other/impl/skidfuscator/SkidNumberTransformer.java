package uwu.narumi.deobfuscator.core.other.impl.skidfuscator;

import org.objectweb.asm.tree.LdcInsnNode;
import uwu.narumi.deobfuscator.api.asm.MethodContext;
import uwu.narumi.deobfuscator.api.asm.matcher.Match;
import uwu.narumi.deobfuscator.api.asm.matcher.group.SequenceMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.MethodMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.NumberMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.OpcodeMatch;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

import java.util.Random;

public class SkidNumberTransformer extends Transformer {

  Match randomNextMatch = SequenceMatch.of(
      OpcodeMatch.of(NEW),
      OpcodeMatch.of(DUP),
      NumberMatch.numLong().capture("init-random"),
      MethodMatch.invokeSpecial().owner("java/util/Random").name("<init>").desc("(J)V"),
      MethodMatch.invokeVirtual().owner("java/util/Random").capture("method")
  );

  @Override
  protected void transform() throws Exception {
    scopedClasses().forEach(classWrapper -> classWrapper.methods().forEach(methodNode -> {
      MethodContext methodContext = MethodContext.of(classWrapper, methodNode);
      randomNextMatch.findAllMatches(methodContext).forEach(matchContext -> {
        Random random = new Random(matchContext.captures().get("init-random").insn().asLong());
        Object object = switch (matchContext.captures().get("method").insn().asMethodInsn().name) {
          case "nextInt" -> random.nextInt();
          case "nextLong" -> random.nextLong();
          case "nextBoolean" -> random.nextBoolean();
          case "nextFloat" -> random.nextFloat();
          case "nextDouble" -> random.nextDouble();
          default -> throw new IllegalStateException("Unexpected value: " + matchContext.captures().get("method").insn().asMethodInsn().name);
        };
        methodNode.instructions.insert(matchContext.insn(), new LdcInsnNode(object));
        matchContext.removeAll();
        markChange();
      });
    }));
  }
}
