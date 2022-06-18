package uwu.narumi.deobfuscator.transformer.impl.bozoriusz;

import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.transformer.Transformer;

import java.util.function.Predicate;

public class BozoriuszShitRemoverTransformer extends Transformer {

    private final Predicate<String> predicate = s -> s.split("\u0001/", 69).length > 3 || s.split("\u0020").length > 3 || s.contains("OBFUSCATED WITH BOZAR");

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        deobfuscator.classes().removeIf(classNode -> predicate.test(classNode.name));
        deobfuscator.getFiles().entrySet().removeIf(entry -> predicate.test(entry.getKey()));
    }
}
