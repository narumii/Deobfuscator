package uwu.narumi.deobfuscator.core.other.impl.hp888;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.context.Context;
import uwu.narumi.deobfuscator.api.helper.ClassHelper;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public class HP888PackerTransformer extends Transformer {

    private final String endOfEncryptedFile;

    public HP888PackerTransformer(String endOfEncryptedFile) {
        this.endOfEncryptedFile = endOfEncryptedFile;
    }

    @Override
    protected void transform(ClassWrapper scope, Context context) throws Exception {
        Set<String> toRemove = new HashSet<>();
        HashMap<String, ClassWrapper> newClasses = new HashMap<>();
        Cipher cipher = Cipher.getInstance("AES");
        AtomicReference<String> key = new AtomicReference<>();
        /* Firstly you must use HP888StringTransformer, so key would be decrypted,
            and it only searches in loader classes so don't tell me its bad searching. */
        context.classes().stream().map(ClassWrapper::classNode).forEach(classNode -> classNode.methods.forEach(methodNode -> methodNode.instructions.forEach(abstractInsnNode -> {
            if (abstractInsnNode.isString() && abstractInsnNode.asString().endsWith("==")) {
                key.set(abstractInsnNode.asString());
            }
        })));
        if (key.get().isEmpty()) {
            LOGGER.error("Key not found");
            return;
        }
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(Base64.getDecoder().decode(key.get()), "AES"));
        context.getFiles().forEach((file, bytes) -> {
            if (file.endsWith(endOfEncryptedFile)) {
                String cleanFileName = file.replace(endOfEncryptedFile, "").replace(".", "/");
                toRemove.add(file);
                try {
                    newClasses
                            .put
                                    (cleanFileName, ClassHelper.loadClass(cleanFileName,
                                            cipher.doFinal(bytes),
                                            ClassReader.SKIP_FRAMES,
                                            ClassWriter.COMPUTE_MAXS,
                                            true));
                    markChange();
                } catch (Exception e) {
                    LOGGER.error(e);
                }
            }
        });
        toRemove.forEach(context.getFiles()::remove);
        context.getClasses().putAll(newClasses);
    }
}
