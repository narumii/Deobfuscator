package uwu.narumi.deobfuscator.transformer.impl.bozar.v1_7;

import java.util.List;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodNode;

import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.matcher.IMatch;
import uwu.narumi.deobfuscator.matcher.InstructionMatcher;
import uwu.narumi.deobfuscator.matcher.impl.ClassMatch;
import uwu.narumi.deobfuscator.matcher.impl.LongMatch;
import uwu.narumi.deobfuscator.matcher.impl.NumberMatch;
import uwu.narumi.deobfuscator.matcher.impl.OpcodeMatch;
import uwu.narumi.deobfuscator.transformer.Transformer;

public class BozarLightFlowTransformer extends Transformer {
	InstructionMatcher beforeMatch = new InstructionMatcher(new IMatch[] {
		new OpcodeMatch(GOTO),
		new ClassMatch(LabelNode.class),
		new OpcodeMatch(POP),
		new ClassMatch(LabelNode.class),
		new OpcodeMatch(GETSTATIC),
		new NumberMatch(),
		new OpcodeMatch(LCMP),
		new OpcodeMatch(DUP),
		new OpcodeMatch(IFEQ),
		new NumberMatch(),
		new OpcodeMatch(IF_ICMPNE)
	});
	
	InstructionMatcher afterMatch = new InstructionMatcher(new IMatch[] {
		new OpcodeMatch(GOTO),
		new ClassMatch(LabelNode.class),
		new NumberMatch(),
		new OpcodeMatch(GOTO),
		new ClassMatch(LabelNode.class)
	});
	
	InstructionMatcher beforeSecondMatch = new InstructionMatcher(new IMatch[] {
		new OpcodeMatch(GETSTATIC),
		new OpcodeMatch(GOTO),
		new ClassMatch(LabelNode.class),
		new LongMatch(),
		new OpcodeMatch(LDIV),
		new ClassMatch(LabelNode.class),
		new OpcodeMatch(L2I),
		new ClassMatch(LookupSwitchInsnNode.class)
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
					} else if(beforeSecondMatch.isMatch(insn)) {
						AbstractInsnNode[] beforeInsns = beforeMatch.collectMatchedInstructions(insn);
						FieldInsnNode flowField = (FieldInsnNode) beforeInsns[0];
						LookupSwitchInsnNode xD = (LookupSwitchInsnNode) beforeInsns[7];
						List<AbstractInsnNode> list = getInstructionsBetween(insn, xD.dflt);
						for(AbstractInsnNode node : list) {
							methodNode.instructions.remove(node);
							removed += list.size();
						}
						classNode.fields.removeIf(field -> field.name.equals(flowField.name) && field.desc.equals(flowField.desc));
						removed++;
					}
				}
			}
		}
    	LOGGER.debug("[BozarLightFlowTransformer] Removed " + removed + " control flow instructions");
    }
}
