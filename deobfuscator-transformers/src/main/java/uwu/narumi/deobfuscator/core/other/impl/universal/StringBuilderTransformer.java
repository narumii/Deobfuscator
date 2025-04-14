package uwu.narumi.deobfuscator.core.other.impl.universal;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.analysis.OriginalSourceValue;
import uwu.narumi.deobfuscator.api.asm.InsnContext;
import uwu.narumi.deobfuscator.api.asm.MethodContext;
import uwu.narumi.deobfuscator.api.asm.matcher.Match;
import uwu.narumi.deobfuscator.api.asm.matcher.MatchContext;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.FrameMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.MethodMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.OpcodeMatch;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

import java.util.HashSet;
import java.util.Set;

/**
 * Transformer that replaces StringBuilder.append() calls with a single string.
 */
public class StringBuilderTransformer extends Transformer {
  private static final Match STRING_BUILDER_INIT =
      OpcodeMatch.of(DUP)
          .and(FrameMatch.stack(0,
              OpcodeMatch.of(NEW).and(Match.of(ctx -> ((TypeInsnNode)ctx.insn()).desc.equals("java/lang/StringBuilder")))));

  private static final Match STRING_BUILDER_APPEND_MATCH =
      MethodMatch.invokeVirtual().owner("java/lang/StringBuilder").name("append").desc("(Ljava/lang/String;)Ljava/lang/StringBuilder;")
          .and(FrameMatch.stack(0, OpcodeMatch.of(LDC).capture("string")));

  private static final Match STRING_BUILDER_TO_STRING_MATCH =
      MethodMatch.invokeVirtual().owner("java/lang/StringBuilder").name("toString").desc("()Ljava/lang/String;");


  @Override
  protected void transform() throws Exception {
    scopedClasses().forEach(classWrapper -> classWrapper.methods().forEach(methodNode -> {
      MethodContext methodContext = MethodContext.of(classWrapper, methodNode);
      STRING_BUILDER_TO_STRING_MATCH.findAllMatches(methodContext).forEach(matchCtx -> {
        InsnContext insnContext = matchCtx.insnContext();

        Set<AbstractInsnNode> toRemove = new HashSet<>();

        OriginalSourceValue appendSourceValue = insnContext.frame().getStack(insnContext.frame().getStackSize() - 1);
        if (!appendSourceValue.isOneWayProduced()) {
          // Not a one-way produced value
          return;
        }
        InsnContext currentInsnCtx = insnContext.of(appendSourceValue.getProducer());

        // Build string (loop through String#append() calls)
        StringBuilder buildedString = new StringBuilder();
        while (true) {
          MatchContext appendMatchCtx = STRING_BUILDER_APPEND_MATCH.matchResult(currentInsnCtx);
          if (appendMatchCtx == null) {
            break;
          }
          String string = appendMatchCtx.captures().get("string").insn().asString();

          // Insert the string at the beginning, because the append is in reverse order (because we are following the stack)
          buildedString.insert(0, string);

          toRemove.addAll(appendMatchCtx.collectedInsns());

          // Next
          OriginalSourceValue appendSourceValue2 = currentInsnCtx.frame().getStack(currentInsnCtx.frame().getStackSize() - 2);
          if (!appendSourceValue2.isOneWayProduced()) {
            // Not a one-way produced value
            return;
          }
          currentInsnCtx = currentInsnCtx.of(appendSourceValue2.getProducer());
        }

        // Check if the first instruction is a StringBuilder init
        MatchContext stringBuilderInitMatchCtx = STRING_BUILDER_INIT.matchResult(currentInsnCtx);
        if (stringBuilderInitMatchCtx == null) {
          // Not a StringBuilder init
          return;
        }
        toRemove.addAll(stringBuilderInitMatchCtx.collectedInsns());
        // This will ensure that the StringBuilder <init> call will also be removed
        toRemove.addAll(currentInsnCtx.consumers());

        // Replace
        methodNode.instructions.set(insnContext.insn(), new LdcInsnNode(buildedString.toString()));
        markChange();

        // Cleanup
        toRemove.forEach(insn -> methodNode.instructions.remove(insn));
      });
    }));
  }
}
