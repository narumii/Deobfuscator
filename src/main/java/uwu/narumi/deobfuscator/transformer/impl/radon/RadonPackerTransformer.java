package uwu.narumi.deobfuscator.transformer.impl.radon;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.helper.ClassHelper;
import uwu.narumi.deobfuscator.transformer.Transformer;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;

public class RadonPackerTransformer extends Transformer {

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        ClassNode loaderClass = deobfuscator.classes().stream()
                .filter(classNode -> classNode.superName.equals("java/lang/ClassLoader"))
                .filter(classNode -> classNode.methods.size() == 5)
                .findFirst().orElseThrow();

        MethodNode methodNode = findMethod(loaderClass, node -> node.name.equals("<init>")).orElseThrow();
        String resource = Arrays.stream(methodNode.instructions.toArray())
                .filter(node -> node instanceof MethodInsnNode)
                .filter(node -> node.getOpcode() == INVOKEVIRTUAL)
                .filter(node -> isString(node.getPrevious()))
                .map(MethodInsnNode.class::cast)
                .filter(node -> node.owner.equals("java/lang/Class"))
                .filter(node -> node.name.equals("getResourceAsStream"))
                .filter(node -> node.desc.equals("(Ljava/lang/String;)Ljava/io/InputStream;"))
                .map(node -> getString(node.getPrevious()).substring(1))
                .findFirst().orElseThrow();

        GZIPInputStream gZIPInputStream = new GZIPInputStream(new ByteArrayInputStream(deobfuscator.getFiles().get(resource)));
        DataInputStream dataInputStream = new DataInputStream(gZIPInputStream);

        int classes = dataInputStream.readInt();
        for (int i = 0; i < classes; ++i) {
            String name = dataInputStream.readUTF();
            byte[] bytes = new byte[dataInputStream.readInt()];
            for (int j = 0; j < bytes.length; ++j) {
                bytes[j] = dataInputStream.readByte();
            }

            if (ClassHelper.isClass(name, bytes)) {
                name = name.replace(".class", "");

                deobfuscator.getClasses().put(name, ClassHelper.loadClass(bytes, deobfuscator.getClassReaderFlags()));
                deobfuscator.getOriginalClasses().put(name, ClassHelper.loadClass(bytes, deobfuscator.getClassReaderFlags()));
            } else {
                deobfuscator.getFiles().put((deobfuscator.getFiles().containsKey(name) ? "packed-" : "") + name, bytes);
            }
        }
    }
}
