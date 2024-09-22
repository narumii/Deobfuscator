package uwu.narumi.deobfuscator.core.other.impl.zkm;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.OriginalSourceValue;
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.asm.InstructionContext;
import uwu.narumi.deobfuscator.api.asm.MethodContext;
import uwu.narumi.deobfuscator.api.asm.matcher.Match;
import uwu.narumi.deobfuscator.api.asm.matcher.MatchContext;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.MethodMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.NumberMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.OpcodeMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.StackMatch;
import uwu.narumi.deobfuscator.api.context.Context;
import uwu.narumi.deobfuscator.api.helper.AsmHelper;
import uwu.narumi.deobfuscator.api.helper.MethodHelper;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Decomposes array params ({@code foo(Object[] args)}) into a readable params like: {@code foo(String arg1, int arg2)}.
 *
 * <p>References:
 * <ul>
 * <li>https://www.zelix.com/klassmaster/featuresMethodParameterObfuscation.html
 * </ul>
 *
 * <p>
 * Example instruction set:
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
// TODO: If class extends another class and descriptor update happens on overwritten method, then also update that method
//  in the super class and interfaces.
// TODO: Remove object array creation and replace it with corresponding params
public class ZelixParametersTransformer extends Transformer {

  private static final Match ARRAY_ACCESS = StackMatch.of(0, OpcodeMatch.of(CHECKCAST).save("cast")
      .and(StackMatch.of(0, OpcodeMatch.of(AALOAD)
          .and(StackMatch.of(0, NumberMatch.numInteger().save("index")
              .and(StackMatch.of(0, OpcodeMatch.of(DUP)
                  .and(StackMatch.ofOriginal(0, OpcodeMatch.of(ALOAD).and(
                      // The object array is always the first argument to method
                      Match.predicate(context -> {
                        return ((VarInsnNode) context.insn()).var == MethodHelper.getFirstParameterIdx(context.insnContext().methodNode());
                      })
                  ).save("load-array")))
              ))
          ))
      ))
  );

  private static final Match ARRAY_VAR_USAGE = Match.predicate(ctx -> ctx.insn().isVarStore()).save("var-store")
      .and(
          StackMatch.of(0, MethodMatch.invokeVirtual().save("to-primitive") // Converting to primitive type
              .and(ARRAY_ACCESS)
          ).or(ARRAY_ACCESS)
      );

  @Override
  protected void transform(ClassWrapper scope, Context context) throws Exception {
    context.classes(scope).forEach(classWrapper -> classWrapper.methods().stream()
        .filter(methodNode -> methodNode.desc.startsWith("([Ljava/lang/Object;)"))
        .forEach(methodNode -> {
          MethodContext methodContext = MethodContext.framed(classWrapper, methodNode);

          boolean decomposeArgs = false;
          List<Type> newArgumentTypes = new ArrayList<>();

          // ALOAD
          VarInsnNode loadArrayInsn = null;

          Map<Integer, Integer> newVarIndexes = new HashMap<>(); // old var index -> new var index
          int nextVarIndex = MethodHelper.getFirstParameterIdx(methodNode);

          // Find all casts from that Object array
          for (AbstractInsnNode insn : methodNode.instructions.toArray()) {
            InstructionContext insnContext = methodContext.newInsnContext(insn);

            MatchContext matchContext = ARRAY_VAR_USAGE.matchResult(insnContext);
            if (matchContext == null) continue;

            // Found argument!
            VarInsnNode varStore = (VarInsnNode) matchContext.storage().get("var-store").insn();
            TypeInsnNode typeInsn = (TypeInsnNode) matchContext.storage().get("cast").insn();
            int index = matchContext.storage().get("index").insn().asInteger();

            Type type = Type.getObjectType(typeInsn.desc);
            // If value is cast to primitive, then pass primitive
            if (matchContext.storage().containsKey("to-primitive")) {
              MethodInsnNode primitiveCastInsn = (MethodInsnNode) matchContext.storage().get("to-primitive").insn();
              type = getTypeFromPrimitiveCast(primitiveCastInsn);
            }

            // Add new argument
            newArgumentTypes.add(index, type);
            //System.out.println(index + " -> " + type);

            // Append new var index
            newVarIndexes.put(varStore.var, nextVarIndex);
            nextVarIndex = nextVarIndex + type.getSize();

            // Clean up array access
            loadArrayInsn = (VarInsnNode) matchContext.storage().get("load-array").insn();
            for (AbstractInsnNode collectedInsn : matchContext.collectedInsns()) {
              if (collectedInsn.equals(loadArrayInsn)) continue;

              methodNode.instructions.remove(collectedInsn);
            }
            markChange();

            decomposeArgs = true;
          }

          if (decomposeArgs) {
            // Update method arguments
            methodNode.desc = Type.getMethodDescriptor(Type.getReturnType(methodNode.desc), newArgumentTypes.toArray(new Type[0]));

            // Remove array access and pop
            for (AbstractInsnNode insn : methodNode.instructions.toArray()) {
              if (insn.getOpcode() != POP) continue;

              InstructionContext insnContext = methodContext.newInsnContext(insn);

              OriginalSourceValue arrayAccess = insnContext.frame().getStack(insnContext.frame().getStackSize() - 1);
              if (arrayAccess.originalSource.isOneWayProduced() && arrayAccess.originalSource.getProducer().equals(loadArrayInsn)) {
                methodNode.instructions.remove(loadArrayInsn); // ALOAD
                methodNode.instructions.remove(insn); // POP
                markChange();
                break;
              }
            }

            // Replace local variables indexes with the corresponding ones
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
        }));
  }

  private static Type getTypeFromPrimitiveCast(MethodInsnNode insn) {
    if (insn.getOpcode() != INVOKEVIRTUAL) throw new IllegalArgumentException("Instruction is not an INVOKEVIRTUAL");

    if (insn.owner.equals("java/lang/Byte") && insn.name.equals("byteValue")) return Type.BYTE_TYPE;
    if (insn.owner.equals("java/lang/Short") && insn.name.equals("shortValue")) return Type.SHORT_TYPE;
    if (insn.owner.equals("java/lang/Integer") && insn.name.equals("intValue")) return Type.INT_TYPE;
    if (insn.owner.equals("java/lang/Long") && insn.name.equals("longValue")) return Type.LONG_TYPE;
    if (insn.owner.equals("java/lang/Double") && insn.name.equals("doubleValue")) return Type.DOUBLE_TYPE;
    if (insn.owner.equals("java/lang/Float") && insn.name.equals("floatValue")) return Type.FLOAT_TYPE;

    throw new IllegalStateException("Unexpected value: " + insn.owner+"."+insn.name+insn.desc);
  }
}
