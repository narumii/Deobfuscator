package remappreserve.nes.ted;

import remappreserve.MainClass;

public class DeeplyNestedClass {
    public void doSomething() {
        System.out.println("DeeplyNestedClass reporting in!");
        System.out.println("Accessing MainClass: " + MainClass.GREETING);
        new MainClass().printGreeting();
    }
}
