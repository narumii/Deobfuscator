package uwu.narumi.deobfuscator.transformer.impl.binsecure.latest;

import org.objectweb.asm.tree.MethodInsnNode;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.exception.TransformerException;
import uwu.narumi.deobfuscator.transformer.Transformer;

/*
    Hard coded shit
 */
public class BinsecureInvokeDynamicVariableTransformer extends Transformer {

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        throw new UnsupportedOperationException();
    }

    private int getOpcode(MethodInsnNode node) {
        boolean store = node.name.startsWith("put");
        switch (node.name.substring(3)) {
            case "Int": {
                return store ? ISTORE : ILOAD;
            }
            case "Long": {
                return store ? LSTORE : LLOAD;
            }
            case "Float": {
                return store ? FSTORE : FLOAD;
            }
            case "Double": {
                return store ? DSTORE : DLOAD;
            }
            default:
                throw new TransformerException();
        }
    }

}
