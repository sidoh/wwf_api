package org.sidoh.wwf_api;

import com.google.common.collect.Lists;
import org.sidoh.wwf_api.game_state.TileBuilder;
import org.sidoh.wwf_api.game_state.WordsWithFriendsBoard;
import org.sidoh.wwf_api.types.game_state.Letter;
import org.sidoh.wwf_api.types.game_state.Tile;
import org.sidoh.wwf_api.util.MersenneTwister;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Implements WWF's bag functionality. This is how game state is determined. A bag
 * is initialized with a random number seed, and popped off of accordingly.
 *
 */
public class Bag {
  /**
   * This is the order that WWF puts tiles in. *'s are blanks.
   */
  private static final List<String> UNSHUFFLED_BAG
    = Arrays.asList(
      "*","*","e","e","e","e","e","e","e","e",
      "e","e","e","e","e","a","a","a","a","a",
      "a","a","a","a","i","i","i","i","i","i",
      "i","i","o","o","o","o","o","o","o","o",
      "n","n","n","n","n","r","r","r","r","r",
      "r","t","t","t","t","t","t","t","d","d",
      "d","d","d","l","l","l","l","s","s","s",
      "s","s","u","u","u","u","g","g","g","b",
      "b","c","c","f","f","h","h","h","h","m",
      "m","p","p","v","v","w","w","y","y","j",
      "k","q","x","z"
    );

  private final List<Tile> tiles;
  private final MersenneTwister twister;
  private final long seed;

  /**
   *
   * @param seed
   */
  public Bag(long seed) {
    this(seed, buildUnshuffledBag());
  }

  /**
   *
   * @param seed
   * @param tiles ordered list of tiles - may disclude already played tiles
   */
  protected Bag(long seed, List<Tile> tiles) {
    this( seed, new MersenneTwister(seed), tiles );
  }

  /**
   *
   * @param seed seed used for PRNG
   * @param twister instance of PRNG (might have already been queried)
   * @param tiles ordered list of tiles - may disclude already played tiles
   */
  protected Bag(long seed, MersenneTwister twister, List<Tile> tiles) {
    this.twister = twister;
    this.tiles = tiles;
    this.seed = seed;
  }

  /**
   * Remove and return a provided number of tiles
   *
   * @param count the number of tiles to remove
   * @return
   */
  public List<Tile> pullTiles(int count) {
    List<Tile> tiles = new ArrayList<Tile>();

    for (int i = 0; i < count; i++) {
      tiles.add(pullTile());
    }

    return tiles;
  }

  /**
   * Remove and return a single tile from the bag
   *
   * @return the removed tile
   */
  public Tile pullTile() {
    return tiles.remove((int) (twister.nextUnsignedI32() % tiles.size()));
  }

  /**
   * Return a single tile to the bag
   *
   * @param tile
   */
  public void returnTile(Tile tile) {
    tiles.add(tile);
  }

  /**
   * Return the provided tiles to the bag
   *
   * @param tiles
   */
  public void returnTiles(Iterable<Tile> tiles) {
    for (Tile tile : tiles) {
      returnTile(tile);
    }
  }

  /**
   *
   * @return true iff there are still tiles
   */
  public boolean tilesLeft() {
    return ! tiles.isEmpty();
  }

  /**
   * Returns a list of the remaining tiles in no particular order.
   *
   * @return
   */
  public List<Tile> getRemainingTiles() {
    return Lists.newArrayList(tiles);
  }

  /**
   * Returns list of tiles in the order they'll be pulled off of (assuming none are returned)
   *
   * @return
   */
  public List<Tile> getRemainingTilesInPullOrder() {
    Bag copy = new Bag(seed, (MersenneTwister)twister.clone(), Lists.newArrayList(tiles));

    return copy.pullTiles(tiles.size());
  }

  /**
   * Fetch the Tile with the provided id.
   *
   * @param id
   * @return
   */
  public static Tile getTileWithId(int id) {
    String letterValue = UNSHUFFLED_BAG.get(id);

    return TileBuilder.getTile(letterValue, id);
  }

  /**
   * WWF tiles have IDs determined by the order specified in UNSHUFFLED_BAG. This builds Tile
   * instances and assigned the appropriate IDs.
   *
   * @return
   */
  private static List<Tile> buildUnshuffledBag() {
    List<Tile> tiles = new ArrayList<Tile>();

    for (int i = 0; i < UNSHUFFLED_BAG.size(); i++) {
      tiles.add(getTileWithId(i));
    }

    return tiles;
  }

  private static Tile buildTile(String letter, int id) {
    Tile tile = new Tile()
      .setLetter(new Letter().setValue(letter.toUpperCase()))
      .setId(id)
      .setValue(WordsWithFriendsBoard.getLetterValue(letter));

    return tile;
  }

  @Override
  public String toString() {
    StringBuilder bag = new StringBuilder("[");
    for (Tile tile : getRemainingTilesInPullOrder()) {
      bag.append(" ");
      bag.append(tile.getLetter().getValue());
    }
    bag.append(" ]");
    return bag.toString();
  }
}
