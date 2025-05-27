package remap;

public class Cat extends Pet {
    private final String name;

    public Cat(String name) {
        this.name = name;
    }

    @Override
    public void eat() {
        System.out.println(this.name + ": Cat is eating");
    }

    @Override
    public void sleep() {
        System.out.println(this.name + ": Cat is sleeping");
    }

    @Override
    public void play() {
        System.out.println(this.name + ": Cat is playing");
    }
}
