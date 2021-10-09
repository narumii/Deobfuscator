package uwu.narumi.deobfuscator.transformer.impl.qprotect.b3_0;

import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.transformer.Transformer;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

/*
    This transformer works on versions: 3.0-b1 and b31
 */
public class qProtectFlowTransformer extends Transformer {

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        deobfuscator.classes().forEach(classNode -> {
            AtomicBoolean hasFlow = new AtomicBoolean();
            classNode.methods.forEach(methodNode -> Arrays.stream(methodNode.instructions.toArray())
                    .filter(node -> node instanceof JumpInsnNode)
                    .filter(node -> node.getOpcode() != GOTO)
                    .filter(node -> node.getPrevious().getOpcode() == GETSTATIC)
                    .filter(node -> ((FieldInsnNode) node.getPrevious()).desc.equals("Z"))
                    .filter(node -> node.getNext().getOpcode() == ACONST_NULL)
                    .filter(node -> node.getNext().getNext().getOpcode() == ATHROW)
                    .map(JumpInsnNode.class::cast)
                    .forEach(node -> {
                        hasFlow.set(true);
                        methodNode.instructions.insertBefore(node.getPrevious(), new JumpInsnNode(GOTO, node.label));
                    }));

            //I think this can remove normal fields also
            if (hasFlow.get())
                classNode.fields.removeIf(fieldNode -> fieldNode.desc.equals("Z") && fieldNode.name.charAt(0) > 127 && fieldNode.name.charAt(fieldNode.name.length() - 1) > 127);
        });
    }
}
