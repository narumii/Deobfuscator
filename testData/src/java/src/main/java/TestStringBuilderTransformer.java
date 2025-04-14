public class TestStringBuilderTransformer {
  public void stringBuilderAppends() {
    System.out.println(
        new StringBuilder()
            .append("foo")
            .append("bar")
            .append("test")
            .append("123")
            .toString()
    );

    System.out.println("a" + "b" + "c" + "d" + "e" + "f" + "g" + "h" + "i" + "j");
  }
}
