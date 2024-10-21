package uwu.narumi.deobfuscator.core.other.impl.pool;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.OriginalSourceValue;
import uwu.narumi.deobfuscator.api.asm.FieldRef;
import uwu.narumi.deobfuscator.api.helper.AsmHelper;
import uwu.narumi.deobfuscator.api.helper.MethodHelper;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Inlines constant static fields
 */
public class InlineStaticFieldTransformer extends Transformer {

  @Override
  protected void transform() throws Exception {
    Set<FieldRef> notConstantFields = new HashSet<>();
    Map<FieldRef, AbstractInsnNode> staticConstantFields = new HashMap<>();

    // Find all static constant fields
    scopedClasses().forEach(classWrapper -> findClInit(classWrapper.classNode()).ifPresent(clInit -> {
      var frames = MethodHelper.analyzeSource(classWrapper.classNode(), clInit);

      Arrays.stream(clInit.instructions.toArray())
          .filter(insn -> insn.getOpcode() == PUTSTATIC)
          .map(FieldInsnNode.class::cast)
          .forEach(insn -> {
            Frame<OriginalSourceValue> frame = frames.get(insn);
            if (frame == null) return;

            FieldRef fieldRef = FieldRef.of(insn);

            if (notConstantFields.contains(fieldRef)) return;

            if (staticConstantFields.containsKey(fieldRef)) {
              // It seems that it is not a constant. Its value is modified elsewhere
              notConstantFields.add(fieldRef);
              staticConstantFields.remove(fieldRef);
              return;
            }

            OriginalSourceValue sourceValue = frame.getStack(frame.getStackSize() - 1);
            if (!sourceValue.originalSource.isOneWayProduced()) return;

            AbstractInsnNode valueInsn = sourceValue.originalSource.getProducer();
            if (valueInsn.isConstant()) {
              // We have constant static field
              staticConstantFields.put(FieldRef.of(insn), valueInsn);
            } else {
              notConstantFields.add(fieldRef);
            }
          });
    }));

    // Also account for FieldNode#value
    scopedClasses().forEach(classWrapper -> {
      classWrapper.classNode().fields.forEach(fieldNode -> {
        if (fieldNode.value != null) {
          FieldRef fieldRef = FieldRef.of(classWrapper.classNode(), fieldNode);

          if (!staticConstantFields.containsKey(fieldRef)) {
            // Add it to static constant fields
            staticConstantFields.put(fieldRef, AsmHelper.toConstantInsn(fieldNode.value));
          }
        }
      });
    });

    // Check if these static fields aren't modified outside clinit
    scopedClasses().forEach(classWrapper -> classWrapper.methods().stream()
        .filter(methodNode -> !methodNode.name.equals("<clinit>"))
        .forEach(methodNode -> {
          Arrays.stream(methodNode.instructions.toArray())
              .filter(insn -> insn.getOpcode() == PUTSTATIC)
              .map(FieldInsnNode.class::cast)
              .forEach(insn -> {
                FieldRef fieldRef = FieldRef.of(insn);
                // Remove not constant field
                staticConstantFields.remove(fieldRef);
              });
        }));

    // Replace static fields accesses with corresponding values
    Set<FieldRef> inlinedFields = new HashSet<>();
    scopedClasses().forEach(classWrapper -> classWrapper.methods().forEach(methodNode -> {
      Arrays.stream(methodNode.instructions.toArray())
          .filter(insn -> insn.getOpcode() == GETSTATIC)
          .map(FieldInsnNode.class::cast)
          .forEach(insn -> {
            FieldRef fieldRef = FieldRef.of(insn);
            AbstractInsnNode constValue = staticConstantFields.get(fieldRef);
            if (constValue != null) {
              // Replace it!
              methodNode.instructions.set(insn, constValue.clone(null));

              // Add to an inlined fields list
              inlinedFields.add(fieldRef);

              this.markChange();
            }
          });
    }));

    // Remove fields that were inlined
    scopedClasses().parallelStream().forEach(classWrapper -> {
      // Remove field
      classWrapper.classNode().fields.removeIf(fieldNode -> inlinedFields.contains(FieldRef.of(classWrapper.classNode(), fieldNode)));

      // Replace PUTSTATIC with POP
      classWrapper.findClInit().ifPresent(methodNode -> {
        Map<AbstractInsnNode, Frame<BasicValue>> frames = MethodHelper.analyzeBasic(classWrapper.classNode(), methodNode);

        Arrays.stream(methodNode.instructions.toArray())
            .filter(insn -> insn.getOpcode() == PUTSTATIC)
            .map(FieldInsnNode.class::cast)
            .forEach(insn -> {
              FieldRef fieldRef = FieldRef.of(insn);
              if (inlinedFields.contains(fieldRef)) {
                Frame<BasicValue> frame = frames.get(insn);
                BasicValue value = frame.getStack(frame.getStackSize() - 1);

                // Replace insn with pop
                methodNode.instructions.set(insn, AsmHelper.toPop(value));
              }
            });
      });
    });

    LOGGER.info("Inlined {} constant static fields", this.getChangesCount());
  }
}
