package remappreserve;

import remappreserve.nes.ted.DeeplyNestedClass;

public class AnotherInMain {
    public void run() {
        System.out.println("AnotherInMain running...");
        MainClass mc = new MainClass();
        mc.printGreeting();

        DeeplyNestedClass dnc = new DeeplyNestedClass();
        mc.callNested(dnc);
        dnc.doSomething();
    }
}
