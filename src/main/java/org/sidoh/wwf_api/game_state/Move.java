package org.sidoh.wwf_api.game_state;

import org.sidoh.wwf_api.types.game_state.Letter;
import org.sidoh.wwf_api.types.game_state.Tile;
import org.sidoh.wwf_api.types.game_state.WordOrientation;

import java.util.LinkedList;
import java.util.List;

public class Move {

  private final LinkedList<Tile> tiles;
  private final int row;
  private final int col;
  private final WordOrientation orientation;
  private Result result;

  public Move(List<Tile> tiles, int row, int col, WordOrientation orientation) {
    this.tiles = new LinkedList<Tile>(tiles);
    this.row = row;
    this.col = col;
    this.orientation = orientation;
  }

  public Move clone() {
    return new Move(tiles, row, col, orientation);
  }

  public Move moveBack() {
    int row = this.row;
    int col = this.col;

    if (orientation == WordOrientation.HORIZONTAL) {
      col--;
    }
    else {
      row--;
    }

    return new Move(tiles, row, col, orientation);
  }

  public Move moveForward() {
    int row = this.row;
    int col = this.col;

    if (orientation == WordOrientation.HORIZONTAL) {
      col++;
    }
    else {
      row++;
    }

    return new Move(tiles, row, col, orientation);
  }

  public Move playBack(Tile tile) {
    Move copy = moveBack();
    copy.tiles.addFirst(tile);

    return copy;
  }

  public Move playFront(Tile tile) {
    Move copy = new Move(tiles, row, col, orientation);
    copy.tiles.addLast(tile);

    return copy;
  }

  public Result getResult() {
    return result;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Move move = (Move) o;

    if (col != move.col) return false;
    if (row != move.row) return false;
    if (tiles != null && tiles.size() > 1 && orientation != move.orientation) return false;
    if (! tilesAreSame(tiles, move.tiles)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = tiles != null ? tilesHashCode(tiles) : 0;
    result = 31 * result + row;
    result = 31 * result + col;

    if (tiles != null && tiles.size() > 1)
      result = 31 * result + (orientation != null ? orientation.hashCode() : 0);

    return result;
  }

  @Override
  public String toString() {
    return "Move{" +
            "tiles=" + tiles +
            ", row=" + row +
            ", col=" + col +
            ", orientation=" + orientation +
            ", result=" + result +
            '}';
  }

  public List<Tile> getTiles() {
    return tiles;
  }

  public int getRow() {
    if (row < 0 || row >= 15 || col < 0 || col >= 15)
      throw new RuntimeException("illegal move generated");

    return row;
  }

  public int getCol() {
    if (row < 0 || row >= 15 || col < 0 || col >= 15)
      throw new RuntimeException("illegal move generated");

    return col;
  }

  public WordOrientation getOrientation() {
    return orientation;
  }

  public Move setResult(Result result) {
    this.result = result;
    return this;
  }

  /**
   * When checking move equality, tile IDs don't matter.
   *
   * @param tiles1
   * @param tiles2
   * @return
   */
  protected static boolean tilesAreSame(List<Tile> tiles1, List<Tile> tiles2) {
    if (tiles1 == null || tiles2 == null)
      return tiles1 == tiles2;

    if (tiles1.size() != tiles2.size())
      return false;

    for (int i = 0; i < tiles1.size(); i++) {
      Tile tile1 = tiles1.get(i);
      Tile tile2 = tiles2.get(i);

      if (! tile1.getLetter().equals(tile2.getLetter())  || tile1.getValue() != tile2.getValue())
        return false;
    }

    return true;
  }

  protected static int tilesHashCode(List<Tile> tiles1) {
    List<Letter> letters = new LinkedList<Letter>();

    for (Tile tile : tiles1) {
      letters.add(tile.getLetter());
    }

    return letters.hashCode();
  }

  /**
   * Encapsulates information about a move. This includes the number of points earned
   * and a list of the words that were formed. Removes the abstraction gained by using
   * Letter since this is really only useful for dictionary lookups.
   *
   */
  public static class Result {
    private final int score;
    private final int numTilesSkipped;
    private final String mainWord;
    private final List<String> resultingWords;

    public Result(int score, int numTilesSkipped, String mainWord, List<String> resultingWords) {
      this.score = score;
      this.numTilesSkipped = numTilesSkipped;
      this.mainWord = mainWord;
      this.resultingWords = resultingWords;
    }

    public int getNumTilesSkipped() {
      return numTilesSkipped;
    }

    public String getMainWord() {
      return mainWord;
    }

    public int getScore() {
      return score;
    }

    public List<String> getResultingWords() {
      return resultingWords;
    }

    @Override
    public String toString() {
      return "Result{" +
        "score=" + score +
        ", numTilesSkipped=" + numTilesSkipped +
        ", mainWord='" + mainWord + '\'' +
        ", resultingWords=" + resultingWords +
        '}';
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      Result result = (Result) o;

      if (numTilesSkipped != result.numTilesSkipped) return false;
      if (score != result.score) return false;
      if (mainWord != null ? !mainWord.equals(result.mainWord) : result.mainWord != null) return false;
      if (resultingWords != null ? !resultingWords.equals(result.resultingWords) : result.resultingWords != null)
        return false;

      return true;
    }

    @Override
    public int hashCode() {
      int result = score;
      result = 31 * result + numTilesSkipped;
      result = 31 * result + (mainWord != null ? mainWord.hashCode() : 0);
      result = 31 * result + (resultingWords != null ? resultingWords.hashCode() : 0);
      return result;
    }
  }
}
