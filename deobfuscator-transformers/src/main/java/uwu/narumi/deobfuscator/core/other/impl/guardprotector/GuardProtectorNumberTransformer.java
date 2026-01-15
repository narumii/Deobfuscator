package uwu.narumi.deobfuscator.core.other.impl.guardprotector;

import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import uwu.narumi.deobfuscator.api.asm.MethodContext;
import uwu.narumi.deobfuscator.api.asm.matcher.Match;
import uwu.narumi.deobfuscator.api.asm.matcher.MatchContext;
import uwu.narumi.deobfuscator.api.asm.matcher.group.SequenceMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.NumberMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.OpcodeMatch;
import uwu.narumi.deobfuscator.api.transformer.Transformer;
import uwu.narumi.deobfuscator.core.other.impl.universal.UniversalNumberTransformer;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;

public class GuardProtectorNumberTransformer extends Transformer {

  Match longArrayInit = SequenceMatch.of(NumberMatch.of().capture("array-size"), Match.of(ctx -> {
    if (ctx.insn() instanceof IntInsnNode intInsn) {
      return intInsn.operand == T_LONG;
    }
    return false;
  }), OpcodeMatch.of(PUTSTATIC).capture("array"));

  Match putStatic = SequenceMatch.of(OpcodeMatch.of(GETSTATIC), NumberMatch.of().capture("array-index"), NumberMatch.of().capture("array-value"), OpcodeMatch.of(LASTORE));
  Match getStatic = SequenceMatch.of(OpcodeMatch.of(GETSTATIC).capture("array"), NumberMatch.of().capture("array-index"), OpcodeMatch.of(LALOAD));

  @Override
  protected void transform() throws Exception {
    scopedClasses().forEach(classWrapper -> {
      HashMap<Integer, Long> longValues = new HashMap<>();
      AtomicReference<String> fieldArrayName = new AtomicReference<>();
      classWrapper.findClInit().ifPresent(clinit -> {
        MethodContext methodContext = MethodContext.of(classWrapper, clinit);
        MatchContext clinitMatch = longArrayInit.findFirstMatch(methodContext);
        if (clinitMatch != null) {
          fieldArrayName.set(clinitMatch.captures().get("array").insn().asFieldInsn().name);
          putStatic.findAllMatches(methodContext).forEach(putStatic -> {
            int arrayIndex = putStatic.captures().get("array-index").insn().asNumber().intValue();
            long arrayValue = putStatic.captures().get("array-value").insn().asNumber().longValue();
            longValues.put(arrayIndex, arrayValue);
            putStatic.removeAll();
          });
          clinitMatch.removeAll();
        }
      });
      classWrapper.methods().forEach(methodNode -> {
        MethodContext methodContext = MethodContext.of(classWrapper, methodNode);
        getStatic.findAllMatches(methodContext).forEach(matchContext -> {
          if (matchContext.captures().get("array").insn().asFieldInsn().name.equals(fieldArrayName.get())) {
            methodNode.instructions.insert(matchContext.insn(), new LdcInsnNode(longValues.get(matchContext.captures().get("array-index").insn().asInteger())));
            matchContext.removeAll();
          }
        });
      });
      Transformer.transform(UniversalNumberTransformer::new, classWrapper, context());
    });
  }
}
