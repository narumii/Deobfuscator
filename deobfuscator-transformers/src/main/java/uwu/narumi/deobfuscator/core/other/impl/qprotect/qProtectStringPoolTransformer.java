package uwu.narumi.deobfuscator.core.other.impl.qprotect;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import uwu.narumi.deobfuscator.api.asm.MethodContext;
import uwu.narumi.deobfuscator.api.asm.matcher.Match;
import uwu.narumi.deobfuscator.api.asm.matcher.MatchContext;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.FieldMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.FrameMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.NumberMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.OpcodeMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.StringMatch;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

import java.util.Objects;

/**
 * Replaces string pool references with actual values.
 */
public class qProtectStringPoolTransformer extends Transformer {
  private static final Match STORE_STRING_TO_ARRAY_MATCH = OpcodeMatch.of(AASTORE)
      .and(FrameMatch.stack(0, StringMatch.of().capture("value")))
      .and(FrameMatch.stack(1, NumberMatch.of().capture("index")))
      .and(FrameMatch.stack(2, FieldMatch.getStatic()));

  private static final Match STRING_POOL_METHOD_MATCH = FieldMatch.putStatic().capture("stringPoolField")
      .and(FrameMatch.stack(0, OpcodeMatch.of(ANEWARRAY).and(Match.of(ctx -> ((TypeInsnNode) ctx.insn()).desc.equals("java/lang/String")))
          .and(FrameMatch.stack(0, NumberMatch.of().capture("size")))));

  @Override
  protected void transform() throws Exception {
    scopedClasses().forEach(classWrapper -> {
      MatchContext stringPoolMatchCtx = classWrapper.methods().stream()
          .map(methodNode -> STRING_POOL_METHOD_MATCH.findFirstMatch(MethodContext.of(classWrapper, methodNode)))
          .filter(Objects::nonNull)
          .findFirst()
          .orElse(null);

      // No number pool method found
      if (stringPoolMatchCtx == null) return;

      //System.out.println("Found string pool method in " + classWrapper.name() + "." + stringPoolMatchCtx.insnContext().methodNode().name);

      int stringPoolSize = stringPoolMatchCtx.captures().get("size").insn().asInteger();
      FieldInsnNode stringPoolFieldInsn = (FieldInsnNode) stringPoolMatchCtx.captures().get("stringPoolField").insn();

      // Get whole number pool
      String[] stringPool = new String[stringPoolSize];
      STORE_STRING_TO_ARRAY_MATCH.findAllMatches(stringPoolMatchCtx.insnContext().methodContext()).forEach(storeNumberMatchCtx -> {
        int index = storeNumberMatchCtx.captures().get("index").insn().asInteger();
        String value = storeNumberMatchCtx.captures().get("value").insn().asString();

        stringPool[index] = value;
      });

      for (String string : stringPool) {
        if (string == null) {
          // String pool is not fully initialized
          return;
        }
      }

      Match stringPoolReferenceMatch = OpcodeMatch.of(AALOAD) // AALOAD - Load array reference
          // Index
          .and(FrameMatch.stack(0, NumberMatch.of().capture("index")
              // Load number pool field
              .and(FrameMatch.stack(0, FieldMatch.getStatic().owner(stringPoolFieldInsn.owner).name(stringPoolFieldInsn.name).desc(stringPoolFieldInsn.desc)))
          ));

      // Replace number pool references with actual values
      classWrapper.methods().forEach(methodNode -> {
        MethodContext methodContext = MethodContext.of(classWrapper, methodNode);

        stringPoolReferenceMatch.findAllMatches(methodContext).forEach(numberPoolReferenceCtx -> {
          int index = numberPoolReferenceCtx.captures().get("index").insn().asInteger();
          String value = stringPool[index];

          // Value
          methodNode.instructions.insert(numberPoolReferenceCtx.insn(), new LdcInsnNode(value));
          numberPoolReferenceCtx.removeAll();
          markChange();
        });
      });

      // Cleanup
      classWrapper.methods().remove(stringPoolMatchCtx.insnContext().methodNode());
      classWrapper.fields().removeIf(fieldNode -> fieldNode.name.equals(stringPoolFieldInsn.name) && fieldNode.desc.equals(stringPoolFieldInsn.desc));
      // Remove string pool initialization from clinit
      classWrapper.findClInit().ifPresent(clinit -> {
        for (AbstractInsnNode insn : clinit.instructions.toArray()) {
          if (insn.getOpcode() == INVOKESTATIC && insn instanceof MethodInsnNode methodInsn &&
              methodInsn.name.equals(stringPoolMatchCtx.insnContext().methodNode().name) && methodInsn.desc.equals(stringPoolMatchCtx.insnContext().methodNode().desc) &&
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
