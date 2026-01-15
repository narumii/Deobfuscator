package uwu.narumi.deobfuscator.core.other.impl.zkm;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import uwu.narumi.deobfuscator.api.asm.FieldRef;
import uwu.narumi.deobfuscator.api.asm.MethodContext;
import uwu.narumi.deobfuscator.api.asm.matcher.Match;
import uwu.narumi.deobfuscator.api.asm.matcher.group.SequenceMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.FieldMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.FrameMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.OpcodeMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.VarLoadMatch;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

import java.util.HashSet;
import java.util.Set;

public class ZelixFieldFlowTransformer extends Transformer {

  /**
   * getstatic d.b I
   * istore i1
   * ...
   * iload i1
   * ifeq J
   * getstatic e.d I
   * istore i2
   * iinc i2 1
   * iload i2
   * putstatic e.d I
   */
  private static final Match FIELD_FLOW_VARIANT_1 = SequenceMatch.of(
      OpcodeMatch.of(ILOAD).and(VarLoadMatch.of().localStoreMatch(
          FrameMatch.stackOriginal(0, FieldMatch.getStatic().capture("flow-field1")).capture("var-load")
      )),
      OpcodeMatch.of(IFEQ),
      FieldMatch.getStatic().capture("flow-field2-get"),
      OpcodeMatch.of(ISTORE),
      OpcodeMatch.of(IINC),
      OpcodeMatch.of(ILOAD),
      FieldMatch.putStatic().capture("flow-field2-put")
  );

  /**
   * getstatic e.d I
   * ifeq AN
   * iinc i3 1
   * iload i3
   * putstatic d.b I
   */
  private static final Match FIELD_FLOW_VARIANT_2 = SequenceMatch.of(
    FieldMatch.getStatic().capture("flow-field-get"),
    OpcodeMatch.of(IFEQ),
    OpcodeMatch.of(IINC),
    OpcodeMatch.of(ILOAD),
    FieldMatch.putStatic().capture("flow-field-put")
  );

  @Override
  protected void transform() throws Exception {
    scopedClasses().forEach(classWrapper -> classWrapper.methods().forEach(methodNode -> {
      Set<FieldRef> flowFields = new HashSet<>();

      MethodContext methodContext = MethodContext.of(classWrapper, methodNode);
      FIELD_FLOW_VARIANT_1.findAllMatches(methodContext).forEach(matchContext -> {
        // For cleanup purposes
        AbstractInsnNode varLoadInsn = matchContext.captures().get("var-load").insn();

        FieldRef field1 = FieldRef.of((FieldInsnNode) matchContext.captures().get("flow-field1").insn());
        FieldRef field2Get = FieldRef.of((FieldInsnNode) matchContext.captures().get("flow-field2-get").insn());
        FieldRef field2Put = FieldRef.of((FieldInsnNode) matchContext.captures().get("flow-field2-put").insn());
        if (!field2Get.equals(field2Put)) {
          // These fields should be the same
          return;
        }

        LOGGER.info("Zelix field flow variant 1 found in {}.{}: {}, {}", classWrapper.name(), methodNode.name, field1, field2Get);
        flowFields.add(field1);
        flowFields.add(field2Get);

        for (AbstractInsnNode insn : matchContext.collectedInsns()) {
          // Skip certain instructions to keep stack consistency
          if (insn == varLoadInsn) continue;
          if (insn == matchContext.captures().get("flow-field1").insn()) continue;

          methodNode.instructions.remove(insn);
        }
        markChange();
      });

      FIELD_FLOW_VARIANT_2.findAllMatches(methodContext).forEach(matchContext -> {
        FieldRef fieldGet = FieldRef.of((FieldInsnNode) matchContext.captures().get("flow-field-get").insn());
        FieldRef fieldPut = FieldRef.of((FieldInsnNode) matchContext.captures().get("flow-field-put").insn());

        LOGGER.info("Zelix field flow variant 2 found in {}.{}: {}, {}", classWrapper.name(), methodNode.name, fieldGet, fieldPut);
        flowFields.add(fieldGet);
        flowFields.add(fieldPut);

        matchContext.removeAll();
        markChange();
      });

//      if (flowFields.isEmpty()) return;

//      LOGGER.info("Flow fields found in {}.{}: {}", classWrapper.name(), methodNode.name, flowFields);
    }));
  }
}
