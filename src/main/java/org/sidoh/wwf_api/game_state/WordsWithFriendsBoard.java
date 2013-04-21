package org.sidoh.wwf_api.game_state;

import org.sidoh.wwf_api.types.api.MoveType;
import org.sidoh.wwf_api.types.game_state.BoardStorage;
import org.sidoh.wwf_api.types.game_state.Slot;
import org.sidoh.wwf_api.types.game_state.SlotModifier;
import org.sidoh.wwf_api.types.game_state.Tile;
import org.sidoh.wwf_api.types.game_state.WordOrientation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Implements the logic and layout of the WWF play board.
 */
public class WordsWithFriendsBoard extends Board implements Cloneable {

  protected static class Builder extends Board.Builder {
    @Override
    public Board build() {
      return new WordsWithFriendsBoard();
    }

    @Override
    public Board build(BoardStorage storage) {
      return new WordsWithFriendsBoard(storage);
    }
  }

  /**
   * Size of the board
   */
  public static final int DIMENSIONS = 15;

  /**
   * Tile/word modifiers. Legend:
   * - N : none
   * - T : triple word
   * - t : triple letter
   * - D : double word
   * - d : double letter
   */
  private static final Iterable<SlotBuilder> SLOTS = Board.getSlotBuilders(
      "NNNTNNtNtNNTNNN"
          + "NNdNNDNNNDNNdNN"
          + "NdNNdNNNNNdNNdN"
          + "TNNtNNNDNNNtNNT"
          + "NNdNNNdNdNNNdNN"
          + "NDNNNtNNNtNNNDN"
          + "tNNNdNNNNNdNNNt"
          + "NNNDNNNNNNNDNNN"
          + "tNNNdNNNNNdNNNt"
          + "NDNNNtNNNtNNNDN"
          + "NNdNNNdNdNNNdNN"
          + "TNNtNNNDNNNtNNT"
          + "NdNNdNNNNNdNNdN"
          + "NNdNNDNNNDNNdNN"
          + "NNNTNNtNtNNTNNN");

  /**
   * Character used to represent a blank tile
   */
  public static final String BLANK_LETTER = "*";

  /**
   * The tiles in the bag at the start of a game. The first argument is the number of points that
   * tile is worth. The second is the number of those tiles in the bag.
   */
  public static final List<TileBuilder> TILES = new TileBagBuilder()
      .addLetter("A", 1, 9).addLetter("B", 4, 2).addLetter("C", 4, 2).addLetter("D", 2, 5).addLetter("E", 1, 13)
      .addLetter("F", 4, 2).addLetter("G", 3, 3).addLetter("H", 3, 4).addLetter("I", 1, 8).addLetter("J", 10, 1)
      .addLetter("K", 5, 1).addLetter("L", 2, 4).addLetter("M", 4, 2).addLetter("N", 2, 5).addLetter("O", 1, 8)
      .addLetter("P", 4, 2).addLetter("Q", 10, 1).addLetter("R", 1, 6).addLetter("S", 1, 5).addLetter("T", 1, 7)
      .addLetter("U", 2, 4).addLetter("V", 5, 2).addLetter("W", 4, 2).addLetter("X", 8, 1).addLetter("Y", 3, 2)
      .addLetter("Z", 10, 1).addLetter(BLANK_LETTER, 0, 2)
      .getTiles();

  /**
   * A map going from tile character -> # of points that tile is worth
   */
  public static final Map<Character, Integer> TILE_VALUES = new HashMap<Character, Integer>() {{
    for (TileBuilder tileBuilder : TILES) {
      Tile tile = tileBuilder.build();
      put(tile.getLetter().getValue().charAt(0), tile.getValue());
    }
  }};

  /**
   * Number of tiles each player has
   */
  public static final int TILES_PER_PLAYER = 7;

  /**
   * Bonus applied when all tiles are used
   */
  public static final int ALL_TILES_BONUS = 35;

  /**
   * Use default values.
   */
  public WordsWithFriendsBoard() {
    super(DIMENSIONS, SLOTS);
  }

  /**
   * @param storage the board
   */
  public WordsWithFriendsBoard(BoardStorage storage) {
    super(storage);

    if (storage.getSlotsSize() != DIMENSIONS * DIMENSIONS)
      throw new InvalidGameStateException("Unepxected board dimensions");
  }

  /**
   * Get the slots adjacent to the specified slot
   *
   * @param row
   * @param column
   * @return
   */
  public AdjacentSlots getAdjacentSlots(int row, int column) {
    AdjacentSlots slots = AdjacentSlots.adjacentTo(getSlot(row, column));

    if (row > 0) {
      slots.setTopMiddle(getSlot(row - 1, column));

      if (column > 0) {
        slots.setLeft(getSlot(row, column - 1));
        slots.setTopLeft(getSlot(row - 1, column - 1));
      }
      if (column < DIMENSIONS - 1) {
        slots.setRight(getSlot(row, column + 1));
        slots.setTopRight(getSlot(row - 1, column + 1));
      }
    }
    if (row < DIMENSIONS - 1) {
      slots.setBottomMiddle(getSlot(row + 1, column));

      if (column > 0) {
        slots.setLeft(getSlot(row, column - 1));
        slots.setBottomLeft(getSlot(row + 1, column - 1));
      }
      if (column < DIMENSIONS - 1) {
        slots.setRight(getSlot(row, column + 1));
        slots.setBottomRight(getSlot(row + 1, column + 1));
      }
    }

    return slots;
  }

  /**
   * Get the slots adjacent to the specified slot.
   *
   * @param index the index of the tile. row and column are determined mathematically from the index
   * @return
   */
  public AdjacentSlots getAdjacentSlots(int index) {
    return getAdjacentSlots(getRowFromIndex(index), getColFromIndex(index));
  }

  /**
   * @return
   */
  @Override
  public WordsWithFriendsBoard clone() {
    return new WordsWithFriendsBoard(storage.deepCopy());
  }

  /**
   * Return the slot located at the provided location
   *
   * @param row
   * @param column
   * @return
   */
  public Slot getSlot(int row, int column) {
    return getSlot(getIndexFromRowAndCol(row, column));
  }

  /**
   * Return the slot located at the provided location
   *
   * @param index
   * @return
   */
  @Override
  public Slot getSlot(int index) {
    return super.getSlot(index);
  }

  /**
   * Makes and scores move
   *
   * @param move
   * @return
   */
  public Move.Result move(Move move) {
    if (move.getMoveType() == MoveType.PLAY) {
      Move.Result result = playWord(move.getTiles(),
          move.getRow(),
          move.getCol(),
          move.getOrientation(),
          true);
      move.setResult(result);
      return result;
    } else if (move.getMoveType() == MoveType.SWAP) {
      Move.Result result = new Move.Result(0, 0, null, null);
      move.setResult(result);
      return result;
    }

    throw new RuntimeException("Unsupported move type: " + move.getMoveType());
  }

  /**
   * Scores a move without actually making it
   *
   * @param move
   * @return
   */
  public Move.Result scoreMove(Move move) {
    Move.Result result = playWord(move.getTiles(),
        move.getRow(),
        move.getCol(),
        move.getOrientation(),
        false);
    move.setResult(result);

    return result;
  }

  /**
   * Play a word on the board. No validiation is done to determine whether or not the play is legal
   * or if the resulting words are in a particular dicitonary.
   *
   * @param _tiles         the tiles to play
   * @param firstLetterRow the row the first tile is played in
   * @param firstLetterCol the column the first tile is played in
   * @param orientation    orientation of the play (either vertical or horizontal)
   * @param placeTiles     if true, modify the board state
   * @return the result of the move. includes score, resulting words, etc.
   */
  protected Move.Result playWord(List<Tile> _tiles, int firstLetterRow, int firstLetterCol, WordOrientation orientation, boolean placeTiles) {
    int score = 0;
    int adjacentWordsScore = 0;
    List<SlotModifier> wordModifiers = new LinkedList<SlotModifier>();
    LinkedList<Tile> tiles = new LinkedList<Tile>(_tiles);

    // Count the number of tiles we skip over because they're already filled.
    int skippedSlots = 0;

    String mainWord = "";
    List<String> adjacentWords = new ArrayList<String>();

    SlotIterator itr = new SlotIterator(firstLetterRow, firstLetterCol, DIMENSIONS, DIMENSIONS, orientation, Direction.FORWARDS);

    for (Integer index : itr) {
      Slot slot = getSlot(index);

      // If there's already a tile here, don't pop tiles off.
      // If there are no more letters, we're trying to count a suffix. Only worth doing if
      // there are letters in this slot
      if (slot.getTile() != null) {
        // Don't count modifiers for suffixes
        score += slot.getTile().getValue();

        // Add letter to word being formed
        mainWord = mainWord.concat(slot.getTile().getLetter().getValue());

        // Only count skipped slots if there are still things left to play
        if (!tiles.isEmpty())
          skippedSlots++;
      }
      // If there are still letters, play them.
      else if (!tiles.isEmpty()) {
        Tile tile = tiles.removeFirst();

        // Add this letter to the word being formed (do it here in case we're not allowed to set)
        mainWord = mainWord.concat(tile.getLetter().getValue());

        if (placeTiles)
          slot.setTile(tile);

        // Remember any word modifiers if we placed a tile
        if (isWordModifier(slot.getModifier()))
          wordModifiers.add(slot.getModifier());

        // Score adjacent words
        Move.Result result = scoreAdjacentWord(index, tile, opposite(orientation));

        // Sanity check
        if (result.getResultingWords().size() > 1)
          throw new RuntimeException("# adjacent words > 1. this should never happen.");

        if (result.getScore() > 0) {
          adjacentWordsScore += result.getScore();
          adjacentWords.add(result.getResultingWords().get(0));
        }

        // Score this letter (could be modified by letter modifier)
        score += getLetterScore(slot, tile);
      }
      // Don't continue after getting to the end of the word
      else {
        break;
      }
    }

    // Include scores from the prefix.
    Iterator<Integer> prefixIterator = itr.reverse().iterator();

    // There's always at least one value since we're starting at the first position of this word.
    // We want to consider the first slot before that one.
    prefixIterator.next();

    while (prefixIterator.hasNext()) {
      Slot slot = getSlot(prefixIterator.next());

      if (slot.getTile() != null) {
        score += slot.getTile().getValue();

        // Prefix word being formed
        mainWord = slot.getTile().getLetter().getValue().concat(mainWord);
      }
      // finish if this slot doesn't have a tile
      else {
        break;
      }
    }

    // Apply word modifiers
    score = getWordScore(score, wordModifiers);

    // Apply bonus if all tiles were used.
    if (_tiles.size() == TILES_PER_PLAYER)
      score += ALL_TILES_BONUS;

    // Ready list of formed words
    adjacentWords.add(mainWord);

    return new Move.Result(score + adjacentWordsScore, skippedSlots, mainWord, adjacentWords);
  }

  /**
   * <p>Scores words adjacent to a play. Example:</p>
   * <p/>
   * <pre>
   *          B O P
   *        G A M E R
   * </pre>
   * <p/>
   * <p>
   * Here, the play "BOP" off of the word "GAMER" includes adjacent words "BA", "OM", and "PE",
   * all of which should be included in the score.
   * </p>
   * <p/>
   * <p>
   * Strictly speaking, using Move.Result is overkill, but it's convenient.
   * </p>
   *
   * @param index
   * @param tile
   * @param orientation
   * @return
   */
  private Move.Result scoreAdjacentWord(Integer index, Tile tile, WordOrientation orientation) {
    // Don't bother if there aren't tiles in either direction
    if (!hasAdjacentTiles(index, orientation))
      return new Move.Result(0, 0, null, Collections.<String>emptyList());

    Slot centerSlot = getSlot(index);

    String word = tile.getLetter().getValue();
    int score = getLetterScore(centerSlot, tile);

    List<SlotModifier> wordModifiers = isWordModifier(centerSlot.getModifier())
        ? Collections.singletonList(centerSlot.getModifier())
        : Collections.<SlotModifier>emptyList();

    SlotIterator centerSlotStart = new SlotIterator(index, DIMENSIONS, DIMENSIONS, orientation, Direction.FORWARDS);
    SlotIterator.Iterator itr = centerSlotStart.iterator();

    // should always have next - skip current tile
    itr.next();

    while (itr.hasNext()) {
      Slot slot = getSlot(itr.next());

      if (slot.getTile() != null)
        score += slot.getTile().getValue();
      else break;

      word = word.concat(slot.getTile().getLetter().getValue());
    }

    // reverse direction
    itr = centerSlotStart.reverse().iterator();
    itr.next();

    while (itr.hasNext()) {
      Slot slot = getSlot(itr.next());

      if (slot.getTile() != null)
        score += slot.getTile().getValue();
      else break;

      word = slot.getTile().getLetter().getValue().concat(word);
    }

    return new Move.Result(
        getWordScore(score, wordModifiers),
        0,    //    don't really care about main words or skipped letters for adjacent words
        null, // --^
        Collections.singletonList(word));
  }

  /**
   * Convenience method used to determine if a provided location has tiles placed next to it in
   * a particular orientation (either vertical or horizontal)
   *
   * @param row         (of tile)
   * @param col         (of tile)
   * @param orientation vertical/horizontal
   * @return true if the provided location has any tiles next to it
   */
  protected boolean hasAdjacentTiles(int row, int col, WordOrientation orientation) {

    if (orientation == WordOrientation.HORIZONTAL) {
      return (col - 1 >= 0 && getSlot(row, col - 1).getTile() != null)
          || (col + 1 < DIMENSIONS && getSlot(row, col + 1).getTile() != null);
    } else {
      return (row - 1 >= 0 && getSlot(row - 1, col).getTile() != null)
          || (row + 1 < DIMENSIONS && getSlot(row + 1, col).getTile() != null);
    }
  }

  protected boolean hasAdjacentTiles(Integer index, WordOrientation orientation) {
    return hasAdjacentTiles(getRowFromIndex(index), getColFromIndex(index), orientation);
  }


  /**
   * Apply word modifiers to a base score to determine the overall score.
   *
   * @param baseScore score based only on tile scores (this includes non-word modifiers)
   * @param modifiers list of word modifiers applying to a word
   * @return score after modifiers are applied
   */
  protected static int getWordScore(int baseScore, List<SlotModifier> modifiers) {
    for (SlotModifier modifier : modifiers) {
      if (modifier == SlotModifier.DOUBLE_WORD)
        baseScore *= 2;
      else if (modifier == SlotModifier.TRIPLE_WORD)
        baseScore *= 3;
      else throw new RuntimeException("non-word modifier in word modifier list: " + modifier);
    }

    return baseScore;
  }

  /**
   * Compute the score attributable to a single tile in a play, not counting word modifiers.
   *
   * @param slot the slot the tile is to be placed in
   * @param tile the tile to be played
   * @return score earned by the tile
   */
  protected static int getLetterScore(Slot slot, Tile tile) {
    int score = tile.getValue();

    if (slot.getModifier() == SlotModifier.DOUBLE_LETTER)
      score *= 2;
    else if (slot.getModifier() == SlotModifier.TRIPLE_LETTER)
      score *= 3;

    return score;
  }

  /**
   * @param mod modifier in question
   * @return true if the provided modifier is a word modifier
   */
  protected static boolean isWordModifier(SlotModifier mod) {
    return mod == SlotModifier.TRIPLE_WORD || mod == SlotModifier.DOUBLE_WORD;
  }

  /**
   * Convenience method for swapping horizontal/vertical. Since WordOrientation is a thrift type,
   * we can't add this method there.
   *
   * @param o
   * @return
   */
  protected static WordOrientation opposite(WordOrientation o) {
    if (o == WordOrientation.HORIZONTAL) {
      return WordOrientation.VERTICAL;
    } else {
      return WordOrientation.HORIZONTAL;
    }
  }

  /**
   * @param index a location on the board
   * @return the row the location refers to
   */
  public static int getRowFromIndex(int index) {
    return index / DIMENSIONS;
  }

  /**
   * @param index a location on the board
   * @return the column the location referrs to
   */
  public static int getColFromIndex(int index) {
    return index % DIMENSIONS;
  }

  /**
   * @param row
   * @param col
   * @return the index of the specified location
   */
  protected static int getIndexFromRowAndCol(int row, int col) {
    return DIMENSIONS * row + col;
  }

  /**
   * Returns the value of the provided tile. For instance, A -> 1.
   *
   * @param letter
   * @return
   */
  public static int getLetterValue(String letter) {
    return TILE_VALUES.get(letter.toUpperCase().charAt(0));
  }

  /**
   * @return a printable representation of the board
   */
  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();

    for (int row = 0; row < DIMENSIONS; row++) {
      for (int col = 0; col < DIMENSIONS; col++) {
        Slot slot = getSlot(row, col);

        builder.append(slot.getTile() == null ? "." : slot.getTile().getLetter().getValue());
        builder.append(" ");
      }

      builder.append("\n");
    }

    return builder.toString();
  }
}
