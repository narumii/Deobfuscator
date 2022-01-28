package uwu.narumi.deobfuscator.transformer.impl.rakszild;

import org.objectweb.asm.tree.MethodNode;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.transformer.Transformer;

import java.util.ArrayList;
import java.util.List;

public class RakSzildTrashMethodsRemoveTransformer extends Transformer {

    /*
    Moj stary po piwie zrobilby lepszy transformer "trash methods"
     */
    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        LOGGER.warn("# I think this transformer can remove normal methods too");

        //tak removeIf nie istnieje
        deobfuscator.classes().forEach(classNode -> {
            List<MethodNode> toRemove = new ArrayList<>();

            classNode.methods.stream()
                    .filter(node -> node.name.length() == 20)
                    .filter(node -> node.localVariables != null)
                    .filter(node -> node.localVariables.size() == 2)
                    .filter(node -> node.localVariables.stream().anyMatch(var -> var.name.length() >= 16 && var.desc.equals("I")))
                    .forEach(toRemove::add);

            classNode.methods.removeAll(toRemove);
        });
    }
}
