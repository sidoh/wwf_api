package org.sidoh.wwf_api.game_state;

import org.apache.thrift.TException;
import org.sidoh.wwf_api.WwfApiTestCase;
import org.sidoh.wwf_api.types.api.GameState;
import org.sidoh.wwf_api.types.api.MoveData;
import org.sidoh.wwf_api.types.api.MoveType;
import org.sidoh.wwf_api.types.game_state.Letter;
import org.sidoh.wwf_api.types.game_state.SlotModifier;
import org.sidoh.wwf_api.types.game_state.Tile;
import org.sidoh.wwf_api.types.game_state.WordOrientation;

import java.io.IOException;
import java.util.Arrays;
import java.util.EnumSet;

public class TestWordsWithFriendsBoard extends WwfApiTestCase {
  public void testSetup() {
    WordsWithFriendsBoard board = new WordsWithFriendsBoard();

    assertNotNull("slot shouldn't be null", board.getSlot(0, 0));
    assertNull("tile in first slot should be null", board.getSlot(0, 0).getTile());
    assertEquals("first slot should have no modifier", SlotModifier.NONE, board.getSlot(0, 0).getModifier());
  }

  public void testSymmetry() {
    WordsWithFriendsBoard board = new WordsWithFriendsBoard();

    EnumSet<SlotModifier> seenModifiers = EnumSet.noneOf(SlotModifier.class);

    for (int i = 1; i <= 7; i++) {
      for (int j = 0; j < 15; j++) {
        assertEquals("should have vertical symmetry",
            board.getSlot(j, 7 - i).getModifier(),
            board.getSlot(j, 7 + i).getModifier());

        seenModifiers.add(board.getSlot(j, 7 - i).getModifier());
      }
    }

    assertEquals("should've seen all modifiers", EnumSet.allOf(SlotModifier.class), seenModifiers);
  }

  public void testPersistence() {
    WordsWithFriendsBoard board = new WordsWithFriendsBoard();

    board.playWord(Arrays.asList(new Tile().setId(1).setLetter(new Letter().setValue("a")).setValue(1)), 0, 0, WordOrientation.HORIZONTAL, true);

    assertNotNull("should've saved played tile", board.getSlot(0, 0).getTile());

    for (int i = 1; i < 15 * 15; i++) {
      assertNull("other slots should have no tile", board.getSlot(i).getTile());
    }
  }

  public void testPlayWord() {
    WordsWithFriendsBoard board = new WordsWithFriendsBoard();
    String word = "SIDOHTTA";

    assertResultEquals(39, playWord(board, 0, 0, word, WordOrientation.HORIZONTAL, true));

    for (int i = 0; i < word.length(); i++) {
      assertEquals("should get expected letter",
          String.valueOf(word.charAt(i)),
          board.getSlot(0, i).getTile().getLetter().getValue());
    }
  }

  public void testAdjacentWordScoring() {
    WordsWithFriendsBoard board = new WordsWithFriendsBoard();

    assertResultEquals(10, Arrays.asList("AXE"), playWord(board, 0, 0, "AXE", WordOrientation.HORIZONTAL, true));
    assertResultEquals(10, Arrays.asList("AXE"), playWord(board, 1, 0, "XE", WordOrientation.VERTICAL, true));
    assertResultEquals(18, Arrays.asList("XE", "XE"), playWord(board, 1, 1, "E", WordOrientation.VERTICAL, true));

    assertResultEquals(30, playWord(board, 14, 3, "AXE", WordOrientation.HORIZONTAL, true));
    assertResultEquals(30, playWord(board, 14, 9, "AXE", WordOrientation.HORIZONTAL, true));
    assertResultEquals(34, playWord(board, 14, 6, "AXE", WordOrientation.HORIZONTAL, true));
  }

  public void testScoreWithoutPlay() {
    WordsWithFriendsBoard board = new WordsWithFriendsBoard();

    assertResultEquals(10, playWord(board, 0, 0, "AXE", WordOrientation.HORIZONTAL, false));
    assertResultEquals(9, playWord(board, 1, 0, "XE", WordOrientation.VERTICAL, false));
    assertResultEquals(1, playWord(board, 1, 1, "E", WordOrientation.VERTICAL, true));

    assertResultEquals(30, playWord(board, 14, 3, "AXE", WordOrientation.HORIZONTAL, true));
    assertResultEquals(30, playWord(board, 14, 9, "AXE", WordOrientation.HORIZONTAL, true));
    assertResultEquals(34, playWord(board, 14, 6, "AXE", WordOrientation.HORIZONTAL, false));
    assertResultEquals(34, playWord(board, 14, 6, "AXE", WordOrientation.HORIZONTAL, true));
  }

  public void testHook() {
    WordsWithFriendsBoard board = new WordsWithFriendsBoard();

    assertResultEquals(30, playWord(board, 7, 7, "GOBBLE", WordOrientation.HORIZONTAL, true));
    Move.Result hookResult = playWord(board, 6, 8, "LAD", WordOrientation.VERTICAL, true);

    assertEquals("should only form one word", 1, hookResult.getResultingWords().size());
    assertEquals("formed word should be LOAD", "LOAD", hookResult.getResultingWords().get(0));

    assertResultEquals(6, hookResult);
  }

  public void testAllLettersScoring() {
    WordsWithFriendsBoard board = new WordsWithFriendsBoard();

    assertResultEquals(59, playWord(board, 7, 7, "NAIVEST", WordOrientation.HORIZONTAL, true));
  }

  public void testGameStateScoring1() throws IOException, TException {
    assertGameStateHasConsistentScores("4673715966.bin");
  }

  public void testGameStateScoring2() throws IOException, TException {
    assertGameStateHasConsistentScores("4683202983.bin");
  }

  public void testGameStateScoring3() throws IOException, TException {
    assertGameStateHasConsistentScores("4685541559.bin");
  }

  protected void assertGameStateHasConsistentScores(String stateFile) throws IOException, TException {
    GameState state = loadGameState(stateFile);
    WordsWithFriendsBoard board = new WordsWithFriendsBoard();

    for (MoveData moveData : state.getAllMoves()) {

      if (moveData.getMoveType() == MoveType.PLAY) {
        Move move = stateHelper.buildGameStateMove(moveData, board);
        board.move(move);

        assertEquals("computed score and expected score should match",
            moveData.getPoints(),
            move.getResult().getScore());
      }
    }
  }
}
