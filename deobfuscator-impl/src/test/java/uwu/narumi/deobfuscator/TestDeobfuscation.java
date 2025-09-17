package uwu.narumi.deobfuscator;

import uwu.narumi.deobfuscator.core.other.composed.*;
import uwu.narumi.deobfuscator.core.other.composed.general.ComposedGeneralFlowTransformer;
import uwu.narumi.deobfuscator.core.other.composed.general.ComposedPeepholeCleanTransformer;
import uwu.narumi.deobfuscator.core.other.impl.branchlock.BranchlockCompabilityStringTransformer;
import uwu.narumi.deobfuscator.core.other.impl.branchlock.BranchlockFlowTransformer;
import uwu.narumi.deobfuscator.core.other.impl.clean.peephole.JsrInlinerTransformer;
import uwu.narumi.deobfuscator.core.other.impl.clean.peephole.UselessPopCleanTransformer;
import uwu.narumi.deobfuscator.core.other.impl.pool.InlineLocalVariablesTransformer;
import uwu.narumi.deobfuscator.core.other.impl.pool.InlineStaticFieldTransformer;
import uwu.narumi.deobfuscator.core.other.impl.universal.RemapperTransformer;
import uwu.narumi.deobfuscator.core.other.impl.universal.StringBuilderTransformer;
import uwu.narumi.deobfuscator.core.other.impl.universal.UniversalNumberTransformer;
import uwu.narumi.deobfuscator.base.TestDeobfuscationBase;
import uwu.narumi.deobfuscator.transformer.TestSandboxSecurityTransformer;

import java.util.Map;

public class TestDeobfuscation extends TestDeobfuscationBase {

  @Override
  protected void registerAll() {
    test("Inlining local variables")
        .transformers(InlineLocalVariablesTransformer::new, ComposedPeepholeCleanTransformer::new)
        .inputClass(InputType.JAVA_CODE, "TestInlineLocalVariables.class")
        .register();
    test("Simple flow obfuscation")
        .transformers(ComposedGeneralFlowTransformer::new)
        .inputClass(InputType.JAVA_CODE, "TestSimpleFlowObfuscation.class")
        .register();
    test("Universal number transformer")
        .transformers(UniversalNumberTransformer::new)
        .inputClass(InputType.JAVA_CODE, "TestUniversalNumberTransformer.class")
        .register();
    test("String builder transformer")
        .transformers(StringBuilderTransformer::new)
        .inputClass(InputType.JAVA_CODE, "TestStringBuilderTransformer.class")
        .register();
    // TODO: Uninitialized static fields should replace with 0?
    test("Inline static fields")
        .transformers(InlineStaticFieldTransformer::new, UselessPopCleanTransformer::new)
        .inputClass(InputType.JAVA_CODE, "TestInlineStaticFields.class")
        .register();
    test("Inline static fields with modification")
        .transformers(InlineStaticFieldTransformer::new, UselessPopCleanTransformer::new)
        .inputClass(InputType.JAVA_CODE, "TestInlineStaticFieldsWithModification.class")
        .register();
    test("Remapper")
        .transformers(RemapperTransformer::new)
        .inputClassesDir(InputType.JAVA_CODE, "remap")
        .register();

    // Test sandbox security (e.g. not allowing dangerous calls)
    test("Sandbox security")
        .transformers(TestSandboxSecurityTransformer::new)
        .inputClass(InputType.JAVA_CODE, "sandbox/TestSandboxSecurity.class")
        .noDecompile()
        .register();

    // JSR Inlining
    test("JSR Inlining")
        .transformers(JsrInlinerTransformer::new)
        .inputClass(InputType.CUSTOM_CLASS, "JSR.class")
        .register();

    // Unknown obf 1
    // TODO: If you know the obfuscator used to obfuscate this class, you can make a PR which renames this test
    test("Unknown obf 1")
        .transformers(ComposedUnknownObf1Transformer::new)
        .inputClassesDir(InputType.CUSTOM_CLASS, "unknown/obf1")
        .register();

    // Zelix
    test("Zelix (22.0.3) Sample 1")
        .transformers(() -> new ComposedZelixTransformer(true))
        .inputClassesDir(InputType.CUSTOM_CLASS, "zkm/sample1")
        .register();
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
    test("Zelix (22.0.3) Sample 2 - Class initialization order")
        .transformers(() -> new ComposedZelixTransformer(true,
            // During obfuscation was specified classInitializationOrder option,
            // so we need to also pass it here for correct decrypted values
            Map.of("a.a.a.a.a4", "a.a.a.a.bc")
        ))
        .inputClassesDir(InputType.CUSTOM_CLASS, "zkm/sample2")
        .register();
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
    test("Zelix (22.0.3) Sample 3 - Snake Game")
        .transformers(() -> new ComposedZelixTransformer(true))
        .inputJar("SnakeGame-obf-zkm.jar")
        .register();

    // Zelix (22.0.3)
    /*
    obfuscate   changeLogFileIn=""
                changeLogFileOut="ChangeLog.txt"
                obfuscateFlow=none
                exceptionObfuscation=none
                encryptStringLiterals=enhanced
                encryptIntegerConstants=none
                encryptLongConstants=none
                mixedCaseClassNames=ifInArchive
                collapsePackagesWithDefault=""
                localVariables=delete
                lineNumbers=delete
                autoReflectionHandling=normal
                obfuscateReferences=none;
     */
    test("Zelix (22.0.3) - String Encryption - Enhanced - Some strings")
        .transformers(ComposedZelixTransformer::new)
        .inputClass(InputType.CUSTOM_CLASS, "zkm/EnhancedStringEncSomeStrings.class")
        .register();
    test("Zelix (22.0.3) - String Encryption - Enhanced - Many strings")
        .transformers(ComposedZelixTransformer::new)
        .inputClass(InputType.CUSTOM_CLASS, "zkm/EnhancedStringEncManyStrings.class")
        .register();

    // Example HP888 classes
    test("HP888")
        .transformers(() -> new ComposedHP888Transformer(".mc"))
        .inputClassesDir(InputType.CUSTOM_CLASS, "hp888")
        .register();

    // qProtect
    test("qProtect Sample 1")
        .transformers(Composed_qProtectTransformer::new)
        .inputClassesDir(InputType.CUSTOM_CLASS, "qprotect/sample1")
        .register();

    test("qProtect Sample 2")
        .transformers(Composed_qProtectTransformer::new, RemapperTransformer::new)
        .inputClassesDir(InputType.CUSTOM_CLASS, "qprotect/sample2")
        .register();

    test("qProtect Sample 3")
        .transformers(Composed_qProtectTransformer::new)
        .inputClassesDir(InputType.CUSTOM_CLASS, "qprotect/sample3")
        .register();

    test("qProtect-Lite Jar Sample")
        .transformers(Composed_qProtectTransformer::new)
        .inputJar("qprotect-obf.jar")
        .register();

    // Superblaubeere
    test("Superblaubeere Sample 1")
        .transformers(ComposedSuperblaubeereTransformer::new)
        .inputClassesDir(InputType.CUSTOM_CLASS, "sb27/sample1")
        .register();

    // Grunt
    test("Grunt Sample 1")
        .transformers(ComposedGruntTransformer::new)
        .inputJar("grunt-obf.jar")
        .register();

    test("POP2 Sample")
        .transformers(UselessPopCleanTransformer::new)
        .inputClass(InputType.CUSTOM_CLASS, "Pop2Sample.class")
        .register();

    test("Kotlin Sample")
        .transformers(UselessPopCleanTransformer::new)
        .inputClass(InputType.CUSTOM_CLASS, "KotlinSample.class")
        .register();

    test("Kotlin Sample 2")
        .transformers(UselessPopCleanTransformer::new)
        .inputClass(InputType.CUSTOM_CLASS, "KotlinSample2.class")
        .register();

    test("Kotlin Sample 3")
        .transformers(UselessPopCleanTransformer::new)
        .inputClass(InputType.CUSTOM_CLASS, "KotlinSample3.class")
        .register();

    test("Branchlock String")
        .transformers(UniversalNumberTransformer::new, BranchlockCompabilityStringTransformer::new)
        .inputJar("branchlock/branchlock-string.jar")
        .register();

    test("Branchlock String + Salting + Number")
        .transformers(ComposedBranchlockTransformer::new)
        .inputJar("branchlock/branchlock-string-salting-number.jar")
        .register();

    test("Branchlock String + Flow + Number")
        .transformers(ComposedBranchlockTransformer::new)
        .inputJar("branchlock/branchlock-string-flow-number.jar")
        .register();

//    test("Branchlock String + Salting + Flow + Number")
//        .transformers(ComposedBranchlockTransformer::new)
//        .inputJar("branchlock/branchlock-string-salting-flow-number.jar")
//        .register();

    test("Branchlock Flow 9")
        .transformers(BranchlockFlowTransformer::new)
        .inputJar("branchlock/flow/flow 9.jar")
        .register();
  }
}
