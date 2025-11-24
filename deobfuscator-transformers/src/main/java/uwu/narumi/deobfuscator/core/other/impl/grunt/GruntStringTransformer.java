package uwu.narumi.deobfuscator.core.other.impl.grunt;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.asm.FieldRef;
import uwu.narumi.deobfuscator.api.asm.MethodContext;
import uwu.narumi.deobfuscator.api.asm.MethodRef;
import uwu.narumi.deobfuscator.api.asm.matcher.Match;
import uwu.narumi.deobfuscator.api.asm.matcher.MatchContext;
import uwu.narumi.deobfuscator.api.asm.matcher.group.SequenceMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.*;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

// TODO: string array support

/**
 * A string transformer for Gruntpocalypse (2.x / the main branch in their repo)
 */
public class GruntStringTransformer extends Transformer {

  /**
   * getstatic Main.GDIaAJBllbXLY5N9 [Ljava/lang/String;
   * iconst_0
   * ldc "⥋脞\uECB2옧蔃氆"
   * invokevirtual java/lang/String.toCharArray ()[C
   * ldc 46256L
   * ldc 468949809
   * invokestatic Main.gL3Vqtvj62G5xf3H ([CJI)Ljava/lang/String;
   * aastore
   */

  private static final Match stringPoolAdditionMatch = SequenceMatch.of(
      FieldMatch.getStatic().capture("stringPool"),
      NumberMatch.of().capture("index"),
      StringMatch.of().capture("enc"),
      MethodMatch.invokeVirtual().owner("java/lang/String").name("toCharArray").desc("()[C"),
      NumberMatch.numLong().capture("seed"),
      NumberMatch.numInteger().capture("key"),
      MethodMatch.invokeStatic().desc("([CJI)Ljava/lang/String;").capture("decryptMethod"),
      OpcodeMatch.of(AASTORE)
  );

  private static final SequenceMatch stringDecryptorMethodMatch = SequenceMatch.of(
      NumberMatch.of().capture("key"),
      VarLoadMatch.of(),
      OpcodeMatch.of(IXOR),
      OpcodeMatch.of(ISTORE),
      NumberMatch.of(0),
      OpcodeMatch.of(ISTORE)
  );

  /**
   * class name to string pool map
   **/
  private final Map<String, String[]> classNameToStringPool = new HashMap<>();

  @Contract("_, _, _, _ -> new")
  private static @NotNull String decrypt(int classKey, char @NotNull [] encrypted, long seed, int key) {
    int cat = classKey ^ key;
    for (int i = 0; i < encrypted.length; ++i) {
      cat = cat ^ (int) seed ^ ~i;
      cat ^= key - i * encrypted.length;
      cat = -cat * key | i;
      encrypted[i] = (char) (encrypted[i] ^ cat);
      int i2 = i & 0xFF;
      key = key << i2 | key >>> -i2;
      seed ^= i2;
    }
    return new String(encrypted);
  }

  private static Integer getClassKey(MethodContext ctx) {
    var m = stringDecryptorMethodMatch.findFirstMatch(ctx);
    if (m == null) {
      return null;
    }
    return m.captures().get("key").insn().asInteger();
  }

  // removes everything matched AND the decrypt method
  private void removeAll(MatchContext ctx) {
    ctx.removeAll();
    final var m = ctx.captures().get("decryptMethod").insn().asMethodInsn();
    context().removeMethod(MethodRef.of(m));
  }

  private Optional<MethodNode> resolve(MethodInsnNode m) {
    ClassWrapper classWrapper = context().getClassesMap().get(m.owner);
    return classWrapper.findMethod(m);
  }


  private void deobfClass(@NotNull ClassWrapper c) {
    final var initOpt = c.findClInit();
    if (initOpt.isEmpty()) return;
    final var init = initOpt.get();
    FieldRef stringPoolRef = null;
    for (final var insn : init.instructions) {
      if (insn instanceof MethodInsnNode min) {
        final var resolved = resolve(min);
        if (resolved.isEmpty()) return;
        var r = resolved.get();
        var ms = stringPoolAdditionMatch.findAllMatches(MethodContext.of(c, r));
        if (ms.isEmpty()) {
          LOGGER.warn("couldn't find any strings in {}#{}{}", min.owner, min.name, min.desc);
          return;
        }
        Integer classKey = null;
        var pool = new String[ms.size()];
        for (var m : ms) {
          var cap = m.captures();
          var i = cap.get("index").insn().asInteger();
          var enc = cap.get("enc").insn().asString();
          var seed = cap.get("seed").insn().asLong();
          var key = cap.get("key").insn().asInteger();
          var dm = cap.get("decryptMethod").insn().asMethodInsn();
          var sp = cap.get("stringPool").insn().asFieldInsn();
          stringPoolRef = FieldRef.of(sp);
          final var resolved2 = c.findMethod(dm);
          if (resolved2.isEmpty()) return;
          var r2 = resolved2.get();
          if (classKey == null) classKey = getClassKey(MethodContext.of(c, r2));
          if (classKey == null) return;
          var str = decrypt(classKey, enc.toCharArray(), seed, key);
          pool[i] = str;
        }
        this.classNameToStringPool.put(c.name(), pool);
        var lol = SequenceMatch.of(
            NumberMatch.numInteger(),
            OpcodeMatch.of(ANEWARRAY),
            FieldMatch.putStatic().fieldRef(stringPoolRef),
            MethodMatch.invokeStatic().owner(min.owner).name(min.name).desc(min.desc)
        );
        final var ffm = lol.findFirstMatch(MethodContext.of(c, init));
        if (ffm != null)
            ffm.removeAll();
        ms.forEach(this::removeAll);
        c.methods().remove(r);
      }
    }
    var spr = stringPoolRef;
    var stringPoolUse = SequenceMatch.of(
        FieldMatch.getStatic().fieldRef(spr),
        NumberMatch.numInteger().capture("index"),
        OpcodeMatch.of(AALOAD)
    );
    final var pool = this.classNameToStringPool.get(c.name());
    c.methods().forEach(method -> {
      var ms = stringPoolUse.findAllMatches(MethodContext.of(c, method));
      for (final var m : ms) {
        final var i = m.captures().get("index").insn().asInteger();
        final var dec = pool[i];
        if (dec == null) {
          LOGGER.warn("Couldn't find a string in the string pool for encrypted string at {}", i);
          continue;
        }
        method.instructions.insertBefore(m.insn(), new LdcInsnNode(dec));
        m.removeAll();
        markChange();
      }
    });
    // owner check is useless I think
    c.fields().removeIf(f -> f.name.equals(spr.name()) && f.desc.equals(spr.desc()));
    if (init.instructions.size() <= 0) {
      c.methods().remove(init);
    }
  }

  @Override
  protected void transform() throws Exception {
    scopedClasses().forEach(this::deobfClass);
  }
}
