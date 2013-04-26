package org.sidoh.wwf_api.parser;

public class ParserException extends RuntimeException {
  public ParserException() {
  }

  public ParserException(String s) {
    super(s);
  }

  public ParserException(String s, Throwable throwable) {
    super(s, throwable);
  }

  public ParserException(Throwable throwable) {
    super(throwable);
  }
}
