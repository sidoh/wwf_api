package org.sidoh.wwf_api.game_state;

import org.sidoh.wwf_api.types.game_state.Letter;
import org.sidoh.wwf_api.types.game_state.Tile;

public class TileBuilder {
  private static int idCounter = 0;

  private final int id;
  private final String value;
  private final int score;

  public TileBuilder(int id, String value, int score) {
    this.id = id;
    this.value = value;
    this.score = score;
  }

  public Tile build() {
    return new Tile()
            .setId(id)
            .setLetter(new Letter().setValue(value))
            .setValue(score);
  }

  public synchronized static Tile getTile(String letter) {
    return getTile(letter, idCounter++);
  }

  public static Tile getTile(String letter, int id) {
    Tile tile = new Tile()
      .setLetter(new Letter().setValue(letter.toUpperCase()))
      .setId(id)
      .setValue(WordsWithFriendsBoard.getLetterValue(letter));

    return tile;
  }
}


