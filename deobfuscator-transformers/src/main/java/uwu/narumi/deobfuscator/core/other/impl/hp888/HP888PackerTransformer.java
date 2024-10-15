package uwu.narumi.deobfuscator.core.other.impl.hp888;

import org.objectweb.asm.ClassReader;
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.helper.ClassHelper;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * HP888 obfuscator has encrypted classes files (usually ending with the suffix ".mc" {@link #encryptedClassFilesSuffix})
 * that are decrypted and loaded when the jar starts.
 */
public class HP888PackerTransformer extends Transformer {

  /**
   * Suffix of encrypted class files
   */
  private final String encryptedClassFilesSuffix;

  public HP888PackerTransformer(String encryptedClassFilesSuffix) {
    this.encryptedClassFilesSuffix = encryptedClassFilesSuffix;
  }

  @Override
  protected void transform() throws Exception {
    Set<String> filesToRemove = new HashSet<>();
    HashMap<String, ClassWrapper> newClasses = new HashMap<>();
    AtomicReference<String> key = new AtomicReference<>();

    /* Firstly you must use HP888StringTransformer, so key would be decrypted,
        and it only searches in loader classes so don't tell me its bad searching. */
    scopedClasses().stream().map(ClassWrapper::classNode).forEach(classNode -> classNode.methods.forEach(methodNode -> methodNode.instructions.forEach(abstractInsnNode -> {
      if (abstractInsnNode.isString() && abstractInsnNode.asString().endsWith("==")) {
        // Find base64 key
        key.set(abstractInsnNode.asString());
      }
    })));

    if (key.get().isEmpty()) {
      LOGGER.error("Key not found");
      return;
    }

    // Decrypt encrypted classes
    Cipher cipher = Cipher.getInstance("AES");
    cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(Base64.getDecoder().decode(key.get()), "AES"));
    context().getFilesMap().forEach((file, bytes) -> {
      if (file.endsWith(encryptedClassFilesSuffix)) {
        filesToRemove.add(file);

        String path = file.replace(encryptedClassFilesSuffix, ".class").replace(".", "/");
        try {
          // Decrypt!
          byte[] decrypted = cipher.doFinal(bytes);

          // Load and put class
          context().addCompiledClass(path, decrypted);

          markChange();
        } catch (Exception e) {
          throw new RuntimeException("Failed to decrypt class: " + path, e);
        }
      }
    });

    // Cleanup
    filesToRemove.forEach(context().getFilesMap()::remove);
  }
}
