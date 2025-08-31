package remap;

@PetMeta(type = "Cat")
public class Cat extends Pet {
  private final String name;

  public Cat(String name) {
    this.name = name;
  }

  @Override
  public void eat() {
    String type = getClass().getAnnotation(PetMeta.class).type();
    System.out.println(this.name + ": " + type + " is eating");
  }

  @Override
  public void sleep() {
    String type = getClass().getAnnotation(PetMeta.class).type();
    System.out.println(this.name + ": " + type + " is sleeping");
  }

  @Override
  public void play() {
    String type = getClass().getAnnotation(PetMeta.class).type();
    System.out.println(this.name + ": " + type + " is playing");
  }
}
