package uwu.narumi.deobfuscator.core.other.impl.clean.peephole;

import org.objectweb.asm.commons.JSRInlinerAdapter;
import org.objectweb.asm.tree.MethodNode;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

import java.util.Arrays;

/**
 * Inlines very old JSR/RET instructions
 */
public class JsrInlinerTransformer extends Transformer {
  @Override
  protected void transform() throws Exception {
    scopedClasses().forEach(classWrapper -> {
      for (int i = 0; i < classWrapper.methods().size(); i++) {
        MethodNode methodNode = classWrapper.methods().get(i);

        boolean isJsr = Arrays.stream(methodNode.instructions.toArray()).anyMatch(insn -> insn.getOpcode() == JSR);

        if (isJsr) {
          // Inline JSR instructions
          final JSRInlinerAdapter adapter = new JSRInlinerAdapter(
              methodNode,
              methodNode.access,
              methodNode.name,
              methodNode.desc,
              methodNode.signature,
              methodNode.exceptions.toArray(new String[0])
          );

          methodNode.accept(adapter);
          classWrapper.methods().set(i, adapter);

          markChange();
        }
      }
    });
  }
}
