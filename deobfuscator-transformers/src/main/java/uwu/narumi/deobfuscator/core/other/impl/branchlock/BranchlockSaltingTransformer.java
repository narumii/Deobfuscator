package uwu.narumi.deobfuscator.core.other.impl.branchlock;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import uwu.narumi.deobfuscator.api.asm.MethodContext;
import uwu.narumi.deobfuscator.api.asm.matcher.Match;
import uwu.narumi.deobfuscator.api.asm.matcher.group.SequenceMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.MethodMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.NumberMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.OpcodeMatch;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

import java.util.Map;
import java.util.WeakHashMap;

public class BranchlockSaltingTransformer extends Transformer {

  public Match saltingTrapMatch = SequenceMatch.of(OpcodeMatch.of(ILOAD), OpcodeMatch.of(ICONST_1), OpcodeMatch.of(IAND), OpcodeMatch.of(IFEQ).or(OpcodeMatch.of(IFNE)).capture("last"));
  public Match saltingInvoke = SequenceMatch.of(NumberMatch.numInteger().capture("salt"), MethodMatch.create().capture("method"));
  public Match saltingXor = SequenceMatch.of(OpcodeMatch.of(ILOAD).capture("param"), NumberMatch.numInteger(), Match.of(ctx -> ctx.insn().isMathOperator() || ctx.insn().isMathOperator()));

  private final Map<MethodInsnNode, Integer> salts = new WeakHashMap<>();

  //TODO: Fix superClass`s / interface`s overridden methods not being deobfuscated

  @Override
  protected void transform() throws Exception {
    scopedClasses().forEach(classWrapper -> {
      classWrapper.methods().forEach(methodNode -> {
        MethodContext methodContext = MethodContext.of(classWrapper, methodNode);

        /* Removing salting traps */
        saltingTrapMatch.findAllMatches(methodContext).forEach(matchContext -> {
          if (matchContext.captures().get("last").insn().next().getOpcode() == GOTO) {
            methodNode.instructions.remove(matchContext.captures().get("last").insn().next());
            matchContext.removeAll();
            markChange();
          }
        });

        /* Save salts */
        saltingInvoke.findAllMatches(methodContext).forEach(matchContext -> {
          int salt = matchContext.captures().get("salt").insn().asInteger();
          MethodInsnNode methodInsnNode = matchContext.captures().get("method").insn().asMethodInsn();
          salts.put(methodInsnNode, salt);
        });
      });
    });
    /* Replace ILOAD var with saved salts */
    scopedClasses().forEach(classWrapper -> {
      classWrapper.methods().forEach(methodNode -> {
        int lastParamIndex = getLastParameterSlot(methodNode.access, methodNode.desc);
        MethodContext methodContext = MethodContext.of(classWrapper, methodNode);
        salts.entrySet().stream().filter(entry -> entry.getKey().owner.equals(classWrapper.name()) && methodNode.desc.equals(entry.getKey().desc) && methodNode.name.equals(entry.getKey().name)).findFirst().ifPresent(entry -> {
          saltingXor.findAllMatches(methodContext).forEach(matchContext -> {
            VarInsnNode varInsnNode = (VarInsnNode) matchContext.insn();
            if (varInsnNode.var == lastParamIndex) {
              methodNode.instructions.set(matchContext.insn(), pushInt(entry.getValue()));
              markChange();
            }
          });
        });
      });
    });
  }

  public AbstractInsnNode pushInt(int value) {
    if (value >= -1 && value <= 5) {
      return new InsnNode(Opcodes.ICONST_0 + value);
    } else if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
      return new IntInsnNode(Opcodes.BIPUSH, value);
    } else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
      return new IntInsnNode(Opcodes.SIPUSH, value);
    } else {
      return new LdcInsnNode(value);
    }
  }

  public int getLastParameterSlot(int access, String desc) {
    Type[] args = Type.getArgumentTypes(desc);
    int index = ((access & Opcodes.ACC_STATIC) != 0) ? 0 : 1;

    for (int i = 0; i < args.length - 1; i++) {
      index += args[i].getSize();
    }
    return index;
  }
}
