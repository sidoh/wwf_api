package org.sidoh.wwf_api.game_state;

import org.sidoh.wwf_api.types.api.*;
import org.sidoh.wwf_api.types.game_state.Rack;
import org.sidoh.wwf_api.types.game_state.Slot;
import org.sidoh.wwf_api.types.game_state.Tile;
import org.sidoh.wwf_api.types.game_state.WordOrientation;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public class GameStateHelper {
  public static final SimpleDateFormat TIMESTAMP_DATE_FORMAT
    = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ'+00:00'", Locale.GERMANY);

  /**
   * Gets the score status for a GameState. This indicates whether the provided player is winning,
   * losing, or the game is tied.
   *
   * @param user
   * @param state
   * @return
   */
  public GameScoreStatus getScoreStatus(User user, GameState state) {
    int myScore = getScore(user, state);
    int otherScore = getScore(getOtherUser(user, state), state);

    if ( myScore == otherScore ) {
      return GameScoreStatus.TIED;
    }
    else if ( myScore < otherScore ) {
      return GameScoreStatus.LOSING;
    }
    else {
      return GameScoreStatus.WINNING;
    }
  }

  /**
   * Gets the score status for a GameState. This indicates whether the provided player is winning,
   * losing, or the game is tied.
   *
   * @param userId
   * @param state
   * @return
   */
  public GameScoreStatus getScoreStatus(long userId, GameState state) {
    return getScoreStatus(state.getMeta().getUsersById().get(userId), state);
  }

  /**
   * Returns the rack of the player currently allowed to move.
   *
   * @param state
   * @return
   */
  public Rack getCurrentPlayerRack(GameState state) {
    return new Rack().setTiles(state.getRacks().get(state.getMeta().getCurrentMoveUserId()));
  }

  /**
   * Returns the rack of the player currently awaiting their turn.
   *
   * @param state
   * @return
   */
  public Rack getOtherPlayerRack(GameState state) {
    User otherUser = getOtherUser(state.getMeta().getCurrentMoveUserId(), state);

    return new Rack().setTiles(state.getRacks().get(otherUser.getId()));
  }

  /**
   * Given one user in the game, return the other one. If the game only has one user,
   * return null.
   *
   * @param user
   * @param state
   * @return
   */
  public User getOtherUser(User user, GameState state) {
    return getOtherUser(user.getId(), state);
  }

  /**
   * Given the ID of one user in the game, return the other one. If the game only has one user,
   * return null.
   *
   * @param uid
   * @param state
   * @return
   */
  public User getOtherUser(long uid, GameState state) {
    return getOtherUser(uid, state.getMeta());
  }

  /**
   * Given the ID of one user in the game, return the other one. If the game only has one user,
   * return null.
   *
   * @param user
   * @param meta
   * @return
   */
  public User getOtherUser(User user, GameMeta meta) {
    return getOtherUser(user.getId(), meta);
  }

  /**
   * Given the ID of one user in the game, return the other one. If the game only has one user,
   * return null.
   *
   * @param uid
   * @param meta
   * @return
   */
  public User getOtherUser(long uid, GameMeta meta) {
    for (User user : meta.getUsersById().values()) {
      if (user.getId() != uid)
        return user;
    }

    return null;
  }

  /**
   * Create a MoveSubmission (used by the WWF API) given a game state move
   *
   * @param move
   * @return
   */
  public MoveSubmission createMoveSubmissionFromPlay(Move move) {
    return createMoveSubmission(MoveType.PLAY)
      .setOrientation(move.getOrientation())
      .setPlayStart(new Coordinates().setX(move.getCol()).setY(move.getRow()))
      .setTilesPlayed(move.getTiles());
  }

  /**
   *
   * @param type
   * @return
   */
  public MoveSubmission createMoveSubmission(MoveType type) {
    return new MoveSubmission().setType(type);
  }

  /**
   * Check if a game has plays. This is useful when deciding whether or not a generated
   * move should play across the middle
   *
   * @param state
   * @return
   */
  public boolean gameHasPlays(GameState state) {
    for (MoveData move : state.getAllMoves()) {
      if (move.getMoveType() == MoveType.PLAY)
        return true;
    }

    return false;
  }

  /**
   * Convenience method for filling a WordsWithFriendsBoard given a game state
   *
   * @param state
   * @return
   */
  public WordsWithFriendsBoard createBoardFromState(GameState state) {
    WordsWithFriendsBoard board = new WordsWithFriendsBoard();
    fillBoard(board, state);

    return board;
  }

  /**
   * Convenience method for filling a WordsWithFriendsBoard given a game state
   *
   * @param board
   * @param state
   */
  public void fillBoard(WordsWithFriendsBoard board, GameState state) {
    List<Slot> tiles = state.getBoard();

    for (int i = 0; i < tiles.size(); i++) {
      Tile tile = tiles.get(i).getTile();

      if (tile != null) {
        board.getSlot(i).setTile( tile );
      }
    }
  }

  /**
   *
   * @param tiles
   * @return
   */
  public Rack buildRack(List<Tile> tiles) {
    return new Rack().setCapacity(WordsWithFriendsBoard.TILES_PER_PLAYER).setTiles(tiles);
  }

  /**
   *
   * @param userId
   * @param state
   * @return
   */
  public Rack buildRack(long userId, GameState state) {
    Rack rack = new Rack().setCapacity(WordsWithFriendsBoard.TILES_PER_PLAYER);
    List<Tile> letters = state.getRacks().get(userId);

    if (letters == null)
      return null;

    rack.setTiles(letters);

    return rack;
  }

  /**
   * Check if the provided tile represents a BLANK.
   *
   * @param tile
   * @return
   */
  public boolean tileIsBlank(Tile tile) {
    return tile.getValue() == 0;
  }

  /**
   *
   * @param user
   * @param state
   * @return user's score.
   */
  public int getScore(User user, GameState state) {
    return getScore(user.getId(), state);
  }

  /**
   *
   * @param uid
   * @param state
   * @return user's score
   */
  public int getScore(long uid, GameState state) {
    Integer score = state.getScores().get(uid);

    return score == null ? 0 : score;
  }

  /**
   * Adds the specified amount to the provided user's score.
   *
   * @param uid
   * @param state
   * @param amount
   * @return
   */
  public int addToScore(long uid, GameState state, int amount) {
    int newScore = getScore(uid, state) + amount;
    state.putToScores(uid, newScore);

    return newScore;
  }

  /**
   * Adds the specified amount to the provided user's score.
   *
   * @param user
   * @param state
   * @param amount
   * @return
   */
  public int addToScore(User user, GameState state, int amount) {
    long uid = user.getId();
    int newScore = getScore(uid, state) + amount;
    state.putToScores(uid, newScore);

    return newScore;
  }

  /**
   * Given a move, apply it to the GameState. This builds the resulting racks, board, scores, etc.
   *
   * @param state
   * @param move
   * @return
   */
  public GameState applyMove(GameState state, Move move) {
    GameState copy = state.deepCopy();

    // Make sure nothing tries to use the old state
    state = null;

    User moveUser = copy.getMeta().getUsersById().get(copy.getMeta().getCurrentMoveUserId());

    // Build board from current game state
    WordsWithFriendsBoard board = createBoardFromState(copy);

    // swap users
    copy.getMeta().setCurrentMoveUserId(getOtherUser(copy.getMeta().getCurrentMoveUserId(), copy).getId());

    // pop tiles
    List<Tile> remaining = new ArrayList<Tile>(Math.max(0, copy.getRemainingTilesSize() - move.getTiles().size()));
    List<Tile> popped = new LinkedList<Tile>();
    for (int i = 0; i < Math.min(copy.getRemainingTilesSize(), move.getTiles().size()); i++) {
      popped.add(copy.getRemainingTiles().get(i));
    }
    for (int i = move.getTiles().size(); i < copy.getRemainingTilesSize(); i++) {
      remaining.add(copy.getRemainingTiles().get(i));
    }
    copy.setRemainingTiles(remaining);

    // racks
    copy.getRacks().get(moveUser.getId()).removeAll(move.getTiles());
    copy.getRacks().get(moveUser.getId()).addAll(popped);

    // scores
    addToScore(moveUser, copy, move.getResult().getScore());

    // board
    copy.setBoard(board.getStorage().getSlots());

    return copy;
  }

  /**
   *
   * @param move
   * @return
   */
  public Move buildGameStateMove(MoveData move) {
    return new Move(move.getTiles(),
      move.getPlayStartPosition().getY(),
      move.getPlayStartPosition().getX(),
      move.getPlayStartPosition().getX() < move.getPlayEndPosition().getX() ? WordOrientation.HORIZONTAL : WordOrientation.VERTICAL);
  }

  /**
   * Build a game state move from a move submission
   *
   * @param move
   * @return
   */
  public Move buildGameStateMove(MoveSubmission move) {
    return new Move(move.getTilesPlayed(),
      move.getPlayStart().getY(),
      move.getPlayStart().getX(),
      move.getOrientation());
  }
}
