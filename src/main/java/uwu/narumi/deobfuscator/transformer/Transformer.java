package uwu.narumi.deobfuscator.transformer;

import org.objectweb.asm.Opcodes;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.exception.DeobfuscationException;

public interface Transformer extends Opcodes {

  void transform(Deobfuscator deobfuscator) throws DeobfuscationException;

  int weight();

  String name();
}
