package uwu.narumi.deobfuscator.exception;

public class DeobfuscationException extends RuntimeException {

  public DeobfuscationException() {
    super();
  }

  public DeobfuscationException(String message) {
    super(message);
  }

  public DeobfuscationException(String message, Throwable cause) {
    super(message, cause);
  }

  public DeobfuscationException(Throwable cause) {
    super(cause);
  }
}
