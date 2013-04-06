package org.sidoh.wwf_api.game_state;

public enum Direction {
  FORWARDS(1),
  BACKWARDS(-1);

  private final int moveValue;

  private Direction(int moveValue) {
    this.moveValue = moveValue;
  }

  public int getMoveValue() {
    return moveValue;
  }
}
