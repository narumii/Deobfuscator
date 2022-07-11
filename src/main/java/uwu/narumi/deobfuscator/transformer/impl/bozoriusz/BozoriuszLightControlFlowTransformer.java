package uwu.narumi.deobfuscator.transformer.impl.bozoriusz;

import org.objectweb.asm.tree.*;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.transformer.Transformer;

import java.util.Arrays;

public class BozoriuszLightControlFlowTransformer extends Transformer {

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        deobfuscator.classes().forEach(classNode -> {
            classNode.fields.removeIf(field -> (field.name.equals("Ꮸ") || field.name.matches("[Il]{50,}")) && field.desc.equals("J"));
            classNode.methods.forEach(methodNode -> {

                //Light type 2
                Arrays.stream(methodNode.instructions.toArray())
                        .filter(node -> node instanceof FieldInsnNode)
                        .map(FieldInsnNode.class::cast)
                        .filter(node -> node.name.equals("Ꮸ") || node.name.matches("[Il]{50,}"))
                        .filter(node -> node.desc.equals("J"))
                        .filter(node -> node.owner.contains(classNode.name))
                        .filter(node -> isNumber(node.getNext()))
                        .filter(node -> node.getPrevious().getPrevious().getPrevious().getPrevious().getOpcode() == GOTO)
                        .forEach(node -> {
                            int index = methodNode.instructions.indexOf(node);
                            AbstractInsnNode icmpne = methodNode.instructions.get(index + 6);
                            AbstractInsnNode gotoEnd = methodNode.instructions.get(index + 8);

                            if (icmpne.getOpcode() == IF_ICMPNE && gotoEnd.getOpcode() == GOTO && ((JumpInsnNode) gotoEnd).label.getPrevious().getOpcode() == GOTO) {
                                getInstructionsBetween(
                                        node.getPrevious().getPrevious().getPrevious().getPrevious(),
                                        icmpne,
                                        true,
                                        true
                                ).forEach(methodNode.instructions::remove);

                                getInstructionsBetween(
                                        gotoEnd,
                                        ((JumpInsnNode) gotoEnd).label,
                                        true,
                                        false
                                ).forEach(methodNode.instructions::remove);
                            }
                        });

                //Light type 2
                Arrays.stream(methodNode.instructions.toArray())
                        .filter(node -> node instanceof FieldInsnNode)
                        .map(FieldInsnNode.class::cast)
                        .filter(node -> node.name.equals("Ꮸ") || node.name.matches("[Il]{50,}"))
                        .filter(node -> node.desc.equals("J"))
                        .filter(node -> node.owner.contains(classNode.name))
                        .filter(node -> node.getNext().getOpcode() == GOTO)
                        .forEach(node -> {
                            AbstractInsnNode switchNode = ((JumpInsnNode) node.getNext()).label.getNext().getNext();
                            if (switchNode instanceof LookupSwitchInsnNode) {
                                LabelNode before = ((LookupSwitchInsnNode) switchNode).dflt;
                                if (is(before.getNext()) && before.getPrevious().getOpcode() == ATHROW) {
                                    getInstructionsBetween(
                                            node,
                                            before,
                                            true,
                                            false
                                    ).forEach(methodNode.instructions::remove);
                                }
                            }
                        });
            });
        });
    }

    private boolean is(AbstractInsnNode node) {
        return node.getOpcode() == NEW || node instanceof FieldInsnNode || node instanceof MethodInsnNode || node instanceof InvokeDynamicInsnNode;
    }
}
