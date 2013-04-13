package org.sidoh.wwf_api;

import junit.framework.TestCase;

import org.apache.thrift.TDeserializer;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TJSONProtocol;
import org.sidoh.wwf_api.game_state.GameStateHelper;
import org.sidoh.wwf_api.game_state.Move;
import org.sidoh.wwf_api.game_state.TileBuilder;
import org.sidoh.wwf_api.game_state.WordsWithFriendsBoard;
import org.sidoh.wwf_api.types.api.GameState;
import org.sidoh.wwf_api.types.game_state.Letter;
import org.sidoh.wwf_api.types.game_state.Rack;
import org.sidoh.wwf_api.types.game_state.Tile;
import org.sidoh.wwf_api.types.game_state.WordOrientation;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class WwfApiTestCase extends TestCase {
  private static int tileId = 0;

  protected static final GameStateHelper stateHelper = new GameStateHelper();

  public static void assertResultEquals(int score, List<String> words, Move.Result result) {
    assertEquals("score should match", score, result.getScore());
    assertEquals("words should match", words, result.getResultingWords());
  }

  public static void assertResultEquals(int score, Move.Result result) {
    assertEquals("score should match", score, result.getScore());
  }

  public static Move.Result playInitialWord(WordsWithFriendsBoard board, String word, WordOrientation orientation) {
    return playWord(board, 7, 7, word, orientation, true);
  }

  public static Move.Result playWord(WordsWithFriendsBoard board, int row, int col, String word, WordOrientation orientation, boolean keep) {
    List<Tile> letters = new ArrayList<Tile>();

    for (int i = 0; i < word.length(); i++) {
      Character value = word.charAt(i);

      letters.add(new Tile()
              .setId(i)
              .setLetter(new Letter().setValue(String.valueOf(value)))
              .setValue(WordsWithFriendsBoard.TILE_VALUES.get(value)));
    }

    Move.Result result;

    if (keep) {
      result = board.move(Move.play(letters, row, col, orientation));
    }
    else {
      result = board.scoreMove(Move.play(letters, row, col, orientation));
    }

    return result;
  }

  /**
   * Reads a JSON game state from resources and applies it to a game state.
   *
   * @param filename
   * @return
   * @throws IOException
   * @throws TException
   */
  public static GameState loadGameState(String filename) throws IOException, TException {
    FileReader stream = new FileReader(String.format("src/resources/game_states/%s", filename));
    TDeserializer deserializer = new TDeserializer(new TJSONProtocol.Factory());
    GameState state = new GameState();

    BufferedReader reader = new BufferedReader(stream);
    String line = reader.readLine();
    String full = "";

    while (line != null) {
      full += line;
      line = reader.readLine();
    }

    deserializer.deserialize(state, full, "UTF-8");

    return state;
  }

  public static Rack buildRack(String letters) {
    Rack rack = new Rack().setCapacity(7);

    for (int i = 0; i < letters.length(); i++) {
      char letter = letters.charAt(i);
      int value = WordsWithFriendsBoard.TILE_VALUES.get(letter);

      rack.addToTiles(new Tile().setId(tileId++).setLetter(new Letter().setValue(String.valueOf(letter))).setValue(value));
    }

    return rack;
  }

  public static WordsWithFriendsBoard parseCsvBoard(String csv) {
    WordsWithFriendsBoard board = new WordsWithFriendsBoard();
    String[] letters = csv.split(",\\s*");

    for (int i = 0; i < letters.length; i++) {
      if ("null".equals(letters[i])) continue;

      board.getSlot(i).setTile( TileBuilder.getTile(letters[i]) );
    }

    return board;
  }
}
