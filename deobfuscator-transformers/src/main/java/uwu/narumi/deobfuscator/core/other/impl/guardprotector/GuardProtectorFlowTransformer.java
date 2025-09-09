package uwu.narumi.deobfuscator.core.other.impl.guardprotector;

import org.objectweb.asm.tree.*;
import uwu.narumi.deobfuscator.api.asm.MethodContext;
import uwu.narumi.deobfuscator.api.asm.matcher.Match;
import uwu.narumi.deobfuscator.api.asm.matcher.group.SequenceMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.OpcodeMatch;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

import java.util.HashSet;
import java.util.Set;

public class GuardProtectorFlowTransformer extends Transformer {

  Match useless0Switch = SequenceMatch.of(OpcodeMatch.of(ICONST_0), OpcodeMatch.of(LOOKUPSWITCH)).doNotSkip();
  Match flowVarStore = SequenceMatch.of(Match.of(ctx -> {
    AbstractInsnNode abstractInsnNode = ctx.insn();
    return abstractInsnNode.getOpcode() == GETSTATIC && abstractInsnNode.isFieldInsn() && (abstractInsnNode.asFieldInsn().desc.equals("I") || abstractInsnNode.asFieldInsn().desc.equals("Z"));
  }).capture("stored-type"), OpcodeMatch.of(ISTORE).capture("stored-var"));
  Match flowVarEquations = SequenceMatch.of(OpcodeMatch.of(ILOAD).capture("loaded-var"), OpcodeMatch.of(IFEQ).or(OpcodeMatch.of(IFNE)).capture("stored-jump"));
  Match flowTableSwitch = SequenceMatch.of(OpcodeMatch.of(ILOAD).capture("loaded-var"), OpcodeMatch.of(TABLESWITCH).capture("switch"));

  @Override
  protected void transform() throws Exception {
    scopedClasses().forEach(classWrapper -> classWrapper.methods().forEach(methodNode -> {
      MethodContext methodContext = MethodContext.of(classWrapper, methodNode);
      useless0Switch.findAllMatches(methodContext).forEach(matchContext -> {
        matchContext.removeAll();
        markChange();
      });
      Set<AbstractInsnNode> toRemove = new HashSet<>();
      flowVarStore.findAllMatches(methodContext).forEach(varStore -> {
        FieldInsnNode fieldInsn = varStore.captures().get("stored-type").insn().asFieldInsn();
        if (!hasPutStatic(classWrapper.classNode(), fieldInsn)) {
//          FieldRef fieldRef = FieldRef.of(fieldInsn); //This doesn`t work. Fields are still present in classes (maybe because they are private static final)
//          context().removeField(fieldRef);
          boolean ifNeJump = fieldInsn.desc.equals("Z");
          int varIndex = ((VarInsnNode) varStore.captures().get("stored-var").insn()).var;
          toRemove.addAll(varStore.collectedInsns());
          flowVarEquations.findAllMatches(methodContext).forEach(jumpEquation -> {
            int loadedVarIndex = ((VarInsnNode) jumpEquation.captures().get("loaded-var").insn()).var;
            JumpInsnNode jump = jumpEquation.captures().get("stored-jump").insn().asJump();
            if ((ifNeJump && jump.getOpcode() == IFNE) || (!ifNeJump && jump.getOpcode() == IFEQ) && varIndex == loadedVarIndex) {
              if (jump.label != null) {
                toRemove.add(jump.label);
                markChange();
                methodNode.instructions.forEach(insn -> {
                  if (isInsnInLabelRange(methodNode, jump.label, insn)) {
                    toRemove.add(insn);
                  }
                });
              }
              toRemove.addAll(jumpEquation.collectedInsns());
              markChange();
            }
          });
          if (!ifNeJump) {
            flowTableSwitch.findAllMatches(methodContext).forEach(flowSwitch -> {
              int loadedVarIndex = ((VarInsnNode) flowSwitch.captures().get("loaded-var").insn()).var;
              if (varIndex == loadedVarIndex) {
                toRemove.addAll(flowSwitch.collectedInsns());
                TableSwitchInsnNode tableSwitchInsnNode = (TableSwitchInsnNode)flowSwitch.captures().get("switch").insn();
                tableSwitchInsnNode.labels.forEach(labelNode -> {
                  toRemove.add(labelNode);
                  markChange();
                  methodNode.instructions.forEach(insn -> {
                    if (isInsnInLabelRange(methodNode, labelNode, insn)) {
                      toRemove.add(insn);
                    }
                  });
                });
              }
            });
          }
        }
      });
      toRemove.forEach(methodNode.instructions::remove);
    }));
  }

  public boolean isInsnInLabelRange(MethodNode method, LabelNode startLabel, AbstractInsnNode insn) {
    InsnList instructions = method.instructions;

    int startIndex = instructions.indexOf(startLabel);
    if (startIndex == -1) return false;

    int insnIndex = instructions.indexOf(insn);
    if (insnIndex == -1) return false;

    int endIndex = instructions.size();
    for (int i = startIndex + 1; i < instructions.size(); i++) {
      if (instructions.get(i) instanceof LabelNode) {
        endIndex = i;
        break;
      }
    }

    return insnIndex > startIndex && insnIndex < endIndex;
  }

  boolean hasPutStatic(ClassNode classNode, FieldInsnNode target) {
    for (MethodNode method : classNode.methods) {
      for (AbstractInsnNode insn : method.instructions) {
        if (insn instanceof FieldInsnNode) {
          FieldInsnNode fin = (FieldInsnNode) insn;
          if (fin.getOpcode() == PUTSTATIC
              && fin.owner.equals(target.owner)
              && fin.name.equals(target.name)
              && fin.desc.equals(target.desc)) {
            return true;
          }
        }
      }
    }
    return false;
  }
}
