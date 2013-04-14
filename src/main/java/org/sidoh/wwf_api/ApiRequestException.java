package org.sidoh.wwf_api;

public class ApiRequestException extends RuntimeException {
  public ApiRequestException(Throwable throwable) {
    super(throwable);
  }

  public ApiRequestException(String s, Throwable throwable) {
    super(s, throwable);
  }

  public ApiRequestException(String s) {
    super(s);
  }
}
