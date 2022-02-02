package uwu.narumi.deobfuscator.transformer.impl.bozar.v1_6;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;

import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.matcher.IMatch;
import uwu.narumi.deobfuscator.matcher.InstructionMatcher;
import uwu.narumi.deobfuscator.matcher.impl.ClassMatch;
import uwu.narumi.deobfuscator.matcher.impl.IntegerMatch;
import uwu.narumi.deobfuscator.matcher.impl.LongMatch;
import uwu.narumi.deobfuscator.matcher.impl.NumberMatch;
import uwu.narumi.deobfuscator.matcher.impl.OpcodeMatch;
import uwu.narumi.deobfuscator.matcher.impl.SkipMatch;
import uwu.narumi.deobfuscator.transformer.Transformer;

// TODO: recode
public class BozarControlFlowTransformer extends Transformer {
	InstructionMatcher beforeMatch = new InstructionMatcher(new IMatch[] {
		new IMatch() {
			@Override
			public boolean match(AbstractInsnNode insn) {
				if(insn.getOpcode() == GETSTATIC) return true;
				
				return insn != null && insn.getPrevious() != null && insn instanceof LabelNode && insn.getPrevious().getOpcode() == GETSTATIC;
			}
		},
		new NumberMatch(),
		new OpcodeMatch(GOTO)
	});
	InstructionMatcher beforeSecondMatch_random0 = new InstructionMatcher(new IMatch[] {
		new OpcodeMatch(LXOR),
		new SkipMatch(),
		new OpcodeMatch(LCMP),
		new OpcodeMatch(IFNE),
		new OpcodeMatch(ALOAD),
		new OpcodeMatch(IFNULL),
		new SkipMatch(),
		new OpcodeMatch(ASTORE),
		new OpcodeMatch(GOTO),
		new ClassMatch(LabelNode.class)
	});
	
	InstructionMatcher beforeSecondMatch_random1 = new InstructionMatcher(new IMatch[] {
		new OpcodeMatch(LCMP),
		new OpcodeMatch(ISTORE),
		new OpcodeMatch(ILOAD),
		new ClassMatch(JumpInsnNode.class),
		new OpcodeMatch(ILOAD),
		new NumberMatch(),
		new OpcodeMatch(IF_ICMPNE)
	});
	
	InstructionMatcher beforeSecondMatch_random2 = new InstructionMatcher(new IMatch[] {
		new OpcodeMatch(LAND),
		new NumberMatch(),
		new OpcodeMatch(LCMP),
		new OpcodeMatch(IFNE),
	});
	
	InstructionMatcher afterSecondMatch = new InstructionMatcher(new IMatch[] {
		new OpcodeMatch(GETSTATIC),
		new LongMatch(),
		new OpcodeMatch(LCMP),
		new IntegerMatch(),
		new OpcodeMatch(IF_ICMPNE),
		new OpcodeMatch(ACONST_NULL),
		new OpcodeMatch(ATHROW)
	});
	
	InstructionMatcher uselessIf = new InstructionMatcher(new IMatch[] {
		new NumberMatch(),
		new OpcodeMatch(GOTO),
		new ClassMatch(LabelNode.class),
		new NumberMatch(),
		new NumberMatch(),
		new ClassMatch(JumpInsnNode.class)
	});
	
	@Override
	public void transform(Deobfuscator deobfuscator) throws Exception {
		int removed = 0;
    	for(ClassNode classNode : deobfuscator.classes()) {
			for(MethodNode methodNode : classNode.methods) {
				for(AbstractInsnNode insn : methodNode.instructions.toArray()) {
					if(beforeMatch.isMatch(insn)) {
						AbstractInsnNode[] beforeInsns = beforeMatch.collectMatchedInstructions(insn);
						AbstractInsnNode last = beforeInsns[beforeInsns.length - 1].getNext();
						// i want to kill myself
						while(!(beforeSecondMatch_random0.isMatch(last)
								|| beforeSecondMatch_random1.isMatch(last)
								|| beforeSecondMatch_random2.isMatch(last))) {
							if(last.getNext() == null) {
								break;
							}
							last = last.getNext();
						}
						
						if(beforeSecondMatch_random0.isMatch(last)) {
							AbstractInsnNode[] insns = beforeSecondMatch_random0.collectMatchedInstructions(last);
							removed += removeInstructionsBetween(methodNode, insn, insns[insns.length - 1]);
						} else if(beforeSecondMatch_random1.isMatch(last)) {
							AbstractInsnNode[] insns = beforeSecondMatch_random1.collectMatchedInstructions(last);
							removed += removeInstructionsBetween(methodNode, insn, insns[insns.length - 1]);
						} else if(beforeSecondMatch_random2.isMatch(last)) {
							AbstractInsnNode[] insns = beforeSecondMatch_random2.collectMatchedInstructions(last);
							AbstractInsnNode last2 = insns[insns.length - 1].getNext().getNext();
							if(afterSecondMatch.isMatch(last2)) {
								AbstractInsnNode[] afterInsns = afterSecondMatch.collectMatchedInstructions(last2);
								for(AbstractInsnNode flowInsn : getInstructionsBetween(insn, afterInsns[afterInsns.length - 1])) {
									if(flowInsn != afterInsns[0].getPrevious()) {
										methodNode.instructions.remove(flowInsn);
										removed++;
									}
								}
							}
						}
					}
				}
			}
			for(MethodNode methodNode : classNode.methods) {
				for(AbstractInsnNode insn : methodNode.instructions.toArray()) {
					if(insn instanceof LdcInsnNode && ((LdcInsnNode)insn).cst.toString().equalsIgnoreCase("()Z") && insn.getNext().getOpcode() == ASTORE) {
						methodNode.instructions.remove(insn.getNext());
						methodNode.instructions.remove(insn);
						removed += 2;
					}
					if(uselessIf.isMatch(insn)) {
						AbstractInsnNode[] insns = uselessIf.collectMatchedInstructions(insn);
						massRemove(insns, methodNode);
						removed += insns.length -1;
					}
				}
			}
    	}
    	LOGGER.debug("[BozarControlFlowTransformer] Removed " + removed + " control flow instructions");
	}
	
	private int removeInstructionsBetween(MethodNode methodNode, AbstractInsnNode start, AbstractInsnNode end) {
		int removed = 0;
		for(AbstractInsnNode insn : getInstructionsBetween(start, end)) {
			methodNode.instructions.remove(insn);
			removed++;
		}
		return removed;
	}
}
