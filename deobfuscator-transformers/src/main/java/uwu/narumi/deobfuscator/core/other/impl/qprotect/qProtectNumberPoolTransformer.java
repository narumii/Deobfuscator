package uwu.narumi.deobfuscator.core.other.impl.qprotect;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import uwu.narumi.deobfuscator.api.asm.MethodContext;
import uwu.narumi.deobfuscator.api.asm.matcher.Match;
import uwu.narumi.deobfuscator.api.asm.matcher.MatchContext;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.FieldMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.FrameMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.MethodMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.NumberMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.OpcodeMatch;
import uwu.narumi.deobfuscator.api.helper.AsmHelper;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

import java.util.Objects;

/**
 * Transforms number pool references to actual values.
 */
public class qProtectNumberPoolTransformer extends Transformer {
  private static final Match STORE_NUMBER_TO_ARRAY_MATCH = OpcodeMatch.of(AASTORE)
      .and(FrameMatch.stack(0, MethodMatch.invokeStatic().owner("java/lang/Integer").name("valueOf").desc("(I)Ljava/lang/Integer;")
          .and(FrameMatch.stack(0, NumberMatch.of().capture("value")))))
      .and(FrameMatch.stack(1, NumberMatch.of().capture("index")))
      .and(FrameMatch.stack(2, FieldMatch.getStatic()));

  private static final Match NUMBER_POOL_METHOD_MATCH = FieldMatch.putStatic().capture("numberPoolField")
      .and(FrameMatch.stack(0, OpcodeMatch.of(ANEWARRAY).and(Match.of(ctx -> ((TypeInsnNode) ctx.insn()).desc.equals("java/lang/Integer")))
          .and(FrameMatch.stack(0, NumberMatch.of().capture("size")))));

  @Override
  protected void transform() throws Exception {
    scopedClasses().forEach(classWrapper -> {
      MatchContext numberPoolMatchCtx = classWrapper.methods().stream()
          .map(methodNode -> NUMBER_POOL_METHOD_MATCH.findFirstMatch(MethodContext.of(classWrapper, methodNode)))
          .filter(Objects::nonNull)
          .findFirst()
          .orElse(null);

      // No number pool method found
      if (numberPoolMatchCtx == null) return;

      //System.out.println("Found number pool method in " + classWrapper.name() + "." + numberPoolMatchCtx.insnContext().methodNode().name);

      int numberPoolSize = numberPoolMatchCtx.captures().get("size").insn().asInteger();
      FieldInsnNode numberPoolFieldInsn = (FieldInsnNode) numberPoolMatchCtx.captures().get("numberPoolField").insn();

      // Get whole number pool
      Integer[] numberPool = new Integer[numberPoolSize];
      STORE_NUMBER_TO_ARRAY_MATCH.findAllMatches(numberPoolMatchCtx.insnContext().methodContext()).forEach(storeNumberMatchCtx -> {
        int index = storeNumberMatchCtx.captures().get("index").insn().asInteger();
        int value = storeNumberMatchCtx.captures().get("value").insn().asInteger();

        numberPool[index] = value;
      });

      for (Integer integer : numberPool) {
        if (integer == null) {
          // Number pool is not fully initialized
          return;
        }
      }

      Match numberPoolReferenceMatch = MethodMatch.invokeVirtual().owner("java/lang/Integer").name("intValue").desc("()I")
          .and(FrameMatch.stack(0, OpcodeMatch.of(AALOAD) // AALOAD - Load array reference
              // Index
              .and(FrameMatch.stack(0, NumberMatch.of().capture("index")
                  // Load number pool field
                  .and(FrameMatch.stack(0, FieldMatch.getStatic().owner(numberPoolFieldInsn.owner).name(numberPoolFieldInsn.name).desc(numberPoolFieldInsn.desc)))
              ))
          ));

      // Replace number pool references with actual values
      classWrapper.methods().forEach(methodNode -> {
        MethodContext methodContext = MethodContext.of(classWrapper, methodNode);

        numberPoolReferenceMatch.findAllMatches(methodContext).forEach(numberPoolReferenceCtx -> {
          int index = numberPoolReferenceCtx.captures().get("index").insn().asInteger();
          int value = numberPool[index];

          // Value
          methodNode.instructions.insert(numberPoolReferenceCtx.insn(), AsmHelper.numberInsn(value));
          numberPoolReferenceCtx.removeAll();
          markChange();
        });
      });

      // Cleanup
      classWrapper.methods().remove(numberPoolMatchCtx.insnContext().methodNode());
      classWrapper.fields().removeIf(fieldNode -> fieldNode.name.equals(numberPoolFieldInsn.name) && fieldNode.desc.equals(numberPoolFieldInsn.desc));
      // Remove number pool initialization from clinit
      classWrapper.findClInit().ifPresent(clinit -> {
        for (AbstractInsnNode insn : clinit.instructions.toArray()) {
          if (insn.getOpcode() == INVOKESTATIC && insn instanceof MethodInsnNode methodInsn &&
              methodInsn.name.equals(numberPoolMatchCtx.insnContext().methodNode().name) && methodInsn.desc.equals(numberPoolMatchCtx.insnContext().methodNode().desc) &&
              methodInsn.owner.equals(classWrapper.name())
          ) {
            // Remove invocation
            clinit.instructions.remove(insn);
          }
        }
      });
    });
  }
}
