package uwu.narumi.deobfuscator.api.transformer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.BasicVerifier;
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.context.Context;
import uwu.narumi.deobfuscator.api.exception.TransformerException;
import uwu.narumi.deobfuscator.api.helper.AsmHelper;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public abstract class Transformer extends AsmHelper implements Opcodes {

  protected static final Logger LOGGER = LogManager.getLogger(Transformer.class);

  // Internal variables
  private boolean hasRan = false;
  private final AtomicInteger changes = new AtomicInteger(0);

  // Config
  protected boolean rerunOnChange = false;

  /**
   * Should the transformer rerun if it changed something
   */
  public boolean shouldRerunOnChange() {
    return rerunOnChange;
  }

  /**
   * Do the transformation. If you implement it you MUST use {@link Transformer#markChange()} somewhere
   *
   * @param scope You can choose the class transform or set it to null to transform all classes
   * @param context The context
   */
  protected abstract void transform(ClassWrapper scope, Context context) throws Exception;

  /**
   * Marks that transformer changed something. You must use it in your transformer for
   */
  protected void markChange() {
    this.changes.incrementAndGet();
  }

  public int getChangesCount() {
    return this.changes.get();
  }

  public boolean isChanged() {
    return this.getChangesCount() > 0;
  }

  public String name() {
    return this.getClass().getSimpleName();
  }

  /**
   * Run the transformer
   *
   * @param transformerSupplier The transformer supplier with all its configuration ready to go. Required to recreate
   *                            transformer multiple times with the same configuration. You must pass here new instance.
   *                            You can't reuse the existing instance.
   * @param scope You can choose the class transform or set it to null to transform all classes
   * @param context The context
   * @return If the transformation changed something
   */
  public static boolean transform(Supplier<Transformer> transformerSupplier, @Nullable ClassWrapper scope, Context context) {
    return transform(transformerSupplier, scope, context, false);
  }

  private static boolean transform(
      Supplier<Transformer> transformerSupplier,
      @Nullable ClassWrapper scope,
      Context context,
      boolean reran
  ) {
    Transformer transformer = transformerSupplier.get();
    if (transformer.hasRan) {
      throw new IllegalArgumentException("transformerSupplier tried to reuse transformer instance. You must pass a new instance of transformer");
    }

    LOGGER.info("-------------------------------------");

    LOGGER.info("Running {} transformer", transformer.name());
    long start = System.currentTimeMillis();

    // Run the transformer!
    try {
      transformer.transform(scope, context);
    } catch (TransformerException e) {
      LOGGER.error("! {}: {}", transformer.name(), e.getMessage());
      return false;
    } catch (Exception e) {
      LOGGER.error("Error occurred when transforming {}", transformer.name(), e);
      if (!context.getOptions().suppressErrors()) {
        throw new RuntimeException(e);
      }
      return false;
    } finally {
      // Mark transformer that it was already used
      transformer.hasRan = true;
    }

    boolean changed = transformer.isChanged();

    LOGGER.info("Ended {} transformer in {} ms", transformer.name(), (System.currentTimeMillis() - start));

    if (changed && transformer.shouldRerunOnChange()) {
      LOGGER.info("\uD83D\uDD04 Changes detected. Rerunning {} transformer", transformer.name());
      Transformer.transform(transformerSupplier, scope, context, true);
    }

    // Bytecode verification
    if (context.getOptions().verifyBytecode() && !reran && changed) {
      // Verify if bytecode is valid
      try {
        verifyBytecode(scope, context);
      } catch (RuntimeException e) {
        LOGGER.error("Transformer {} produced invalid bytecode", transformer.name(), e);
      }
    }

    return changed;
  }

  /**
   * Verifies if the bytecode is valid
   */
  private static void verifyBytecode(@Nullable ClassWrapper scope, Context context) throws IllegalStateException {
    for (ClassWrapper classWrapper : context.classes(scope)) {
      for (MethodNode methodNode : classWrapper.methods()) {
        Analyzer<BasicValue> analyzer = new Analyzer<>(new BasicVerifier());
        try {
          analyzer.analyzeAndComputeMaxs(classWrapper.name(), methodNode);
        } catch (AnalyzerException e) {
          throw new IllegalStateException("Invalid bytecode in " + classWrapper.name() + "#" + methodNode.name + methodNode.desc, e);
        }
      }
    }
  }
}
