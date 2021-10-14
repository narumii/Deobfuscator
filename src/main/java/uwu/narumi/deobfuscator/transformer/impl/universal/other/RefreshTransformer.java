package uwu.narumi.deobfuscator.transformer.impl.universal.other;

import me.coley.cafedude.InvalidClassException;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.helper.ClassHelper;
import uwu.narumi.deobfuscator.transformer.Transformer;

/*
    Some times can be helpful
 */
public class RefreshTransformer extends Transformer {

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        deobfuscator.classes().forEach(classNode -> {
            try {
                deobfuscator.getClasses().put(classNode.name,
                        ClassHelper.loadClass(ClassHelper.classToBytes(classNode, deobfuscator.getClassWriterFlags()), deobfuscator.getClassReaderFlags())
                );
            } catch (InvalidClassException e) {
                e.printStackTrace();
            }
        });
    }
}
