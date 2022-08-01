package uwu.narumi.deobfuscator.transformer.impl.universal.other;

import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.transformer.Transformer;

/**
 * Changes class major version
 * 
 * @see <a href="https://javaalmanac.io/bytecode/versions/">Cheatsheet</a>
 * @author HeyaGlitz#6299
 */
public class ClassFileVersionChangerTransformer extends Transformer {
	private final int version;

	public ClassFileVersionChangerTransformer(int version) {
		this.version = version;
	}

	@Override
	public void transform(Deobfuscator deobfuscator) throws Exception {
		if (this.version < 45 || this.version > 64)
			LOGGER.warn("Specified class file version is invalid! Output may not not be functional.");

		deobfuscator.classes().forEach(classNode -> {
			classNode.version = this.version;
		});
	}
}
