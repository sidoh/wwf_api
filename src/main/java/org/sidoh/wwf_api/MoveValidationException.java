package org.sidoh.wwf_api;

public class MoveValidationException extends RuntimeException {
  public MoveValidationException() {
    super();
  }

  public MoveValidationException(String s) {
    super(s);
  }

  public MoveValidationException(String s, Throwable throwable) {
    super(s, throwable);
  }

  public MoveValidationException(Throwable throwable) {
    super(throwable);
  }
}
