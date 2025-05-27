package uwu.narumi.deobfuscator.core.other.impl.zkm;

import org.objectweb.asm.tree.MethodInsnNode;
import uwu.narumi.deobfuscator.api.asm.InsnContext;
import uwu.narumi.deobfuscator.api.asm.MethodContext;
import uwu.narumi.deobfuscator.api.asm.MethodRef;
import uwu.narumi.deobfuscator.api.asm.matcher.Match;
import uwu.narumi.deobfuscator.api.asm.matcher.MatchContext;
import uwu.narumi.deobfuscator.api.asm.matcher.group.SequenceMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.MethodMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.OpcodeMatch;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Removes useless try catches. References:
 * <ul>
 * <li>https://www.zelix.com/klassmaster/featuresExceptionObfuscation.html</li>
 * </ul>
 *
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

  private static final Match INVOKE_AND_RETURN =
      SequenceMatch.of(
          MethodMatch.invokeStatic().capture("invocation"),
          OpcodeMatch.of(ATHROW)
      );

  @Override
  protected void transform() throws Exception {
    scopedClasses().forEach(classWrapper -> {
      List<MethodRef> instantReturnExceptionMethods = new ArrayList<>();

      // Find methods that instantly returns an exception
      classWrapper.methods().forEach(methodNode -> {
        MethodContext methodContext = MethodContext.of(classWrapper, methodNode);

        // Check instructions
        if (methodNode.instructions.size() == 2 && INSTANT_RETURN_EXCEPTION.matches(methodContext.at(methodNode.instructions.getFirst()))) {
          // Add it to list
          instantReturnExceptionMethods.add(MethodRef.of(classWrapper.classNode(), methodNode));
        }
      });

      Set<MethodRef> toRemove = new HashSet<>();

      // Remove try-catches with these instant return exception methods
      classWrapper.methods().forEach(methodNode -> {
        MethodContext methodContext = MethodContext.of(classWrapper, methodNode);

        methodNode.tryCatchBlocks.removeIf(tryBlock -> {
          InsnContext start = methodContext.at(tryBlock.handler.getNext());
          MatchContext result = INVOKE_AND_RETURN.matchResult(start);
          if (result != null) {
            MethodRef methodRef = MethodRef.of((MethodInsnNode) result.insn());

            // Check if method is returning an exception instantly
            if (!instantReturnExceptionMethods.contains(methodRef)) return false;

            toRemove.add(methodRef);

            markChange();
            return true;
          } else {
            return false;
          }
        });
      });

      // Remove instant return exception methods
      classWrapper.methods().removeIf(methodNode -> toRemove.contains(MethodRef.of(classWrapper.classNode(), methodNode)));
    });
  }
}
