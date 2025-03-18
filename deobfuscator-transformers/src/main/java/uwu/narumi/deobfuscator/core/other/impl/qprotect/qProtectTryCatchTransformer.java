package uwu.narumi.deobfuscator.core.other.impl.qprotect;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import uwu.narumi.deobfuscator.api.asm.MethodContext;
import uwu.narumi.deobfuscator.api.asm.matcher.Match;
import uwu.narumi.deobfuscator.api.asm.matcher.MatchContext;
import uwu.narumi.deobfuscator.api.asm.matcher.group.SequenceMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.MethodMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.OpcodeMatch;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

/**
 * Removes fake try catch blocks
 * <p>
 * Transforms this:
 * <pre>
 * block12: {
 *   plugin = IllIllllIlIl;
 *   try {
 *     PLJ.lIIlIIIllIlI("NFdlWk9sV3NYZTQ1Zm5vRg==", "##J\u001d\"\nZ\u00182F-2t%2R5\u001fPX?Iei\u001c0S*!\u001bU\"$\\ \n.5#E,Zs\u0000\"\u000blOfo2\u001a`-O\u0003:b<4?z D;\u000e^\u0012'\u0004V\u0013?%\u0007\u0003%\u000e\u0017\u000bxb\r\u0006\u0019;#T,Zs\u0000\"\u000blOx\u007f");
 *     break block12;
 *   } catch (RuntimeException runtimeException) {
 *     // empty catch block
 *   }
 *   while (true) {
 *     // Infinite loop
 *   }
 * }
 * </pre>
 *
 * Into this:
 * <pre>
 * plugin = IllIllllIlIl;
 * PLJ.lIIlIIIllIlI("NFdlWk9sV3NYZTQ1Zm5vRg==", "##J\u001d\"\nZ\u00182F-2t%2R5\u001fPX?Iei\u001c0S*!\u001bU\"$\\ \n.5#E,Zs\u0000\"\u000blOfo2\u001a`-O\u0003:b<4?z D;\u000e^\u0012'\u0004V\u0013?%\u0007\u0003%\u000e\u0017\u000bxb\r\u0006\u0019;#T,Zs\u0000\"\u000blOx\u007f");
 * </pre>
 */
public class qProtectTryCatchTransformer extends Transformer {
  private static final Match SELF_THROW_TRY_CATCH_MATCH = SequenceMatch.of(
      OpcodeMatch.of(DUP),
      MethodMatch.invokeVirtual().name("printStackTrace").desc("()V"),
      OpcodeMatch.of(ATHROW)
  );

  @Override
  protected void transform() throws Exception {
    scopedClasses().forEach(classWrapper -> classWrapper.methods().forEach(methodNode -> {
      MethodContext methodContext = MethodContext.of(classWrapper, methodNode);

      // Remove infinite loop try catch blocks
      methodNode.tryCatchBlocks.removeIf(tryCatch -> {
        LabelNode handlerLabel = tryCatch.handler;
        AbstractInsnNode nextInsn = handlerLabel.getNext();

        // Check if handler is in infinite loop (goto itself)
        if (nextInsn != null && nextInsn.getOpcode() == GOTO && ((JumpInsnNode) nextInsn).label == handlerLabel) {
          methodNode.instructions.remove(nextInsn);
          markChange();

          return true;
        }

        return false;
      });

      // Remove self-throw try catch blocks
      methodNode.tryCatchBlocks.removeIf(tryCatch -> {
        LabelNode handlerLabel = tryCatch.handler;
        AbstractInsnNode nextInsn = handlerLabel.getNext();

        MatchContext matchContext = SELF_THROW_TRY_CATCH_MATCH.matchResult(methodContext.at(nextInsn));
        if (matchContext != null) {
          matchContext.removeAll();
          markChange();

          return true;
        }

        return false;
      });
    }));
  }
}
