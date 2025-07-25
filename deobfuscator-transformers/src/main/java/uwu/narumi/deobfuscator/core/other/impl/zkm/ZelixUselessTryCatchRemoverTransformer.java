package uwu.narumi.deobfuscator.core.other.impl.zkm;

import uwu.narumi.deobfuscator.api.asm.InsnContext;
import uwu.narumi.deobfuscator.api.asm.MethodContext;
import uwu.narumi.deobfuscator.api.asm.matcher.Match;
import uwu.narumi.deobfuscator.api.asm.matcher.group.SequenceMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.InvokeDynamicMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.NumberMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.OpcodeMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.VarLoadMatch;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

/**
 * <pre>
 *   {@code
 *   try {
 *        ...
 *   }
 *   catch (RuntimeException runtimeException) {
 *       throw class_16.method_169("£", (Object)runtimeException, (long)-140537288428847988L, (long)l);
 *   }
 *   }
 * </pre>
 *
 */

public class ZelixUselessTryCatchRemoverTransformer extends Transformer {

  private static final SequenceMatch InvokeD_ATHROW =
      SequenceMatch.of(
          NumberMatch.of(),
          VarLoadMatch.of(),
          InvokeDynamicMatch.create(),
          OpcodeMatch.of(ATHROW)
      );
/*
LDC -140537288428847988L
LLOAD a1
INVOKEDYNAMIC £ (Ljava/lang/Object;JJ)Ljava/lang/RuntimeException; handle[H_INVOKESTATIC cn/cool/cherish/module/impl/combat/友何友树何何树友树何.d(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;] args[]
ATHROW
 */
  @Override
  protected void transform() throws Exception {
    scopedClasses().forEach(classWrapper -> {
      classWrapper.methods().forEach(methodNode -> {
        MethodContext methodContext = MethodContext.of(classWrapper, methodNode);

        methodNode.tryCatchBlocks.removeIf(tryBlock -> {
          InsnContext start = methodContext.at(tryBlock.handler.getNext());
          if(InvokeD_ATHROW.matches(start)){
            methodNode.instructions.remove(tryBlock.handler.getNext());
            methodNode.instructions.remove(tryBlock.handler.getNext());
            methodNode.instructions.remove(tryBlock.handler.getNext());
            methodNode.instructions.remove(tryBlock.handler.getNext());
            markChange();
            //System.out.println("TryCatch Block");
            return true;
          }
          return false;
        });
      });
    });
  }
}
