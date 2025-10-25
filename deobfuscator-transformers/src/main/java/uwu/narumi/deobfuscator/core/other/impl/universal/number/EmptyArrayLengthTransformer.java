package uwu.narumi.deobfuscator.core.other.impl.universal.number;

import uwu.narumi.deobfuscator.api.asm.MethodContext;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.*;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

public class EmptyArrayLengthTransformer extends Transformer {
  @Override protected void transform() throws Exception {
    scopedClasses().forEach(classWrapper -> classWrapper.methods().forEach(methodNode -> {
      MethodContext methodContext = MethodContext.of(classWrapper, methodNode);
      OpcodeMatch.of(ARRAYLENGTH)
        .and(FrameMatch.stack(0,
          OpcodeMatch.of(NEWARRAY).and(FrameMatch.stack(0, NumberMatch.numInteger().capture("array-length")))))
        .findAllMatches(methodContext)
        .forEach(matchContext -> {
          methodNode.instructions.insert(matchContext.insn(),
            numberInsn(matchContext.captures().get("array-length").insn().asInteger()));
          matchContext.removeAll();
          markChange();
        });
    }));
  }
}
