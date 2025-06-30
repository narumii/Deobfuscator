package remappreserve;

public class MainClass {
    public static String GREETING = "Hello from MainClass";

    public void printGreeting() {
        System.out.println(GREETING);
    }

    public void callNested(remappreserve.nes.ted.DeeplyNestedClass dnc) {
        dnc.doSomething();
    }
}
