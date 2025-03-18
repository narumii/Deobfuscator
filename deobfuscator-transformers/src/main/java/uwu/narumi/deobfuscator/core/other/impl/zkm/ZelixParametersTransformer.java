package uwu.narumi.deobfuscator.core.other.impl.zkm;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import uwu.narumi.deobfuscator.api.asm.MethodContext;
import uwu.narumi.deobfuscator.api.asm.matcher.Match;
import uwu.narumi.deobfuscator.api.asm.matcher.MatchContext;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.MethodMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.NumberMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.OpcodeMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.FrameMatch;
import uwu.narumi.deobfuscator.api.helper.AsmHelper;
import uwu.narumi.deobfuscator.api.helper.MethodHelper;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Decomposes object array param ({@code foo(Object[] args)}) into a readable params like: {@code foo(String arg1, int arg2)}.
 *
 * <p>References:
 * <ul>
 * <li>https://www.zelix.com/klassmaster/featuresMethodParameterObfuscation.html
 * </ul>
 *
 * <p>
 * Object array destructuring example:
 * <pre>
 * aload p0
 * dup
 * iconst_0
 * aaload
 * checkcast java/lang/String
 * astore v3
 * dup
 * iconst_1
 * aaload
 * checkcast java/lang/Long
 * invokevirtual java/lang/Long.longValue ()J
 * lstore j1
 * pop
 * </pre>
 */
// TODO: Remove object array creation and replace it with corresponding params
public class ZelixParametersTransformer extends Transformer {

  private static final Match OBJECT_ARRAY_ALOAD = OpcodeMatch.of(ALOAD).and(
      Match.of(context -> {
        // The object array is always the first argument to method
        return ((VarInsnNode) context.insn()).var == MethodHelper.getFirstParameterIdx(context.insnContext().methodNode());
      }));

  private static final Match OBJECT_ARRAY_ACCESS = FrameMatch.stack(0, OpcodeMatch.of(CHECKCAST).capture("cast")
      .and(FrameMatch.stack(0, OpcodeMatch.of(AALOAD)
          .and(FrameMatch.stack(0, NumberMatch.numInteger().capture("index")
              .and(FrameMatch.stack(0, OpcodeMatch.of(DUP)
                  .and(FrameMatch.stackOriginal(0, OBJECT_ARRAY_ALOAD.capture("load-array")))
              ))
          ))
      ))
  );

  private static final Match OBJECT_ARRAY_VAR_USAGE = Match.of(ctx -> ctx.insn().isVarStore()).capture("var-store")
      .and(
          FrameMatch.stack(0, MethodMatch.invokeVirtual().capture("to-primitive") // Converting to a primitive type
              .and(OBJECT_ARRAY_ACCESS)
          ).or(OBJECT_ARRAY_ACCESS)
      );

  private static final Match OBJECT_ARRAY_POP = OpcodeMatch.of(POP)
      .and(
          FrameMatch.stackOriginal(0, OBJECT_ARRAY_ALOAD.capture("load-array"))
      );

  @Override
  protected void transform() throws Exception {
    scopedClasses().forEach(classWrapper -> classWrapper.methods().stream()
        .filter(methodNode -> methodNode.desc.startsWith("([Ljava/lang/Object;)"))
        .forEach(methodNode -> {
          MethodContext methodContext = MethodContext.of(classWrapper, methodNode);

          Map<Integer, Integer> newVarIndexes = new HashMap<>(); // old var index -> new var index

          // Decompose object array to argument types
          List<Type> newArgumentTypes = decomposeObjectArrayToTypes(methodContext, newVarIndexes);

          // This flag is used to determine if we need to decompose arguments
          boolean shouldReplaceArgumentTypes = removeObjectArrayAccess(methodContext);

          if (shouldReplaceArgumentTypes) {
            // Update method arguments
            String desc = Type.getMethodDescriptor(Type.getReturnType(methodNode.desc), newArgumentTypes.toArray(new Type[0]));
            AsmHelper.updateMethodDescriptor(context(), methodContext, desc);

            // Replace local variables indexes with the corresponding ones
            fixLocalVariableIndexes(methodNode, newVarIndexes);
          }
        }));
  }

  /**
   * Decomposes object array to argument types.
   *
   * @param methodContext Method context
   * @param newVarIndexes New var indexes to fill
   * @return Argument types
   */
  private List<Type> decomposeObjectArrayToTypes(MethodContext methodContext, Map<Integer, Integer> newVarIndexes) {
    List<Type> newArgumentTypes = new ArrayList<>();

    int nextVarIndex = MethodHelper.getFirstParameterIdx(methodContext.methodNode());

    // Find all casts from that Object array
    for (MatchContext matchContext : OBJECT_ARRAY_VAR_USAGE.findAllMatches(methodContext)) {
      // Found argument!
      VarInsnNode varStore = (VarInsnNode) matchContext.captures().get("var-store").insn();
      TypeInsnNode typeInsn = (TypeInsnNode) matchContext.captures().get("cast").insn();
      int index = matchContext.captures().get("index").insn().asInteger();

      Type type = Type.getObjectType(typeInsn.desc);
      // If value is cast to primitive, then pass primitive
      if (matchContext.captures().containsKey("to-primitive")) {
        MethodInsnNode primitiveCastInsn = (MethodInsnNode) matchContext.captures().get("to-primitive").insn();
        type = getTypeFromPrimitiveCast(primitiveCastInsn);
      }

      // Add new argument
      newArgumentTypes.add(index, type);
      //System.out.println(index + " -> " + type);

      // Append new var index
      newVarIndexes.put(varStore.var, nextVarIndex);
      nextVarIndex = nextVarIndex + type.getSize();

      // Clean up array access
      VarInsnNode loadArrayInsn = (VarInsnNode) matchContext.captures().get("load-array").insn();
      for (AbstractInsnNode collectedInsn : matchContext.collectedInsns()) {
        if (collectedInsn.equals(loadArrayInsn)) continue;

        methodContext.methodNode().instructions.remove(collectedInsn);
      }

      markChange();
    }

    return newArgumentTypes;
  }

  /**
   * Removes object array access. Removes its ALOAD and POP instructions.
   *
   * @return {@code true} if any object array access was removed
   */
  private boolean removeObjectArrayAccess(MethodContext methodContext) {
    // Remove all object array accesses
    Optional<MatchContext> optMatch = OBJECT_ARRAY_POP.findAllMatches(methodContext).stream().findFirst();
    if (optMatch.isEmpty()) return false;
    MatchContext matchContext = optMatch.get();

    AbstractInsnNode loadArrayInsn = matchContext.captures().get("load-array").insn();

    // Remove those instructions
    methodContext.methodNode().instructions.remove(loadArrayInsn); // ALOAD
    methodContext.methodNode().instructions.remove(matchContext.insn()); // POP

    return true;
  }

  /**
   * Replace local variables indexes with the corresponding ones
   *
   * @param methodNode Method node
   * @param newVarIndexes New var indexes to use to replace the old ones
   */
  private void fixLocalVariableIndexes(MethodNode methodNode, Map<Integer, Integer> newVarIndexes) {
    for (AbstractInsnNode insn : methodNode.instructions.toArray()) {
      if (insn instanceof VarInsnNode varInsn) {
        if (newVarIndexes.containsKey(varInsn.var)) {
          // Replace it!
          varInsn.var = newVarIndexes.get(varInsn.var);
          markChange();
        }
      } else if (insn instanceof IincInsnNode iincInsn) {
        if (newVarIndexes.containsKey(iincInsn.var)) {
          // Replace it!
          iincInsn.var = newVarIndexes.get(iincInsn.var);
          markChange();
        }
      }
    }
  }
}
