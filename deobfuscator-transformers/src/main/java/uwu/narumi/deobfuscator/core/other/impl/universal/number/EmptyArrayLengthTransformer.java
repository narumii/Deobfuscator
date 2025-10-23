package uwu.narumi.deobfuscator.core.other.impl.universal.number;

import org.objectweb.asm.tree.*;
import uwu.narumi.deobfuscator.api.helper.AsmHelper;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

public class EmptyArrayLengthTransformer extends Transformer {
  @Override
  protected void transform() throws Exception {
    scopedClasses().parallelStream()
      .forEach(classWrapper -> classWrapper.methods().parallelStream().forEach(methodNode -> {
        InsnList list = methodNode.instructions;
        list.forEach(insn -> {
          if (insn.getOpcode() == ARRAYLENGTH) {
            AbstractInsnNode arrayInsn = insn.getPrevious();
            if (arrayInsn != null && arrayInsn.getOpcode() == NEWARRAY) {
              AbstractInsnNode sizeInsn = arrayInsn.getPrevious();
              Integer constValue = getInt(sizeInsn);
              if (constValue != null) {
                list.set(insn, AsmHelper.numberInsn(constValue));
                list.remove(arrayInsn);
                list.remove(sizeInsn);
              }
            }
          }
        });
      }));
  }

  private Integer getInt(AbstractInsnNode insn) {
    if (insn instanceof InsnNode) {
      switch (insn.getOpcode()) {
        case ICONST_M1: return -1;
        case ICONST_0: return 0;
        case ICONST_1: return 1;
        case ICONST_2: return 2;
        case ICONST_3: return 3;
        case ICONST_4: return 4;
        case ICONST_5: return 5;
      }
    } else if (insn instanceof IntInsnNode iinsn) {
      return iinsn.operand;
    } else if (insn instanceof LdcInsnNode linsn) {
      Object cst = linsn.cst;
      if (cst instanceof Integer) return (Integer) cst;
    }
    return null;
  }
}
