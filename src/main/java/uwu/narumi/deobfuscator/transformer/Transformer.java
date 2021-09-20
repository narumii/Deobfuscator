package uwu.narumi.deobfuscator.transformer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.Opcodes;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.helper.ASMHelper;

public abstract class Transformer extends ASMHelper implements Opcodes {

    protected static final Logger LOGGER = LogManager.getLogger(Transformer.class);
    protected static final Removeable REMOVEABLE = new Removeable(); //xd

    public abstract void transform(Deobfuscator deobfuscator) throws Exception;

    public String name() {
        return this.getClass().getSimpleName();
    }

    //For FieldNode
    //Wtf i even put this here? xd
    protected static final class Removeable {
        private Removeable() {
        }
    }
}
