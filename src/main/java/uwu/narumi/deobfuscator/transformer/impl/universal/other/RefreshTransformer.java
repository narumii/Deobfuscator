package uwu.narumi.deobfuscator.transformer.impl.universal.other;

import me.coley.cafedude.InvalidClassException;
import org.objectweb.asm.tree.ClassNode;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.helper.ClassHelper;
import uwu.narumi.deobfuscator.transformer.Transformer;

import java.util.HashMap;
import java.util.Map;

/*
    Some times can be helpful
 */
public class RefreshTransformer extends Transformer {

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        Map<String, ClassNode> newClasses = new HashMap<>();
        deobfuscator.classes().forEach(classNode -> {
            try {
                newClasses.put(classNode.name,
                        ClassHelper.loadClass(ClassHelper.classToBytes(classNode, deobfuscator.getClassWriterFlags()), deobfuscator.getClassReaderFlags())
                );
            } catch (InvalidClassException e) {
                e.printStackTrace();
            }
        });

        deobfuscator.getClasses().clear();
        deobfuscator.getClasses().putAll(newClasses);
        newClasses.clear(); //idk
    }
}
