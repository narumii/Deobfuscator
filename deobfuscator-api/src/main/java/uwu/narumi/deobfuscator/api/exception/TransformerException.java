package uwu.narumi.deobfuscator.api.exception;

public class TransformerException extends RuntimeException {

  public TransformerException() {}

  public TransformerException(String message) {
    super(message);
  }

  public TransformerException(String message, Throwable cause) {
    super(message, cause);
  }

  public TransformerException(Throwable cause) {
    super(cause);
  }

  public TransformerException(
      String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
