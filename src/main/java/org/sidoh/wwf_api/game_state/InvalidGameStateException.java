package org.sidoh.wwf_api.game_state;

public class InvalidGameStateException extends IllegalStateException {
  public InvalidGameStateException(String message, Throwable cause) {
    super(message, cause);
  }

  public InvalidGameStateException(String message) {
    super(message);
  }
}
