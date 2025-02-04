package uwu.narumi.deobfuscator.api.asm.remapper;

import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * A collection of common name patterns.
 */
public class NamePattern {
  // iIIIiIIlliiIIlIii
  public static final Predicate<String> III = Pattern.compile("^[lIi]*$").asMatchPredicate();
  public static final Predicate<String> WEB_EXPLOIT = s -> s.contains("<html>");
}
