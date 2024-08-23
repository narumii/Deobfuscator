# Contributing

## How to run deobfuscator
1. Navigate to class `deobfuscator-impl/src/test/java/Bootstrap`
2. In this class edit the deobfuscator configuration
    - `input` - Your input jar file
    - `transformers` - Pick transformers that you want to run. You can find them in `deobfuscator-transformers` module.
3. Run this class manually from your IDE

![tak](./assets/run-deobfuscator.gif)

## Project structure
The project is structured as follows:
- `deobfuscator-api` - The API for the deobfuscator.
- `deobfuscator-impl` - The main deobfuscator runner.
- `deobfuscator-transformers` - Transformers for the deobfuscator.
- `deobfuscator-transformers-analyzer` - Analyzer-like transformers
- `testData` - Tests for transformers
  - `src/java` - You can write your java code to test transformers
  - `compiled/custom-classes` - Compiled classes to test transformers. You can throw here classes from your obfuscated jars.
  - `compiled/custom-jars` - Jars to test transformers. You can throw here your obfuscated jars.
  - `results` - Expected results that are auto-generated decompiled java code
- `deobfuscator-impl/src/test/java/uwu/narumii/deobfuscator/TestDeobfuscation` - Class where each test sample is registered.

## Transformers
### What are transformers?
Whole deobfuscation process is based on transformers. Transformers are smaller classes that are responsible for deobfuscating specific obfuscation techniques. In simple words - transformers are transforming obfuscated code into a more readable form.

### How to create your own transformer?
1. Create a new class in `deobfuscator-transformers` module.
2. Pick `Transformer`-like class you would like to implement:
    - `Transformer` - Basic transformer that transforms classes.
    - `ComposedTransformer` - Transformer that consists of multiple transformers.
3. You can start coding!

## Testing
### How these test work?
1. The registered samples are transformed using corresponding transformers.
2. The output gets decompiled using Vineflower.
3. The output gets compared with the expected output.

### How to create your own tests?
You can create your own tests for transformers. There are a few ways to do it:
- If the obfuscation is simple enough, you can write your own sample in `testData/src/java`.
- If the obfuscation is more complex, you can throw your obfuscated classes into `testData/compiled/custom-classes` and test transformers on them.
- You can also throw your obfuscated jars into `testData/compiled/custom-jars` and test transformers on them.

You need to register each sample in class `deobfuscator-impl/src/test/java/uwu/narumii/deobfuscator/TestDeobfuscation`

