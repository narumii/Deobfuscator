public class TestInlineStaticFields {
  public static int TEST1 = 123;
  public static String TEST2 = "placki";
  public static boolean TEST3 = true;
  public static int TEST4;
  public static String TEST5;

  static {
    try {
      TEST5 = Number.class.getSimpleName();
    } catch (Exception e) {
      TEST5 = null;
    }
  }

  public static void test() {
    System.out.println(TEST1);
    System.out.println(TEST2);
    System.out.println(TEST3);
    System.out.println(TEST4);

    System.out.println(TEST5.toUpperCase());
  }

  public static void modifyStatic() {
    TEST1 = 321;
  }
}
