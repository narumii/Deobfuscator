package uwu.narumi.deobfuscator.transformer.impl.bozoriusz;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.ClassNode;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.transformer.Transformer;

import java.util.function.Predicate;

public class BozoriuszShitRemoverTransformer extends Transformer {

    private final Predicate<String> namePredicate = (s -> s.split("\u0001/", 69).length > 3 || s.split("\u0020").length > 3);
    private final Predicate<ClassNode> contentPredicate = (cn -> cn.fields.isEmpty() && cn.methods.get(0).name.equals("\u0001"));

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        deobfuscator.classes().removeIf(classNode -> namePredicate.test(classNode.name)
                || contentPredicate.test(classNode));
        deobfuscator.getFiles().entrySet().removeIf(entry -> namePredicate.test(entry.getKey()));
    }
}
