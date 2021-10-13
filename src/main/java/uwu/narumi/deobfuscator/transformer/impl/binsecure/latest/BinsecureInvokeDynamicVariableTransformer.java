package uwu.narumi.deobfuscator.transformer.impl.binsecure.latest;

import org.objectweb.asm.tree.*;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.exception.TransformerException;
import uwu.narumi.deobfuscator.transformer.Transformer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class BinsecureInvokeDynamicVariableTransformer extends Transformer {


    /*
        x = index/position
        y = value

        put(x, y);
        get(x)
     */

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        deobfuscator.classes().forEach(classNode -> classNode.methods.forEach(methodNode -> {
            Map<Long, Integer> vars = new HashMap<>();
            methodNode.maxLocals += 3;

            Arrays.stream(methodNode.instructions.toArray())
                    .filter(node -> node instanceof MethodInsnNode)
                    .map(MethodInsnNode.class::cast)
                    .filter(node -> node.owner.equals("sun/misc/Unsafe"))
                    .filter(node -> node.name.startsWith("put") || node.name.startsWith("get"))
                    .forEach(node -> {
                        boolean doubleSize = node.name.endsWith("Long") || node.name.endsWith("Double");
                        boolean store = node.name.startsWith("put");

                        long key = 0;
                        if (node.getPrevious() instanceof VarInsnNode && isLong(node.getPrevious().getPrevious().getPrevious())) {
                            key = getLong(node.getPrevious().getPrevious().getPrevious());
                            if (!vars.containsKey(key))
                                vars.put(key, methodNode.maxLocals + (doubleSize ? 2 : 1));

                            methodNode.instructions.remove(node.getPrevious().getPrevious().getPrevious().getPrevious().getPrevious());
                            methodNode.instructions.remove(node.getPrevious().getPrevious().getPrevious().getPrevious());
                            methodNode.instructions.remove(node.getPrevious().getPrevious().getPrevious());
                            methodNode.instructions.remove(node.getPrevious().getPrevious());

                            methodNode.instructions.set(node, new VarInsnNode(getOpcode(node, store), vars.get(key)));
                        } else if (isLong(node.getPrevious().getPrevious().getPrevious().getPrevious()) || isLong(node.getPrevious().getPrevious())) {
                            if (node.name.startsWith("put")) {
                                key += getLong(node.getPrevious().getPrevious().getPrevious().getPrevious());
                            } else {
                                key += getLong(node.getPrevious().getPrevious());
                            }

                            if (!vars.containsKey(key))
                                vars.put(key, methodNode.maxLocals + (doubleSize ? 2 : 1));

                            if (store) {
                                if (doubleSize)
                                    methodNode.instructions.remove(node.getPrevious().getPrevious().getPrevious().getPrevious().getPrevious().getPrevious().getPrevious().getPrevious());

                                methodNode.instructions.remove(node.getPrevious().getPrevious().getPrevious().getPrevious().getPrevious().getPrevious().getPrevious());
                                methodNode.instructions.remove(node.getPrevious().getPrevious().getPrevious().getPrevious().getPrevious().getPrevious());
                                methodNode.instructions.remove(node.getPrevious().getPrevious().getPrevious().getPrevious().getPrevious());
                            }

                            methodNode.instructions.remove(node.getPrevious().getPrevious().getPrevious().getPrevious());
                            methodNode.instructions.remove(node.getPrevious().getPrevious().getPrevious());
                            methodNode.instructions.remove(node.getPrevious().getPrevious());
                            methodNode.instructions.remove(node.getPrevious());

                            methodNode.instructions.set(node, new VarInsnNode(getOpcode(node, store), vars.get(key)));
                        }

                        methodNode.maxLocals += (doubleSize ? 2 : 1);
                    });

            clean(methodNode);
        }));
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

    private int getOpcode(MethodInsnNode node, boolean store) {
        int opcode;
        switch (node.name.substring(3)) {
            case "Int": {
                opcode = store ? ISTORE : ILOAD;
                break;
            }
            case "Long": {
                opcode = store ? LSTORE : LLOAD;
                break;
            }
            case "Float": {
                opcode = store ? FSTORE : FLOAD;
                break;
            }
            case "Double": {
                opcode = store ? DSTORE : DLOAD;
                break;
            }
            default:
                throw new TransformerException();
        }

        return opcode;
    }
}
