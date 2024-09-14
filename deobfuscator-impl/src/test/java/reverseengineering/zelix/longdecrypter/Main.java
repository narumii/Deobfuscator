package reverseengineering.zelix.longdecrypter;

import java.lang.invoke.MethodHandles;

public class Main {
  // Usage
  // LongDecrypter2 instances
  private static final long a1 = LongDecrypter1.buildNumberDecryptor(900058104405336414L, 6106449219005125011L, MethodHandles.lookup().lookupClass()).decrypt(99062861074978L);
  private static final long a2 = LongDecrypter1.buildNumberDecryptor(5832394289974403481L, -8943439614781261032L, MethodHandles.lookup().lookupClass()).decrypt(19597665297729L);
  private static final long a3 = LongDecrypter1.buildNumberDecryptor(-1563944528177415659L, 8240211990857304620L, MethodHandles.lookup().lookupClass()).decrypt(224919788586450L);

  // LongDecrypter1 instances
//  private static final long b1 = LongDecrypter1.buildNumberDecryptor(-2891110000934166428L, 4534565905501632758L, MethodHandles.lookup().lookupClass()).decrypt(77751647876842L);
//  private static final long b2 = LongDecrypter1.buildNumberDecryptor(3109756102241299096L, 3300563161622516573L, MethodHandles.lookup().lookupClass()).decrypt(3703773754795L);

  public static void main(String[] args) {
    System.out.println(a1); // 110160429747013
    System.out.println(a2); // 119662894797887
    System.out.println(a3); // 62547565276859

    //System.out.println(b1); // 97375689351077
    //System.out.println(b2); // 92083991818967

    ILongDecrypter b1Instance = LongDecrypter1.buildNumberDecryptor(-2891110000934166428L, 4534565905501632758L, MethodHandles.lookup().lookupClass());
    ILongDecrypter b2Instance = LongDecrypter1.buildNumberDecryptor(3109756102241299096L, 3300563161622516573L, MethodHandles.lookup().lookupClass());
    System.out.println(b1Instance);
    System.out.println(b2Instance);
    long b1 = b1Instance.decrypt(77751647876842L);
    long b2 = b2Instance.decrypt(3703773754795L);
    System.out.println(b1);
    System.out.println(b2);
  }
}
