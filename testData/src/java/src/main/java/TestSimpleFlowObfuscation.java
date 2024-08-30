public class TestSimpleFlowObfuscation {
  public void testFlow() {
    int a = 1;
    if (a == 1) {
      a = 2;
      if (a == 2) {
        a = 3;
        if (System.currentTimeMillis() == 123) {
          a = 4;
          if (a == 4) {
            a = 5;
            if (a == 5) {
              a = 6;
              if (a == 6) {
                System.out.println("123");
              } else {
                throw new RuntimeException();
              }
            } else {
              throw new RuntimeException();
            }
          } else {
            throw new RuntimeException();
          }
        }
      } else {
        throw new RuntimeException();
      }
    } else {
      throw new RuntimeException();
    }
  }

  public void compareTest() {
    int a = 123;

    if (a == 100) {
      System.out.println("a is 100");
    } else {
      System.out.println("a is not 100");
    }

    while (a * 321 == 100) {
      a += 1;
    }
  }

  public void switches() {
    // LOOKUPSWITCH
    int b = 100;
    switch (b) {
      case 100:
        System.out.println("REACHABLE 100");
        break;
      case 200:
        System.out.println("unreachable 200");
        break;
      default:
        System.out.println("unreachable default");
        break;
    }

    // TABLESWITCH
    int c = 3;
    switch (c) {
      case 1:
        System.out.println("unreachable 1");
        break;
      case 2:
        System.out.println("unreachable 2");
        break;
      case 3:
        System.out.println("REACHABLE 3");
        break;
      case 4:
        System.out.println("unreachable 4");
        break;
      case 5:
        System.out.println("unreachable 5");
        break;
      default:
        System.out.println("unreachable default");
        break;
    }

    // TODO: Simplify "equals" calls (in UniversalNumberTransformer)
    String d = "test5";
    switch (d) {
      case "test":
        System.out.println("unreachable test");
        break;
      case "test2":
        System.out.println("unreachable test2");
        break;
      case "test3":
        System.out.println("unreachable test3");
        break;
      case "test4":
        System.out.println("unreachable test4");
        break;
      case "test5":
        System.out.println("REACHABLE test5");
        break;
      default:
        System.out.println("unreachable default");
        break;
    }
  }
}
