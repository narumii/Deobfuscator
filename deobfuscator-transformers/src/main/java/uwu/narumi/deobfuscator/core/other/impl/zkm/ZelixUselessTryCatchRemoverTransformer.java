package uwu.narumi.deobfuscator.core.other.impl.zkm;

import org.objectweb.asm.tree.MethodInsnNode;
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.asm.InstructionContext;
import uwu.narumi.deobfuscator.api.asm.MethodContext;
import uwu.narumi.deobfuscator.api.asm.MethodRef;
import uwu.narumi.deobfuscator.api.asm.matcher.Match;
import uwu.narumi.deobfuscator.api.asm.matcher.group.SequenceMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.MethodMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.OpcodeMatch;
import uwu.narumi.deobfuscator.api.context.Context;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

import java.util.ArrayList;
import java.util.List;

/**
 * Removes useless try catches
 * <pre>
 * {@code
 * try {
 *     ...
 * } catch (PacketEventsLoadFailureException packetEventsLoadFailureException) {
 *     throw PacketEvents.a(packetEventsLoadFailureException);
 * }
 *
 * // Self return
 * private static Exception a(Exception exception) {
 *     return exception;
 * }
 * }
 * </pre>
 */
public class ZelixUselessTryCatchRemoverTransformer extends Transformer {
  private static final Match INSTANT_RETURN_EXCEPTION =
      SequenceMatch.of(
          OpcodeMatch.of(ALOAD),
          OpcodeMatch.of(ARETURN)
      );

  @Override
  protected void transform(ClassWrapper scope, Context context) throws Exception {
    context.classes(scope).forEach(classWrapper -> {
      List<MethodRef> instantReturnExceptionMethods = new ArrayList<>();

      // Find methods that instantly returns an exception
      classWrapper.methods().forEach(methodNode -> {
        MethodContext framelessContext = MethodContext.frameless(classWrapper, methodNode);

        // Check instructions
        if (methodNode.instructions.size() == 2 && INSTANT_RETURN_EXCEPTION.matches(framelessContext.newInsnContext(methodNode.instructions.getFirst()))) {
          // Add it to list
          instantReturnExceptionMethods.add(MethodRef.of(classWrapper.getClassNode(), methodNode));
        }
      });

      Match invokeAndReturnMatch =
          SequenceMatch.of(
              MethodMatch.invokeStatic().and(Match.predicate(ctx -> {
                MethodRef methodRef = MethodRef.of((MethodInsnNode) ctx.insn());
                return instantReturnExceptionMethods.contains(methodRef);
              })),
              OpcodeMatch.of(ATHROW)
          );

      // Remove try-catches with these instant return exception methods
      classWrapper.methods().forEach(methodNode -> {
        MethodContext framelessContext = MethodContext.frameless(classWrapper, methodNode);

        methodNode.tryCatchBlocks.removeIf(tryBlock -> {
          InstructionContext start = framelessContext.newInsnContext(tryBlock.handler.getNext());
          if (invokeAndReturnMatch.matches(start)) {
            markChange();
            return true;
          } else {
            return false;
          }
        });
      });
    });
  }
}
