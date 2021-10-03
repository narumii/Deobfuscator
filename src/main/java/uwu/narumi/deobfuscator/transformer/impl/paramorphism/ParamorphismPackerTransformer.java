package uwu.narumi.deobfuscator.transformer.impl.paramorphism;

import org.objectweb.asm.tree.ClassNode;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.helper.ClassHelper;
import uwu.narumi.deobfuscator.transformer.Transformer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.zip.GZIPInputStream;

public class ParamorphismPackerTransformer extends Transformer {

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        deobfuscator.classes().removeIf(classNode -> classNode.name.endsWith("PackedClassLoader") && classNode.superName.equals("java/lang/ClassLoader"));
        deobfuscator.getFiles().forEach((name, bytes) -> {
            try {
                ByteArrayInputStream var4 = new ByteArrayInputStream(bytes);
                ByteArrayOutputStream var5 = new ByteArrayOutputStream(Math.max(8192, var4.available()));
                byte[] var6 = new byte[8192];

                int var7;
                for (var7 = var4.read(var6); var7 >= 0; var7 = var4.read(var6)) {
                    var5.write(var6, 0, var7);
                }

                var6 = var5.toByteArray();

                for (var7 = 7; var7 >= 0; --var7) {
                    var6[7 - var7] = (byte) ((int) (2272919233031569408L >> 8 * var7 & 255L));
                }

                GZIPInputStream var8 = new GZIPInputStream(new ByteArrayInputStream(var6.clone()));
                ByteArrayOutputStream var9 = new ByteArrayOutputStream(Math.max(8192, var8.available()));
                byte[] var10 = new byte[8192];

                for (int var11 = var8.read(var10); var11 >= 0; var11 = var8.read(var10)) {
                    var9.write(var10, 0, var11);
                }

                var10 = var9.toByteArray();
                if (ClassHelper.isClass("ignored.class", var10)) {
                    ClassNode classNode = ClassHelper.loadClass(var10, deobfuscator.getClassReaderFlags());
                    deobfuscator.getClasses().put(classNode.name, classNode);
                    deobfuscator.getOriginalClasses().put(classNode.name, classNode);
                    deobfuscator.getFiles().remove(name);
                }
            } catch (Exception e) {
            }
        });
    }
}
