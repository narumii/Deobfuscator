package uwu.narumi.deobfuscator.transformer.impl.qprotect.b31;

import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.transformer.Transformer;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

/*
    This transformer works only on version: b31
 */
public class qProtectStringTransformer extends Transformer {

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        deobfuscator.classes().forEach(classNode -> {
            AtomicReference<MethodInsnNode> methodInsn = new AtomicReference<>();

            classNode.methods.stream()
                    .forEach(methodNode -> Arrays.stream(methodNode.instructions.toArray())
                            .filter(node -> node instanceof MethodInsnNode)
                            .filter(node -> node.getOpcode() == INVOKESTATIC)
                            .map(MethodInsnNode.class::cast)
                            .filter(node -> node.desc.equals("(Ljava/lang/String;Ljava/lang/String;IIII)Ljava/lang/String;"))

                            .filter(node -> isInteger(node.getPrevious()))
                            .filter(node -> isInteger(node.getPrevious().getPrevious()))
                            .filter(node -> isInteger(node.getPrevious().getPrevious().getPrevious()))
                            .filter(node -> isInteger(node.getPrevious().getPrevious().getPrevious().getPrevious()))
                            .filter(node -> isString(node.getPrevious().getPrevious().getPrevious().getPrevious().getPrevious()))
                            .filter(node -> isString(node.getPrevious().getPrevious().getPrevious().getPrevious().getPrevious().getPrevious()))

                            .forEach(node -> {
                                String first = getString(node.getPrevious().getPrevious().getPrevious().getPrevious().getPrevious().getPrevious());
                                String second = getString(node.getPrevious().getPrevious().getPrevious().getPrevious().getPrevious());
                                int third = getInteger(node.getPrevious().getPrevious().getPrevious().getPrevious());
                                int fourth = getInteger(node.getPrevious().getPrevious().getPrevious());
                                int fifth = getInteger(node.getPrevious().getPrevious());
                                int sixth = getInteger(node.getPrevious());

                                String decode = decrypt(first, second, third, fourth, fifth, sixth);

                                if (methodInsn.get() == null)
                                    methodInsn.set(node);

                                methodNode.instructions.remove(node.getPrevious().getPrevious().getPrevious().getPrevious().getPrevious().getPrevious());
                                methodNode.instructions.remove(node.getPrevious().getPrevious().getPrevious().getPrevious().getPrevious());
                                methodNode.instructions.remove(node.getPrevious().getPrevious().getPrevious().getPrevious());
                                methodNode.instructions.remove(node.getPrevious().getPrevious().getPrevious());
                                methodNode.instructions.remove(node.getPrevious().getPrevious());
                                methodNode.instructions.remove(node.getPrevious());

                                methodNode.instructions.set(node, new LdcInsnNode(decode));
                            }));

            if (methodInsn.get() != null)
                deobfuscator.getClasses().get(methodInsn.get().owner).methods.removeIf(methodNode -> methodNode.name.equals(methodInsn.get().name) && methodNode.desc.equals(methodInsn.get().desc));
        });
    }


    private String decrypt(String var0, String var1, int var2, int var3, int var4, int var5) {
        char[] var6 = var1.toCharArray();
        char[] var7 = new char[var6.length];
        char[] var8 = new char[]{'\u4832', '\u2385', '\u2386', '\u9813', '\u9125', '\u4582', '\u0913', '\u3422', '\u0853', '\u0724'};
        char[] var9 = new char[]{'\u4820', '\u8403', '\u8753', '\u3802', '\u3840', '\u3894', '\u8739', '\u1038', '\u8304', '\u3333'};

        for (int var10 = 0; var10 < var6.length; ++var10) {
            var7[var10] = (char) (var6[var10] ^ var8[var10 % var8.length]);
        }

        char[] var17 = new char[var7.length];

        for (int var11 = 0; var11 < var6.length; ++var11) {
            var17[var11] = (char) (var7[var11] ^ var9[var11 % var9.length]);
        }

        String var18 = new String(var17);
        int var12 = var18.hashCode();
        int var13 = var3 - var5 - var2;
        char[] var14 = var0.toCharArray();
        char[] var15 = new char[var14.length];

        for (int var16 = 0; var16 < var15.length; ++var16) {
            switch (var16 % 2) {
                case 0:
                    var15[var16] = (char) (var13 ^ var12 ^ var14[var16]);
                    break;
                case 1:
                    var15[var16] = (char) (var5 ^ var13 ^ var14[var16]);
            }
        }

        return new String(var15);
    }


    /*private String encrypt(String var0, String var1, int var2, int var3, int var4, int var5) {
        char[] var6 = (char[])var1.toCharArray();
        char[] var7 = new char[var6.length];
        char[] var8 = new char[]{'\u4832', '\u2385', '\u2386', '\u9813', '\u9125', '\u4582', '\u0913', '\u3422', '\u0853', '\u0724'};
        char[] var9 = new char[]{'\u4820', '\u8403', '\u8753', '\u3802', '\u3840', '\u3894', '\u8739', '\u1038', '\u8304', '\u3333'};

        for(int var10 = 0; var10 < var6.length; ++var10) {
            var7[var10] = (char)(var6[var10] ^ var8[var10 % var8.length]);
        }

        char[] var17 = new char[var7.length];

        for(int var11 = 0; var11 < var6.length; ++var11) {
            var17[var11] = (char)(var7[var11] ^ var9[var11 % var9.length]);
        }

        String var18 = new String(var17);
        int var12 = var18.hashCode();
        int var13 = var3 - var5 - var2;
        char[] var14 = (char[])var0.toCharArray();
        char[] var15 = new char[var14.length];

        for(int var16 = 0; var16 < var15.length; ++var16) {
            switch(var16 % 2) {
                case 0:
                    var15[var16] = (char)(var13 ^ var12 ^ var14[var16]);
                    break;
                case 1:
                    var15[var16] = (char)(var5 ^ var13 ^ var14[var16]);
            }
        }

        return new String(var15);
    }*/
}
