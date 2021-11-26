package uwu.narumi.deobfuscator.transformer.impl.qprotect.latest;

import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.SourceValue;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.transformer.Transformer;

import java.util.Arrays;
import java.util.Map;

/*
    WUHFIOWAIOWHAIOFAIOWHFI9WAIOSD XD?
 */
public class qProtectFlowTransformer extends Transformer {


    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        deobfuscator.classes().forEach(classNode -> {
            classNode.methods.forEach(methodNode -> {
                firstPhase(methodNode);
                secondPhase(classNode, methodNode);
            });
        });
    }

    private void firstPhase(MethodNode methodNode) {
        boolean modified;
        do {
            modified = false;
            for (AbstractInsnNode node : methodNode.instructions.toArray()) {
                if (node instanceof LookupSwitchInsnNode && isInteger(node.getPrevious())) {
                    modified = resolveLookup((LookupSwitchInsnNode) node, methodNode, node.getPrevious());
                } else if (node instanceof TableSwitchInsnNode && isInteger(node.getPrevious())) {
                    resolveTable((TableSwitchInsnNode) node, methodNode, node.getPrevious());
                    modified = true;
                }
            }
        } while (modified);
    }


    private void secondPhase(ClassNode classNode, MethodNode methodNode) {
        if (Arrays.stream(methodNode.instructions.toArray()).noneMatch(node -> node instanceof LookupSwitchInsnNode || node instanceof TableSwitchInsnNode))
            return;

        boolean modified;
        do {
            modified = false;
            Map<AbstractInsnNode, Frame<SourceValue>> frames = analyzeSource(classNode, methodNode);
            if (frames == null)
                break;

            for (AbstractInsnNode node : methodNode.instructions.toArray()) {
                AbstractInsnNode integer;

                if (node instanceof LookupSwitchInsnNode && (integer = getInteger(node, frames)) != null) {
                    modified = resolveLookup((LookupSwitchInsnNode) node, methodNode, integer);
                } else if (node instanceof TableSwitchInsnNode && (integer = getInteger(node, frames)) != null) {
                    resolveTable((TableSwitchInsnNode) node, methodNode, integer);
                    modified = true;
                }
            }
        } while (modified);
    }

    private boolean resolveLookup(LookupSwitchInsnNode node, MethodNode methodNode, AbstractInsnNode keyNode) {
        int key = getInteger(keyNode);
        LabelNode originalCodeBlockStart = node.keys.contains(key) ? node.labels.get(node.keys.indexOf(key)) : node.dflt;

        if (originalCodeBlockStart.getNext().getOpcode() == GOTO) {
            methodNode.instructions.remove(keyNode);
            methodNode.instructions.set(node, new JumpInsnNode(GOTO, ((JumpInsnNode) originalCodeBlockStart.getNext()).label));
            return true;
        }

        return false;
    }

    private void resolveTable(TableSwitchInsnNode node, MethodNode methodNode, AbstractInsnNode keyNode) {
        int key = getInteger(keyNode);
        LabelNode originalCodeBlockStart = key < node.min || key > node.max ? node.dflt : node.labels.get(key);

        methodNode.instructions.remove(keyNode);
        methodNode.instructions.set(node, new JumpInsnNode(GOTO, originalCodeBlockStart));
    }

    private AbstractInsnNode getInteger(AbstractInsnNode node, Map<AbstractInsnNode, Frame<SourceValue>> frames) {
        Frame<SourceValue> frame = frames.get(node);
        if (frame == null)
            return null;

        SourceValue value = frame.getStack(frame.getStackSize() - 1);
        if (value == null || value.insns == null || value.insns.isEmpty())
            return null;

        AbstractInsnNode stackInsn = value.insns.iterator().next();
        if (!isInteger(stackInsn))
            return null;

        return stackInsn;
    }
}
