package uwu.narumi.deobfuscator.transformer.impl.bozar;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;

import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.matcher.InstructionMatcher;
import uwu.narumi.deobfuscator.matcher.IMatch;
import uwu.narumi.deobfuscator.matcher.impl.ClassMatch;
import uwu.narumi.deobfuscator.matcher.impl.IntegerMatch;
import uwu.narumi.deobfuscator.matcher.impl.LongMatch;
import uwu.narumi.deobfuscator.matcher.impl.NumberMatch;
import uwu.narumi.deobfuscator.matcher.impl.OpcodeMatch;
import uwu.narumi.deobfuscator.transformer.Transformer;

public class BozarHeavyConstantFlowTransformer extends Transformer {
	InstructionMatcher beforeMatch = new InstructionMatcher(new IMatch[] {
		new LongMatch(),
		new LongMatch(),
		new OpcodeMatch(LCMP),
		new OpcodeMatch(ISTORE),
		new OpcodeMatch(ILOAD),
		new OpcodeMatch(IFNE),
		new ClassMatch(LabelNode.class),
		new NumberMatch(),
		new OpcodeMatch(GOTO),
		new ClassMatch(LabelNode.class),
	});
	
	InstructionMatcher afterMatch = new InstructionMatcher(new IMatch[] {
		new ClassMatch(LabelNode.class),
		new OpcodeMatch(ILOAD),
		new IntegerMatch(),
		new OpcodeMatch(IADD),
		new IntegerMatch(),
		new OpcodeMatch(IF_ICMPNE),
		new ClassMatch(InsnNode.class),
		new OpcodeMatch(GOTO),
		new ClassMatch(LabelNode.class)
	});

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
    	int removed = 0;
    	for(ClassNode classNode : deobfuscator.classes()) {
			for(MethodNode methodNode : classNode.methods) {
				for(AbstractInsnNode insn : methodNode.instructions.toArray()) {
					if(beforeMatch.isMatch(insn)) {
						AbstractInsnNode[] beforeInsns = beforeMatch.collectMatchedInstructions(insn);
						AbstractInsnNode real = beforeInsns[beforeInsns.length - 1];
						if(real.getNext().getNext() != null) {
							if(afterMatch.isMatch(real.getNext().getNext())) {
								AbstractInsnNode[] afterInsns = afterMatch.collectMatchedInstructions(real.getNext().getNext());
								massRemove(afterInsns, methodNode);
								massRemove(beforeInsns, methodNode);
								removed += (beforeInsns.length + afterInsns.length) - 2;
							}
						}
					}
				}
			}
		}
    	LOGGER.debug("[BozarHeavyConstantTransformer] Removed " + removed + " control flow instructions");
    }
}
