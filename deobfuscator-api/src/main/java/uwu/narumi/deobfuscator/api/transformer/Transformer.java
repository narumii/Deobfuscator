package uwu.narumi.deobfuscator.api.transformer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.context.Context;
import uwu.narumi.deobfuscator.api.exception.TransformerException;
import uwu.narumi.deobfuscator.api.helper.AsmHelper;

import java.util.function.Supplier;

public abstract class Transformer extends AsmHelper implements Opcodes {

  protected static final Logger LOGGER = LogManager.getLogger(Transformer.class);

  // Config
  protected boolean rerunOnChange = false;

  /**
   * Should the transformer rerun if it changed something
   */
  public boolean shouldRerunOnChange() {
    return rerunOnChange;
  }

  /**
   * Do the transformation
   *
   * @param scope You can choose the class to scope the transformer or set it to null to transform all classes
   * @param context The context
   * @return If the transformation changed something
   */
  protected abstract boolean transform(ClassWrapper scope, Context context) throws Exception;

  public String name() {
    return this.getClass().getSimpleName();
  }

  /**
   * Run the transformer
   *
   * @param transformerSupplier The transformer supplier with all its configuration ready to go. Required to recreate
   *                            transformer multiple times with the same configuration. You must pass here new instance.
   *                            You can't reuse the existing instance.
   * @param scope You can choose the class to scope the transformer or set it to null to transform all classes
   * @param context The context
   */
  public static boolean transform(Supplier<Transformer> transformerSupplier, @Nullable ClassWrapper scope, Context context) {
    return transform(transformerSupplier, scope, context, null);
  }

  private static boolean transform(
      Supplier<Transformer> transformerSupplier,
      @Nullable ClassWrapper scope,
      Context context,
      @Nullable Transformer oldInstance
  ) {
    boolean changed = false;

    Transformer transformer = transformerSupplier.get();
    if (oldInstance != null && transformer == oldInstance) {
      throw new IllegalArgumentException("transformerSupplier tried to reuse existing transformer instance. You must pass a new instance of transformer");
    }

    LOGGER.info("-------------------------------------");
    try {

      LOGGER.info("Running {} transformer", transformer.name());
      long start = System.currentTimeMillis();

      // Run the transformer!
      changed = transformer.transform(scope, context);
      if (changed && transformer.shouldRerunOnChange()) {
        LOGGER.info("Changes detected. Rerunning {} transformer", transformer.name());
        Transformer.transform(transformerSupplier, scope, context, transformer);
      }

      LOGGER.info("Ended {} transformer in {} ms", transformer.name(), (System.currentTimeMillis() - start));
    } catch (TransformerException e) {
      LOGGER.error("! {}: {}", transformer.name(), e.getMessage());
    } catch (Exception e) {
      LOGGER.error("Error occurred when transforming {}", transformer.name());
      LOGGER.debug("Error", e);
    }
    LOGGER.info("-------------------------------------\n");

    return changed;
  }
}
