package remap;

public abstract class Pet {
    public abstract void eat();

    public abstract void sleep();

    public abstract void play();

    public void display() {
        System.out.println("Displaying pet");
    }
}
