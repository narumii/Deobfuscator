public class TestInlineStaticFields {
  public static int TEST1 = 123;
  public static String TEST2 = "placki";
  public static boolean TEST3 = true;

  public static void test() {
    System.out.println(TEST1);
    System.out.println(TEST2);
    System.out.println(TEST3);
  }

  public static void modifyStatic() {
    // TODO: Account for field modification
    TEST1 = 321;
  }
}
