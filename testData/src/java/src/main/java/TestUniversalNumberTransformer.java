public class TestUniversalNumberTransformer {
  public void testNumbers1() {
    int a = 1 + 2;
    int b = 235434535 / 12323432;
    double c = 123123.123123 * 123123.123123;
    float d = 123123.123123f / 12.34f;

    System.out.println(a + b + c + d);
  }

  public void divideByZero() {
    int a = 2;
    if (a == 0) {
      // Transformer shouldn't touch it
      int b = 9 / 0;
      System.out.println(b);
    }
  }
}
