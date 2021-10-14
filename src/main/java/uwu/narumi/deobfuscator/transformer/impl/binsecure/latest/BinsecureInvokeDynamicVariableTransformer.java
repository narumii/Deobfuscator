package uwu.narumi.deobfuscator.transformer.impl.binsecure.latest;

import org.objectweb.asm.tree.*;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.exception.TransformerException;
import uwu.narumi.deobfuscator.transformer.Transformer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/*
    Hard coded shit
 */
public class BinsecureInvokeDynamicVariableTransformer extends Transformer {

    //TODO: Move it from here lol
    private static final Map<String, Map<Long, Integer>> offsets = new HashMap<>();

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        deobfuscator.classes().forEach(classNode -> classNode.methods.forEach(methodNode -> {
            resolvePut(classNode, methodNode);
            resolveIinc(classNode, methodNode);
            resolveGet(classNode, methodNode);

            clean(methodNode);
        }));
    }

    private void resolvePut(ClassNode classNode, MethodNode methodNode) {
        if (!offsets.containsKey(classNode.name + methodNode.name + methodNode.desc))
            offsets.put(classNode.name + methodNode.name + methodNode.desc, new HashMap<>());

        Map<Long, Integer> offsets = BinsecureInvokeDynamicVariableTransformer.offsets.get(classNode.name + methodNode.name + methodNode.desc);
        Arrays.stream(methodNode.instructions.toArray())
                .filter(node -> node instanceof MethodInsnNode)
                .map(MethodInsnNode.class::cast)
                .filter(node -> node.owner.equals("sun/misc/Unsafe"))
                .filter(node -> node.name.equals("putLong") || node.name.equals("putDouble"))
                .filter(node -> node.getPrevious().getOpcode() == POP2)
                .forEach(node -> {
                    long offset = getLong(node.getPrevious().getPrevious().getPrevious().getPrevious());
                    if (!offsets.containsKey(offset))
                        offsets.put(offset, methodNode.maxLocals + 2);

                    methodNode.instructions.remove(node.getPrevious().getPrevious().getPrevious().getPrevious().getPrevious().getPrevious().getPrevious().getPrevious());
                    methodNode.instructions.remove(node.getPrevious().getPrevious().getPrevious().getPrevious().getPrevious().getPrevious().getPrevious());
                    methodNode.instructions.remove(node.getPrevious().getPrevious().getPrevious().getPrevious().getPrevious().getPrevious());
                    methodNode.instructions.remove(node.getPrevious().getPrevious().getPrevious().getPrevious().getPrevious());
                    methodNode.instructions.remove(node.getPrevious().getPrevious().getPrevious().getPrevious());
                    methodNode.instructions.remove(node.getPrevious().getPrevious().getPrevious());
                    methodNode.instructions.remove(node.getPrevious().getPrevious());
                    methodNode.instructions.remove(node.getPrevious());

                    methodNode.instructions.set(node, new VarInsnNode(getOpcode(node), offsets.get(offset)));
                    methodNode.maxLocals += 2;
                });

        Arrays.stream(methodNode.instructions.toArray())
                .filter(node -> node instanceof MethodInsnNode)
                .map(MethodInsnNode.class::cast)
                .filter(node -> node.owner.equals("sun/misc/Unsafe"))
                .filter(node -> node.name.equals("putInt") || node.name.equals("putFloat"))
                .filter(node -> node.getPrevious().getOpcode() == POP2)
                .forEach(node -> {
                    long offset = getLong(node.getPrevious().getPrevious().getPrevious().getPrevious());
                    if (!offsets.containsKey(offset))
                        offsets.put(offset, ++methodNode.maxLocals);

                    methodNode.instructions.remove(node.getPrevious().getPrevious().getPrevious().getPrevious().getPrevious().getPrevious().getPrevious());
                    methodNode.instructions.remove(node.getPrevious().getPrevious().getPrevious().getPrevious().getPrevious().getPrevious());
                    methodNode.instructions.remove(node.getPrevious().getPrevious().getPrevious().getPrevious().getPrevious());
                    methodNode.instructions.remove(node.getPrevious().getPrevious().getPrevious().getPrevious());
                    methodNode.instructions.remove(node.getPrevious().getPrevious().getPrevious());
                    methodNode.instructions.remove(node.getPrevious().getPrevious());
                    methodNode.instructions.remove(node.getPrevious());

                    methodNode.instructions.set(node, new VarInsnNode(getOpcode(node), offsets.get(offset)));
                });

        Arrays.stream(methodNode.instructions.toArray())
                .filter(node -> node instanceof MethodInsnNode)
                .map(MethodInsnNode.class::cast)
                .filter(node -> node.owner.equals("sun/misc/Unsafe"))
                .filter(node -> node.name.startsWith("put"))
                .filter(node -> node.getPrevious() instanceof VarInsnNode)
                .forEach(node -> {
                    long offset = getLong(node.getPrevious().getPrevious().getPrevious());
                    if (!offsets.containsKey(offset))
                        offsets.put(offset, ++methodNode.maxLocals);

                    methodNode.instructions.remove(node.getPrevious().getPrevious().getPrevious().getPrevious().getPrevious());
                    methodNode.instructions.remove(node.getPrevious().getPrevious().getPrevious().getPrevious());
                    methodNode.instructions.remove(node.getPrevious().getPrevious().getPrevious());
                    methodNode.instructions.remove(node.getPrevious().getPrevious());

                    methodNode.instructions.set(node, new VarInsnNode(getOpcode(node), offsets.get(offset)));
                });
    }

    private void resolveGet(ClassNode classNode, MethodNode methodNode) {
        if (!offsets.containsKey(classNode.name + methodNode.name + methodNode.desc))
            offsets.put(classNode.name + methodNode.name + methodNode.desc, new HashMap<>());

        Map<Long, Integer> offsets = BinsecureInvokeDynamicVariableTransformer.offsets.get(classNode.name + methodNode.name + methodNode.desc);
        Arrays.stream(methodNode.instructions.toArray())
                .filter(node -> node instanceof MethodInsnNode)
                .map(MethodInsnNode.class::cast)
                .filter(node -> node.owner.equals("sun/misc/Unsafe"))
                .filter(node -> node.name.startsWith("get"))
                .filter(node -> node.getPrevious().getOpcode() == LADD)
                .forEach(node -> {
                    long offset = getLong(node.getPrevious().getPrevious());
                    if (!offsets.containsKey(offset)) {
                        offsets.put(offset, ++methodNode.maxLocals);
                        System.out.println("# Index not found that shouldn't happened");
                    }

                    methodNode.instructions.remove(node.getPrevious().getPrevious().getPrevious().getPrevious());
                    methodNode.instructions.remove(node.getPrevious().getPrevious().getPrevious());
                    methodNode.instructions.remove(node.getPrevious().getPrevious());
                    methodNode.instructions.remove(node.getPrevious());

                    methodNode.instructions.set(node, new VarInsnNode(getOpcode(node), offsets.get(offset)));
                });
    }

    private void resolveIinc(ClassNode classNode, MethodNode methodNode) {
        if (!offsets.containsKey(classNode.name + methodNode.name + methodNode.desc))
            offsets.put(classNode.name + methodNode.name + methodNode.desc, new HashMap<>());

        Map<Long, Integer> offsets = BinsecureInvokeDynamicVariableTransformer.offsets.get(classNode.name + methodNode.name + methodNode.desc);
        Arrays.stream(methodNode.instructions.toArray())
                .filter(node -> node instanceof MethodInsnNode)
                .map(MethodInsnNode.class::cast)
                .filter(node -> node.owner.equals("sun/misc/Unsafe"))
                .filter(node -> node.name.equals("getInt"))
                .filter(node -> node.getNext().getNext().getOpcode() == IADD)
                .filter(node -> node.getNext().getNext().getNext() instanceof MethodInsnNode)
                .filter(node -> ((MethodInsnNode) node.getNext().getNext().getNext()).owner.equals("sun/misc/Unsafe"))
                .filter(node -> ((MethodInsnNode) node.getNext().getNext().getNext()).name.equals("putInt"))
                .forEach(node -> {
                    long offset = getLong(node.getPrevious().getPrevious().getPrevious().getPrevious().getPrevious().getPrevious());
                    if (!offsets.containsKey(offset)) {
                        offsets.put(offset, ++methodNode.maxLocals);
                        System.out.println("# Index not found that shouldn't happened");
                    }

                    MethodInsnNode putInt = (MethodInsnNode) node.getNext().getNext().getNext();

                    methodNode.instructions.remove(node.getPrevious().getPrevious().getPrevious().getPrevious().getPrevious().getPrevious().getPrevious().getPrevious());
                    methodNode.instructions.remove(node.getPrevious().getPrevious().getPrevious().getPrevious().getPrevious().getPrevious().getPrevious());
                    methodNode.instructions.remove(node.getPrevious().getPrevious().getPrevious().getPrevious().getPrevious().getPrevious());
                    methodNode.instructions.remove(node.getPrevious().getPrevious().getPrevious().getPrevious().getPrevious());
                    methodNode.instructions.remove(node.getPrevious().getPrevious().getPrevious().getPrevious());
                    methodNode.instructions.remove(node.getPrevious().getPrevious().getPrevious());
                    methodNode.instructions.remove(node.getPrevious().getPrevious());
                    methodNode.instructions.remove(node.getPrevious());

                    methodNode.instructions.set(node, new VarInsnNode(ILOAD, offsets.get(offset)));
                    methodNode.instructions.set(putInt, new VarInsnNode(ISTORE, offsets.get(offset)));
                });
    }

    private void clean(MethodNode methodNode) {
        try {
            Arrays.stream(methodNode.instructions.toArray())
                    .filter(node -> node instanceof MethodInsnNode)
                    .map(MethodInsnNode.class::cast)
                    .filter(node -> node.owner.equals("sun/misc/Unsafe"))
                    .filter(node -> node.name.equals("freeMemory"))
                    .filter(node -> node.desc.equals("(J)V"))
                    .filter(node -> node.getPrevious().getOpcode() == LLOAD)
                    .forEach(node -> {
                        methodNode.instructions.remove(node.getPrevious());
                        methodNode.instructions.remove(node);
                    });

            for (AbstractInsnNode node : methodNode.instructions.toArray()) {
                if (node instanceof MethodInsnNode
                        && ((MethodInsnNode) node).owner.equals("sun/misc/Unsafe")
                        && ((MethodInsnNode) node).name.equals("allocateMemory")
                        && ((MethodInsnNode) node).desc.equals("(J)J")) {

                    methodNode.instructions.remove(node.getPrevious().getPrevious().getPrevious().getPrevious());
                    methodNode.instructions.set(node.getPrevious().getPrevious().getPrevious(), new InsnNode(ACONST_NULL));

                    getInstructionsBetween(
                            node.getPrevious(),
                            node.getNext().getNext().getNext().getNext().getNext().getNext().getNext().getNext().getNext().getNext(),
                            true, true).forEach(methodNode.instructions::remove);
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int getOpcode(MethodInsnNode node) {
        boolean store = node.name.startsWith("put");
        switch (node.name.substring(3)) {
            case "Int": {
                return store ? ISTORE : ILOAD;
            }
            case "Long": {
                return store ? LSTORE : LLOAD;
            }
            case "Float": {
                return store ? FSTORE : FLOAD;
            }
            case "Double": {
                return store ? DSTORE : DLOAD;
            }
            default:
                throw new TransformerException();
        }
    }
}
