package uwu.narumi.deobfuscator.pool;

public class ConstantPool {

  private final int[] ints;

  public ConstantPool(int[] ints) {
    this.ints = ints;
  }

  public int getIntAt(int position) {
    if (position + 1 > ints.length) {
      throw new IllegalArgumentException();
    }

    return ints[position];
  }

  public int getSize() {
    return ints.length;
  }
}
