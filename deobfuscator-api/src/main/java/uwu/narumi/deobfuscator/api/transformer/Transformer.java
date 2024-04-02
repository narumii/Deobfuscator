package uwu.narumi.deobfuscator.api.transformer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.Opcodes;
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.context.Context;
import uwu.narumi.deobfuscator.api.helper.AsmHelper;

public abstract class Transformer extends AsmHelper implements Opcodes {

  protected static final Logger LOGGER = LogManager.getLogger(Transformer.class);

  public void transform(Context context) throws Exception {
    transform(null, context);
  }

  public abstract void transform(ClassWrapper scope, Context context) throws Exception;

  public String name() {
    return this.getClass().getSimpleName();
  }
}
