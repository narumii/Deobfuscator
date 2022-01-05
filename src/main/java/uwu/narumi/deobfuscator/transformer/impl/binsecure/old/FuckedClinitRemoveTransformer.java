package uwu.narumi.deobfuscator.transformer.impl.binsecure.latest;

import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.transformer.Transformer;

public class FuckedClinitRemoveTransformer extends Transformer {

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        deobfuscator.classes().forEach(classNode -> classNode.methods.removeIf(methodNode -> {
            if (methodNode.name.equals("<clinit>")) {
                if (!methodNode.desc.equals("()V")) {
             //       System.out.println("C "+ classNode.name +" M "+methodNode.name + " D " + methodNode.desc);
                    return true;
                }
            }
            return false;
        }));
        deobfuscator.getOriginalClasses().forEach((s, classNode) -> classNode.methods.removeIf(methodNode -> {
            if (methodNode.name.equals("<clinit>")) {
                if (!methodNode.desc.equals("()V")) {
                 //   System.out.println("C "+ classNode.name +" M "+methodNode.name + " D " + methodNode.desc);
                    return true;
                }
            }
            return false;
        }));

        /*  classNode.methods.removeIf(methodNode -> methodNode.name.equals("<clinit>") && !methodNode.desc.equals("()V"))*/
    }
}
