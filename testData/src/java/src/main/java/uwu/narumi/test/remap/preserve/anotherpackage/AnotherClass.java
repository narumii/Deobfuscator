package uwu.narumi.test.remap.preserve.anotherpackage;

import uwu.narumi.test.remap.preserve.MyClass;

public class AnotherClass {
    public void anotherMethod() {
        MyClass mc = new MyClass();
        mc.myMethod();
        MyClass.NestedClass nc = new MyClass.NestedClass();
        nc.nestedMethod();
        System.out.println("Hello from AnotherClass");
    }
}
