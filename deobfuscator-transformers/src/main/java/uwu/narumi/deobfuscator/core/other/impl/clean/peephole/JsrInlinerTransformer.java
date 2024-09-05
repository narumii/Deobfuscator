package uwu.narumi.deobfuscator.core.other.impl.clean.peephole;

import org.objectweb.asm.commons.JSRInlinerAdapter;
import org.objectweb.asm.tree.MethodNode;
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.context.Context;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

import java.util.Arrays;

/**
 * Inlines very old JSR/RET instructions
 */
public class JsrInlinerTransformer extends Transformer {
  @Override
  protected void transform(ClassWrapper scope, Context context) throws Exception {
    context.classes(scope).forEach(classWrapper -> {
      for (int i = 0; i < classWrapper.methods().size(); i++) {
        MethodNode methodNode = classWrapper.methods().get(i);

        boolean isJsr = Arrays.stream(methodNode.instructions.toArray()).anyMatch(insn -> insn.getOpcode() == JSR);

        if (isJsr) {
          MethodNode clonedMethodNode = new MethodNode(
              methodNode.access,
              methodNode.name,
              methodNode.desc,
              methodNode.signature,
              methodNode.exceptions.toArray(new String[0])
          );

          // Inline JSR instructions
          methodNode.accept(new JSRInlinerAdapter(
              clonedMethodNode,
              methodNode.access,
              methodNode.name,
              methodNode.desc,
              methodNode.signature,
              methodNode.exceptions.toArray(new String[0])
          ));

          // Replace method with the jsr inlined version
          classWrapper.methods().set(i, clonedMethodNode);

          markChange();
        }
      }
    });
  }
}
