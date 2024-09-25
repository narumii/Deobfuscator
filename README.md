# Diobfuscator
A deobfuscator for java

> [!IMPORTANT]
> **This is dev(v2) version of diobfuscator version and it's not completed yet, if you are willing to help there is a list of things that needs to be completed. The old diobfuscator was moved to [v1 branch](https://github.com/narumii/Deobfuscator/tree/v1).**<br>
> 
> - Porting old transformers to new code base
> - Testing `Match` API
> - Implementing/Improving transformers
> - Writing tests
> - Feedback on how the new api presents itself (mainly `Match` API)
>   <br>
> 
> 1. *You can also provide samples of obfuscation to help with development of the transformers.*
> 2. **No... there is no gui planned**
> 3. _Also from now on Diobfuscator uses [Java Google Codestyle](https://github.com/google/styleguide/blob/gh-pages/intellij-java-google-style.xml)_
>    - `mvn fmt:format`
>    - `mvn fmt:check`

> Built on: [Java 17 (Temurin)](https://adoptium.net/temurin/releases/?version=17)

## ✅ How to run deobfuscator
If you want to use this deobfuscator, you need to start it from your IDE manually.

1. Place your obfuscated jar inside the root project directory. For example in `work/obf-test.jar`
2. Navigate to class [`Bootstrap.java`](./deobfuscator-impl/src/test/java/Bootstrap.java)
3. In this class edit the deobfuscator configuration
    - `inputJar` - Your obfuscated jar file that you placed in step 1
    - `transformers` - Pick transformers that you want to run. You can find them in [`deobfuscator-transformers`](./deobfuscator-transformers/src/main/java/uwu/narumi/deobfuscator/core/other) module.
4. Run this class manually from your IDE. You can use our pre-configured IntelliJ task named `Bootstrap`.

![tak](./assets/run-deobfuscator.gif)

## 🔧 Contributing
Contributions are welcome! See [CONTRIBUTING.md](./CONTRIBUTING.md) for a project introduction and some basics about java bytecode.

---

<p align="center">
     <a href="https://discord.gg/tRU27KtPAZ"><img src="https://discordapp.com/api/guilds/900083350314811432/widget.png?style=banner2"/></a>
</p>

---

