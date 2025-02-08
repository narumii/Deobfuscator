package remap;

public class Main {
    public static void main(String[] args) {
        System.out.println("Hello, World!");

        Cat cat = new Cat("Fiona");
        cat.eat();
        cat.play();
        cat.sleep();
        cat.display();

        SampleRunnable runnable = new SampleRunnable();
        runnable.run();
    }
}