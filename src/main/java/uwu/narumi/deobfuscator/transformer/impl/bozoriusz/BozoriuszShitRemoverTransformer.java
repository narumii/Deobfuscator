package uwu.narumi.deobfuscator.transformer.impl.bozoriusz;

import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.transformer.Transformer;

import java.util.function.Predicate;

public class BozoriuszShitRemoverTransformer extends Transformer {

    private final Predicate<String> predicate = s -> s.split("\u0001/", 69).length > 3 || s.split("\u0020").length > 3;

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        deobfuscator.classes().removeIf(classNode -> predicate.test(classNode.name)
                || classNode.methods.isEmpty() && classNode.fields.isEmpty());
        deobfuscator.getFiles().entrySet().removeIf(entry -> predicate.test(entry.getKey()));
    }
}
