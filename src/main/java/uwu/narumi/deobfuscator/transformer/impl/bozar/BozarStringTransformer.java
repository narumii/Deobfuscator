package uwu.narumi.deobfuscator.transformer.impl.bozar;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;

import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.matcher.InstructionMatcher;
import uwu.narumi.deobfuscator.matcher.IMatch;
import uwu.narumi.deobfuscator.matcher.impl.IntegerMatch;
import uwu.narumi.deobfuscator.matcher.impl.MethodMatch;
import uwu.narumi.deobfuscator.matcher.impl.OpcodeMatch;
import uwu.narumi.deobfuscator.transformer.Transformer;

public class BozarStringTransformer extends Transformer {
	InstructionMatcher createByteArray = new InstructionMatcher(new IMatch[] {
		new IntegerMatch(),
		new IMatch() {
			@Override
			public boolean match(AbstractInsnNode insn) {
				return insn.getOpcode() == NEWARRAY && ((IntInsnNode)insn).operand == T_BYTE;
			}
		},
		new OpcodeMatch(ASTORE)
	});
	
	InstructionMatcher putValueAtArray = new InstructionMatcher(new IMatch[] {
		new OpcodeMatch(ALOAD),
		new IntegerMatch(),
		new IntegerMatch(),
		new OpcodeMatch(BASTORE)
	});
	
	InstructionMatcher createStringFromByteArray = new InstructionMatcher(new IMatch[] {
		new OpcodeMatch(NEW),
		new OpcodeMatch(DUP),
		new OpcodeMatch(ALOAD),
		new MethodMatch(INVOKESPECIAL, "java/lang/String", "([B)V", "<init>"),
	});
	
    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
    	int count = 0;
    	for(ClassNode classNode : deobfuscator.classes()) {
			for(MethodNode methodNode : classNode.methods) {
				for(AbstractInsnNode insn : methodNode.instructions.toArray()) {
					if(createByteArray.isMatch(insn)) {
						AbstractInsnNode[] createArrayInsns = createByteArray.collectMatchedInstructions(insn);
						
						int strSize = getInteger(createArrayInsns[0]);
						byte[] byteArray = new byte[strSize];
						
						AbstractInsnNode nextInsn = createArrayInsns[2].getNext();
						if(nextInsn != null) {
							while(putValueAtArray.isMatch(nextInsn)) {
								AbstractInsnNode[] insns = putValueAtArray.collectMatchedInstructions(nextInsn);
								
								int index = getInteger(insns[1]);
								byte value = (byte) getInteger(insns[2]);
								byteArray[index] = value;
								
								nextInsn = insns[3].getNext();
								
								massRemove(insns, methodNode);
							}
							if(createStringFromByteArray.isMatch(nextInsn)) {
								massRemove(createStringFromByteArray.collectMatchedInstructions(nextInsn), methodNode);
								String deobfuscatedString = new String(byteArray);
								methodNode.instructions.insertBefore(insn, new LdcInsnNode(deobfuscatedString));
								massRemove(createByteArray.collectMatchedInstructions(insn), methodNode);
								count++;
							}
						}
					}
				}
			}	
    	}
    	LOGGER.debug("[BozarStringTransformer] Deobfuscated " + count + " strings");
    }
}