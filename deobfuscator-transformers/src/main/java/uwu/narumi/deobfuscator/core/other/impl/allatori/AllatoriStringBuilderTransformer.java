/*
             MODIFIED DO WHAT THE FUCK YOU WANT TO PUBLIC LICENSE
                     Version 2, December 2004

  Copyright (C) 2025 d1cku5er

  Everyone is permitted to copy and distribute verbatim or modified
  copies of this license document, and changing it is allowed as long
  as the name is changed.

             MODIFIED DO WHAT THE FUCK YOU WANT TO PUBLIC LICENSE
    TERMS AND CONDITIONS FOR COPYING, DISTRIBUTION AND MODIFICATION

   0. You just DO WHAT THE FUCK YOU WANT TO.


 */
package uwu.narumi.deobfuscator.core.other.impl.allatori;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.MethodInsnNode;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

import java.util.Arrays;

/**
 * <p>Decompiler-friendly transformer, might change something looks like
 * <pre><code>
 *   new StringBuilder().insert(0, "d1ck").append("u5er").toString()
 * </code></pre>
 * to
 * <pre><code>
 *   "d1ck" + "u5er"
 * </code></pre>
 * <br>
 *
 * <strong>ALERT: Only works with some specified decompilers, might break
 * the program</strong>
 *
 * @author d1cku5er
 * @apiNote You might need to use InlineConstantValuesTransformer before using this
 */
public class AllatoriStringBuilderTransformer extends Transformer {
  @Override protected void transform () throws Exception {
    scopedClasses().forEach(cw -> {
      cw.methods()
          .forEach(mn -> {
            /*
            (POP) org.objectweb.asm.tree.InsnNode@a
            (POP) org.objectweb.asm.tree.InsnNode@b
            (ICONST_0) org.objectweb.asm.tree.InsnNode@c
            (LDC) org.objectweb.asm.tree.LdcInsnNode@d
            insert(int String)
            (INVOKEVIRTUAL) org.objectweb.asm.tree.MethodInsnNode@e
            */
            Arrays.stream(mn.instructions.toArray())
                .forEach(ain -> {
                  if (ain != null) {
                    if (
                      // Invoke virtual check
                        ain.getOpcode() == Opcodes.INVOKEVIRTUAL
                        // Method call
                        && ain.isMethodInsn() && ain.asMethodInsn().name.equals("insert")
                        // int, java.lang.String
                        && ain.asMethodInsn().desc.equals("(ILjava/lang/String;)Ljava/lang/StringBuilder;")
                        // iconst zero
                        && ain.previous().previous().getOpcode() == Opcodes.ICONST_0

                    ) {
                      mn.instructions.remove(ain.previous().previous());
                      ((MethodInsnNode) ain).name = "append";
                      ((MethodInsnNode) ain).desc = "(Ljava/lang/String;)Ljava/lang/StringBuilder;";
                      markChange();
                      /*
                      (POP) org.objectweb.asm.tree.InsnNode@a
                      (POP) org.objectweb.asm.tree.InsnNode@b
                      (LDC) org.objectweb.asm.tree.LdcInsnNode@d
                      append(String)
                      (INVOKEVIRTUAL) org.objectweb.asm.tree.MethodInsnNode@e
                      */
                    }
                  }
                });
          });
    });
  }
}
