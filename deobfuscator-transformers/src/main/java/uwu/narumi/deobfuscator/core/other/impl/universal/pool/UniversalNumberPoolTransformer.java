package uwu.narumi.deobfuscator.core.other.impl.universal.pool;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import uwu.narumi.deobfuscator.api.asm.FieldRef;
import uwu.narumi.deobfuscator.api.asm.MethodContext;
import uwu.narumi.deobfuscator.api.asm.matcher.Match;
import uwu.narumi.deobfuscator.api.asm.matcher.MatchContext;
import uwu.narumi.deobfuscator.api.asm.matcher.group.AnyMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.FieldMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.FrameMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.InsnMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.MethodMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.NumberMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.OpcodeMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.RangeOpcodeMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.VarLoadMatch;
import uwu.narumi.deobfuscator.api.helper.AsmHelper;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

import java.util.Objects;
import java.util.Set;

/**
 * Transforms number pool references to actual values.
 */
public class UniversalNumberPoolTransformer extends Transformer {
  private static final Set<String> PRIMITIVE_OBJECTS = Set.of(
      "java/lang/Byte",
      "java/lang/Short",
      "java/lang/Integer",
      "java/lang/Long",
      "java/lang/Float",
      "java/lang/Double"
  );

  private static final Match CONVERT_TO_PRIMITIVE_MATCH = AnyMatch.of(
      MethodMatch.invokeVirtual().name("intValue").desc("()I"),
      MethodMatch.invokeVirtual().name("byteValue").desc("()B"),
      MethodMatch.invokeVirtual().name("shortValue").desc("()S"),
      MethodMatch.invokeVirtual().name("longValue").desc("()J"),
      MethodMatch.invokeVirtual().name("floatValue").desc("()F")
  );

  private static final Match NUMBER_POOL_METHOD_MATCH = FieldMatch.putStatic().capture("numberPoolField")
      .and(FrameMatch.stack(0,
          OpcodeMatch.of(ANEWARRAY).and(Match.of(ctx -> PRIMITIVE_OBJECTS.contains(((TypeInsnNode) ctx.insn()).desc)))
              .or(OpcodeMatch.of(NEWARRAY))
              .capture("arrayType")
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

      boolean isPrimitiveArray = numberPoolMatchCtx.captures().get("arrayType").insn() instanceof IntInsnNode;

      //System.out.println("Found number pool method in " + classWrapper.name() + "." + numberPoolMatchCtx.insnContext().methodNode().name);

      int numberPoolSize = numberPoolMatchCtx.captures().get("size").insn().asInteger();
      FieldInsnNode numberPoolFieldInsn = (FieldInsnNode) numberPoolMatchCtx.captures().get("numberPoolField").insn();
      FieldRef fieldRefPool = FieldRef.of(numberPoolFieldInsn);

      // Get whole number pool
      Number[] numberPool = getFieldNumberPool(numberPoolMatchCtx.insnContext().methodContext(), numberPoolSize, fieldRefPool);
      if (numberPool == null) {
        LOGGER.warn("Number pool is not fully initialized for {}#{}{}", numberPoolMatchCtx.insnContext().methodContext().classWrapper().name(), numberPoolMatchCtx.insnContext().methodContext().methodNode().name, numberPoolMatchCtx.insnContext().methodContext().methodNode().desc);
        return;
      }

      Match numberPoolReferenceMatch;
      if (isPrimitiveArray) {
        numberPoolReferenceMatch = RangeOpcodeMatch.of(IALOAD, DALOAD).or(RangeOpcodeMatch.of(BALOAD, SALOAD)) // Load array reference
            // Index
            .and(FrameMatch.stack(0, NumberMatch.of().capture("index")))
            // Load number pool field
            .and(FrameMatch.stack(1, FieldMatch.getStatic().fieldRef(fieldRefPool)));
      } else {
        numberPoolReferenceMatch = CONVERT_TO_PRIMITIVE_MATCH
            .and(FrameMatch.stack(0, OpcodeMatch.of(AALOAD) // Load array reference
                // Index
                .and(FrameMatch.stack(0, NumberMatch.of().capture("index")))
                // Load number pool field
                .and(FrameMatch.stack(1, FieldMatch.getStatic().fieldRef(fieldRefPool)))));
      }

      // Replace number pool references with actual values
      classWrapper.methods().forEach(methodNode -> {
        MethodContext methodContext = MethodContext.of(classWrapper, methodNode);

        numberPoolReferenceMatch.findAllMatches(methodContext).forEach(numberPoolReferenceCtx -> {
          int index = numberPoolReferenceCtx.captures().get("index").insn().asInteger();
          Number value = numberPool[index];

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

  /**
   * Get number pool from the field from the method.
   *
   * @param methodContext Method context
   * @param poolSize Size of the pool
   * @param fieldRefPool Field that holds the array of numbers
   * @return The number pool
   */
  public static Number[] getFieldNumberPool(MethodContext methodContext, int poolSize, FieldRef fieldRefPool) {
    Match STORE_NUMBER_TO_ARRAY_OBJ_MATCH = OpcodeMatch.of(AASTORE)
        .and(FrameMatch.stack(0, MethodMatch.invokeStatic().name("valueOf")
            .and(FrameMatch.stack(0, NumberMatch.of().capture("value")))))
        .and(FrameMatch.stack(1, NumberMatch.of().capture("index")))
        .and(FrameMatch.stack(2, FieldMatch.getStatic().fieldRef(fieldRefPool)));

    Match STORE_NUMBER_TO_ARRAY_PRIMITIVE_MATCH = RangeOpcodeMatch.of(IASTORE, DASTORE).or(RangeOpcodeMatch.of(BASTORE, SASTORE))
        .and(FrameMatch.stack(0, NumberMatch.of().capture("value")))
        .and(FrameMatch.stack(1, NumberMatch.of().capture("index")))
        .and(FrameMatch.stack(2, FieldMatch.getStatic().fieldRef(fieldRefPool)));

    Match STORE_NUMBER_TO_ARRAY_MATCH = STORE_NUMBER_TO_ARRAY_OBJ_MATCH.or(STORE_NUMBER_TO_ARRAY_PRIMITIVE_MATCH);

    Number[] numberPool = new Number[poolSize];
    // Collect all numbers from the method
    STORE_NUMBER_TO_ARRAY_MATCH.findAllMatches(methodContext).forEach(storeNumberMatchCtx -> {
      int index = storeNumberMatchCtx.captures().get("index").insn().asInteger();
      Number value = storeNumberMatchCtx.captures().get("value").insn().asNumber();

      numberPool[index] = value;
    });

    for (Number number : numberPool) {
      if (number == null) {
        // Number pool is not fully initialized
        return null;
      }
    }

    return numberPool;
  }

  /**
   * Get number pool from variable from the method.
   *
   * @param methodContext Method context
   * @param poolSize Size of the pool
   * @param storeArrayInsn Store array instruction that we will be referring to
   * @return The number pool
   */
  public static Number[] getVarNumberPool(MethodContext methodContext, int poolSize, VarInsnNode storeArrayInsn) {
    Match STORE_NUMBER_TO_ARRAY_OBJ_MATCH = OpcodeMatch.of(AASTORE)
        .and(FrameMatch.stack(0, MethodMatch.invokeStatic().name("valueOf")
            .and(FrameMatch.stack(0, NumberMatch.of().capture("value")))))
        .and(FrameMatch.stack(1, NumberMatch.of().capture("index")))
        .and(FrameMatch.stack(2, VarLoadMatch.of().localStoreMatch(InsnMatch.of(storeArrayInsn))));

    Match STORE_NUMBER_TO_ARRAY_PRIMITIVE_MATCH = RangeOpcodeMatch.of(IASTORE, DASTORE).or(RangeOpcodeMatch.of(BASTORE, SASTORE))
        .and(FrameMatch.stack(0, NumberMatch.of().capture("value")))
        .and(FrameMatch.stack(1, NumberMatch.of().capture("index")))
        .and(FrameMatch.stack(2, VarLoadMatch.of().localStoreMatch(InsnMatch.of(storeArrayInsn))));

    Match STORE_NUMBER_TO_ARRAY_MATCH = STORE_NUMBER_TO_ARRAY_OBJ_MATCH.or(STORE_NUMBER_TO_ARRAY_PRIMITIVE_MATCH);

    Number[] numberPool = new Number[poolSize];
    // Collect all numbers from the method
    STORE_NUMBER_TO_ARRAY_MATCH.findAllMatches(methodContext).forEach(storeNumberMatchCtx -> {
      int index = storeNumberMatchCtx.captures().get("index").insn().asInteger();
      Number value = storeNumberMatchCtx.captures().get("value").insn().asNumber();
      //System.out.println(index + " -> "+value);

      numberPool[index] = value;
    });

    //System.out.println(Arrays.toString(numberPool));

    for (int i = 0; i < numberPool.length; i++) {
      Number number = numberPool[i];
      if (number == null) {
        // Null values set to 0 to keep with primitive array behavior
        numberPool[i] = 0;
      }
    }

    return numberPool;
  }
}
