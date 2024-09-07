package uwu.narumi.deobfuscator.api.asm.matcher.rule;

import org.objectweb.asm.tree.AbstractInsnNode;

import java.util.List;
import java.util.Map;

/**
 * Result of {@link Match#matchResult(MatchContext)}
 *
 * @param start Starting point of the match
 * @param storage Storage for saving some instructions ({@link Match#save(String)}) in matching process. id -> instruction
 * @param collectedInsns Collected instructions that matches this match and children matches
 */
public record MatchResult(
    AbstractInsnNode start,
    Map<String, AbstractInsnNode> storage,
    List<AbstractInsnNode> collectedInsns
) {
}
