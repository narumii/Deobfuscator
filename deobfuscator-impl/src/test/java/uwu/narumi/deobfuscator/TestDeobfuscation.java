package uwu.narumi.deobfuscator;

import uwu.narumi.deobfuscator.core.other.composed.ComposedHP888Transformer;
import uwu.narumi.deobfuscator.core.other.composed.ComposedZelixTransformer;
import uwu.narumi.deobfuscator.core.other.composed.general.ComposedGeneralFlowTransformer;
import uwu.narumi.deobfuscator.core.other.composed.general.ComposedPeepholeCleanTransformer;
import uwu.narumi.deobfuscator.core.other.impl.clean.peephole.JsrInlinerTransformer;
import uwu.narumi.deobfuscator.core.other.impl.pool.InlineLocalVariablesTransformer;
import uwu.narumi.deobfuscator.core.other.impl.pool.InlineStaticFieldTransformer;
import uwu.narumi.deobfuscator.core.other.impl.universal.UniversalNumberTransformer;
import uwu.narumi.deobfuscator.base.TestDeobfuscationBase;
import uwu.narumi.deobfuscator.transformer.TestSandboxSecurityTransformer;

import java.util.List;
import java.util.Map;

public class TestDeobfuscation extends TestDeobfuscationBase {

  @Override
  protected void registerAll() {
    register("Inlining local variables", InputType.JAVA_CODE, List.of(
        InlineLocalVariablesTransformer::new,
        ComposedPeepholeCleanTransformer::new
    ), Source.of("TestInlineLocalVariables"));
    register("Simple flow obfuscation", InputType.JAVA_CODE, List.of(ComposedGeneralFlowTransformer::new), Source.of("TestSimpleFlowObfuscation"));
    register("Universal Number Transformer", InputType.JAVA_CODE, List.of(UniversalNumberTransformer::new), Source.of("TestUniversalNumberTransformer"));
    // TODO: Uninitialized static fields should replace with 0?
    register("Inline static fields", InputType.JAVA_CODE, List.of(InlineStaticFieldTransformer::new), Source.of("TestInlineStaticFields"));
    register("Inline static fields with modification", InputType.JAVA_CODE, List.of(InlineStaticFieldTransformer::new), Source.of("TestInlineStaticFieldsWithModification"));

    // Test sandbox security (e.g. not allowing dangerous calls)
    register("Sandbox security", InputType.JAVA_CODE, List.of(TestSandboxSecurityTransformer::new), Source.of("sandbox/TestSandboxSecurity", false));

    // JSR Inlining
    register("JSR Inlining", InputType.CUSTOM_CLASS, List.of(JsrInlinerTransformer::new), Source.of("JSR"));

    // Samples
    register("Some flow obf sample", InputType.CUSTOM_CLASS, List.of(ComposedGeneralFlowTransformer::new), Source.of("FlowObfSample"));

    // Zelix
    register("Zelix (22.0.3) Sample 1", InputType.CUSTOM_CLASS, List.of(ComposedZelixTransformer::new),
        Source.of("zkm/sample1/ExampleClass"),
        Source.of("zkm/sample1/ILongDecrypter", false),
        Source.of("zkm/sample1/LongDecrypter1", false),
        Source.of("zkm/sample1/LongDecrypter2", false),
        // These are required to compute frames
        Source.of("zkm/sample1/ByteBufUtil", true),
        Source.of("zkm/sample1/ByteBufUtil_7", true),
        Source.of("zkm/sample1/ByteBufUtil_8", true)
    );
    // Obfuscated using this ZKM config (https://www.zelix.com/klassmaster/docs/langZKMScript.html):
    /*
    obfuscate   changeLogFileIn=""
                changeLogFileOut="ChangeLog.txt"
                obfuscateFlow=aggressive
                exceptionObfuscation=heavy
                encryptStringLiterals=enhanced
                encryptIntegerConstants=aggressive
                encryptLongConstants=normal
                mixedCaseClassNames=false
                aggressiveMethodRenaming=true
                localVariables=delete
                lineNumbers=delete
                autoReflectionHandling=normal
                obfuscateReferences=none
                methodParameterChanges=flowObfuscate
                obfuscateParameters=normal;
     */
    register("Zelix (22.0.3) Sample 2 - Class initialization order", InputType.CUSTOM_CLASS,
        List.of(
            () -> new ComposedZelixTransformer(
                // During obfuscation was specified classInitializationOrder option,
                // so we need to also pass it here for correct decrypted values
                Map.of("a.a.a.a.a4", "a.a.a.a.bc")
            )
        ),
        Source.of("zkm/sample2/bc"),
        Source.of("zkm/sample2/a4"),
        Source.of("zkm/sample2/ba"),
        Source.of("zkm/sample2/a_"),
        Source.of("zkm/sample2/ILongDecrypter", false),
        Source.of("zkm/sample2/SimpleLongDecrypter", false),
        Source.of("zkm/sample2/FallbackLongDecrypter", false)
    );
    // Obfuscated using the following ZKM config (https://www.zelix.com/klassmaster/docs/langZKMScript.html):
    /*
    obfuscate   changeLogFileIn=""
            changeLogFileOut="ChangeLog.txt"
            obfuscateFlow=aggressive
            exceptionObfuscation=heavy
            encryptStringLiterals=enhanced
            encryptIntegerConstants=aggressive
            encryptLongConstants=normal
            mixedCaseClassNames=ifInArchive
            aggressiveMethodRenaming=true
            collapsePackagesWithDefault=""
            localVariables=delete
            lineNumbers=delete
            autoReflectionHandling=normal
            obfuscateReferences=none
            methodParameterChanges=flowObfuscate
            obfuscateParameters=normal;
     */
    register("Zelix (21) Snake Game", InputType.CUSTOM_JAR,
        List.of(ComposedZelixTransformer::new),
        Source.of("SnakeGame-obf-zkm")
    );

    // Example HP888 classes. Without packer.
    register("HP888", InputType.CUSTOM_CLASS, List.of(ComposedHP888Transformer::new),
        Source.of("hp888/ExampleClass"),
        Source.of("hp888/Strings") // Obfuscated strings
    );
  }
}
