package uwu.narumi.deobfuscator.transformer.impl.branchlock;

import org.objectweb.asm.tree.*;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.helper.ASMHelper;
import uwu.narumi.deobfuscator.transformer.Transformer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class BranchLockCompatibilityStringTransformer extends Transformer {
    private String stringFieldName = "";
    private int stringsDeobfuscated = 0;

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        deobfuscator.classes()
//                .filter(classNode -> !classNode.name.contains("apache") && !classNode.name.contains("codehaus"))
                .forEach(classNode -> {

                    if (!classNode.name.contains("Branchlock")) {

//                        classNode.methods.forEach(methodNode -> {
//                            if (methodNode.name.equals("<clinit>")) {
//                                String[] strings = getStrings(classNode);
//                            }
//                        });

                        //loop all classNodes util <clinit> in reached then pass that specific method in to getStrings and then run the rest

                        //String[] strings = getStrings(classNode, classNode.methods.stream().filter(methodNode -> methodNode.name.equals("<init>")).findFirst().get());

                        classNode.methods.stream().filter(methodNode -> methodNode.name.equals("<clinit>")).forEach(methodNode -> {

                            try {
                                String[] strings = getStrings(classNode);

                                deobfuscator.classes().stream()
                                        .flatMap(classNodee -> classNodee.methods.stream())
                                        .forEach(methodNodee -> {
                                            for (AbstractInsnNode node : methodNodee.instructions.toArray()) {
                                                if (node.getOpcode() == GETSTATIC
                                                        && ((FieldInsnNode) node).name.equals(stringFieldName)
                                                        && node.getNext().getNext().getOpcode() == AALOAD
                                                        && ((FieldInsnNode) node).desc.equals("[Ljava/lang/String;")
                                                        && classNode.name.equals(((FieldInsnNode) node).owner)) {

                                                    int deobfedStringId = getInteger(node.getNext());

                                                    String deobfedString = strings[deobfedStringId];
                                                    stringsDeobfuscated++;

                                                    //                                            System.out.println(Arrays.toString(strings));

                                                    methodNodee.instructions.remove(node.getNext());
                                                    methodNodee.instructions.remove(node.getNext());
                                                    methodNodee.instructions.set(node, new LdcInsnNode(deobfedString));

                                                }
                                            }
                                        });
                            } catch (Exception e) {
                                System.out.println("Error in class " + classNode.name);
                            }
                        });
                    }

                });

        System.out.println("Deobfuscated " + stringsDeobfuscated + " strings");
    }

    private String[] getStrings(ClassNode classNode) {
        // Get the <clinit> method
        MethodNode methodNode = classNode.methods.stream().filter(methodNode1 -> methodNode1.name.equals("<clinit>")).findFirst().orElse(null);

        // Get the FieldInsnNode with desc of [Ljava/lang/String;
        methodNode.instructions.forEach(instruction -> {
            if (instruction.getOpcode() == PUTSTATIC && ((FieldInsnNode) instruction).desc.equals("[Ljava/lang/String;")) {
                stringFieldName = ((FieldInsnNode) instruction).name;
            }
        });

        final String[] encryptedString = {null};
        final int[] xorShiftKey1 = {0};
        final int[] xorShiftKey2 = {0};
        final LookupSwitchInsnNode[] lookupSwitchInsnNode = {null};

        methodNode.instructions.forEach(instruction -> {
            if (instruction.getOpcode() == LDC) {
                if (instruction.getNext() instanceof MethodInsnNode && ((MethodInsnNode)instruction.getNext()).name.equals("toCharArray")) {
                    encryptedString[0] = ((LdcInsnNode) instruction).cst.toString();
                }
            } else if (instruction instanceof IntInsnNode &&
                    instruction.getPrevious().getOpcode() == CALOAD &&
                    instruction.getNext().getOpcode() == IXOR &&
                    instruction.getNext().getNext().getNext().getOpcode() == IXOR &&
                    instruction.getNext().getNext().getNext().getNext() instanceof InsnNode &&
                    instruction.getNext().getNext() instanceof VarInsnNode) {
                xorShiftKey1[0] = ((IntInsnNode) instruction).operand;
            } else if (instruction instanceof IntInsnNode &&
                    instruction.getPrevious().getOpcode() == CALOAD &&
                    instruction.getNext().getOpcode() == IXOR &&
                    instruction.getNext().getNext() instanceof LookupSwitchInsnNode) {
                xorShiftKey2[0] = ((IntInsnNode) instruction).operand;
                lookupSwitchInsnNode[0] = (LookupSwitchInsnNode) instruction.getNext().getNext();
            }
        });


//        System.out.println("Data for " + classNode.name);
//        System.out.println("Encrypted String: " + encryptedString[0]);
//        System.out.println("XorShiftKey1: " + xorShiftKey1[0]);
//        System.out.println("XorShiftKey2: " + xorShiftKey2[0]);
//        System.out.println("Keys: " + keys.get());

        List<Integer> keys = lookupSwitchInsnNode[0].keys;
        LabelNode[] labels = lookupSwitchInsnNode[0].labels.toArray(new LabelNode[0]);
        // Get all the labels in the method
        List<LabelNode> labelNodes = new ArrayList<>();
        methodNode.instructions.forEach(instruction -> {
            if (instruction instanceof LabelNode) {
                labelNodes.add((LabelNode) instruction);
            }
        });

        HashMap<Integer, Integer> switchCases = new HashMap<>();

        // associate the lookupSwitchInsnNode labels with the labels in the method
        for (int i = 0; i < keys.size(); i++) {
            for (int j = 0; j < labelNodes.size(); j++) {
                if (labels[i] == labelNodes.get(j)) {

                    AbstractInsnNode abstractSearchNode = labelNodes.get(j);

                    while (!(abstractSearchNode instanceof VarInsnNode)) {
                        if (abstractSearchNode instanceof LabelNode) {
                            AbstractInsnNode checkNode = abstractSearchNode.getNext();

                            if (checkNode instanceof VarInsnNode) abstractSearchNode = checkNode;
                            if (checkNode instanceof JumpInsnNode) abstractSearchNode = ((JumpInsnNode) checkNode).label;
                        }
                    }

                    VarInsnNode varInsnNode = (VarInsnNode) abstractSearchNode;
                    IntInsnNode xorShiftValue = (IntInsnNode) varInsnNode.getNext();

//                    System.out.println(xorShiftValue.operand + " Case: " + keys.get(i) + " Class: " + classNode.name);

                    switchCases.put(keys.get(i), xorShiftValue.operand);
                }
            }
        }

        try {
            return process(classNode.name, classNode.name.replace("/", ".").toCharArray(), xorShiftKey1[0], xorShiftKey2[0], encryptedString[0].toCharArray(), switchCases);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String[] process(String className, char[] classNameChars, int xorShiftKey1, int xorShiftKey2, char[] encryptedStringArray, HashMap<Integer, Integer> switchCases) {
        // Start Key Variables
//        encryptedStringArray = "๴ໟªÇໟÛÃ໚Ç¬'Þ­".toCharArray();
//        classNameChars = "me.john.utils.Main".toCharArray();
        // End Key Variables

        int var1 = 1;
        int i2 = 0;
        int hashedMethodName = "<clinit>".hashCode() & 65535;

        String[] strArr = new String[classNameChars[0] ^ hashedMethodName];

        do {
            int i6 = 0;
            int i3 = var1;
            var1++;
            int i4 = (encryptedStringArray[i3] ^ xorShiftKey1) ^ hashedMethodName;
            int i5 = i4;
            char[] rebuiltString = new char[i4];

            while (i5 > 0) {
                char c = encryptedStringArray[var1];

                int key = switchCases.get(classNameChars[var1 % classNameChars.length] ^ xorShiftKey2);
//                System.out.println("Key: " + key);
                c ^= key;

                rebuiltString[i6] = c;

                try {
                    i6++;
                    var1++;
                    i5--;
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }

            int i7 = i2;
            i2++;
            strArr[i7] = new String(rebuiltString).intern();
        } while (var1 < encryptedStringArray.length);

        System.out.println(Arrays.toString(strArr));

        // Write to a file called string_lookup.txt the method, class, and the order in the array
        try {
            // Delete the file if it exists
            BufferedWriter writer = new BufferedWriter(new FileWriter("string_lookup.txt", true));
            // for each string in the array as long as its not null
            for (int i = 0; i < strArr.length; i++) {
                if (strArr[i] != null) {
                    writer.write(strArr[i] + " Index: " + i + " Class: " + className);
                    writer.newLine();
                }
            }
            writer.newLine();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return strArr;
    }
}
