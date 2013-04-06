package org.sidoh.wwf_api;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import org.sidoh.wwf_api.game_state.GameStateHelper;
import org.sidoh.wwf_api.game_state.Move;
import org.sidoh.wwf_api.game_state.WordsWithFriendsBoard;
import org.sidoh.wwf_api.types.api.GameState;
import org.sidoh.wwf_api.types.api.MoveSubmission;
import org.sidoh.wwf_api.types.api.MoveType;
import org.sidoh.wwf_api.types.game_state.Tile;
import org.sidoh.wwf_api.types.game_state.WordOrientation;

import java.util.LinkedList;
import java.util.List;

/**
 * Responsible for generating request data for things that require complicated generation, like
 * moves.
 */
public class RequestGenerator {
  /**
   * Needed for computing board checksum, etc.
   */
  private static final GameStateHelper stateHelper = new GameStateHelper();

  public interface RequestParam {
    public String getKey();
    public Object getDefaultValue();
  }

  public enum MoveRequestParam implements RequestParam {
    GAME_ID("game_id", null),
    FROM_X("from_x", 0),
    TO_X("to_x", 0),
    FROM_Y("from_y", 0),
    TO_Y("to_y", 0),
    TEXT("text", null),
    WORDS("words][", null), // this is a bit hacky. results in move[words][] being the key
    PROMOTED("promoted", null),
    POINTS("points", 0),
    BOARD_CHECKSUM("board_checksum", null),
    MOVE_INDEX("move_index", null);

    private final String key;
    private final Object defaultValue;

    private MoveRequestParam(String key, Object defaultValue) {
      this.defaultValue = defaultValue;
      this.key = "move[".concat(key).concat("]");
    }

    @Override
    public String getKey() {
      return key;
    }

    @Override
    public Object getDefaultValue() {
      return defaultValue;
    }
  }

  /**
   * Generates all of the relevant POST params for a move call.
   *
   * @param gameState
   * @param move
   * @return multimap because some keys can be repeated.
   */
  public Multimap<MoveRequestParam, Object> generateMoveParams(GameState gameState, MoveSubmission move) {
    Multimap<MoveRequestParam, Object> params = LinkedHashMultimap.create();

    // Create a board to fiddle with. (needed for computing checksums, etc.)
    WordsWithFriendsBoard board = stateHelper.createBoardFromState(gameState);

    // Always need the game id, checksum, promoted, and move index, regardless of move type.
    // Can't always compute the checksum right away... need to wait until after play to compute
    // same for promoted.
    params.put(MoveRequestParam.GAME_ID, gameState.getId());
    params.put(MoveRequestParam.MOVE_INDEX, gameState.getAllMovesSize());

    // PLAY is definitely the most complicated of the bunch...
    if (move.getType() == MoveType.PLAY) {
      Move gameMove = buildBoardMove(move);

      // Make move - need to do so to accurately compute checksum, etc.
      board.move(gameMove);

      // Call helper method to compute some of the request params related to the
      // play parameters
      PlayParams playParams = buildPlayParams(gameMove, board);

      params.put(MoveRequestParam.BOARD_CHECKSUM, computeBoardChecksum(board));
      params.put(MoveRequestParam.POINTS, gameMove.getResult().getScore());
      params.put(MoveRequestParam.MOVE_INDEX, gameState.getAllMoves().size());
      params.put(MoveRequestParam.PROMOTED, computePromotedValue(gameState, gameMove));
      params.put(MoveRequestParam.FROM_X, playParams.getFromX());
      params.put(MoveRequestParam.FROM_Y, playParams.getFromY());
      params.put(MoveRequestParam.TO_X, playParams.getToX());
      params.put(MoveRequestParam.TO_Y, playParams.getToY());
      params.put(MoveRequestParam.TEXT, playParams.getText());

      for (String word : gameMove.getResult().getResultingWords()) {
        params.put(MoveRequestParam.WORDS, word.toLowerCase());
      }
    }
    // PASS is indicated by setting all of the x,y stuff = 0. but those are the default values,
    // so don't need to do anything.
    else if (move.getType() == MoveType.PASS) {  }
    // SWAP is indicated by setting FROM_X to 101 and listing the swapped tiles.
    else if (move.getType() == MoveType.SWAP) {
      params.put(MoveRequestParam.FROM_X, 101);
      params.put(MoveRequestParam.TEXT, buildSwapText(move.getTilesPlayed()));
    }
    // TIE indicated by 96
    else if (move.getType() == MoveType.TIE) {
      params.put(MoveRequestParam.FROM_X, 96);
    }
    // DECLINE indicated by 97
    else if (move.getType() == MoveType.DECLINE) {
      params.put(MoveRequestParam.FROM_X, 97);
    }
    // FORFEIT is indicated by setting FROM_X to 99
    else if (move.getType() == MoveType.RESIGN) {
      params.put(MoveRequestParam.FROM_X, 99);
    }
    // GAME_OVER indicated by 100
    else if (move.getType() == MoveType.GAME_OVER) {
      params.put(MoveRequestParam.FROM_X, 100);
    }
    else {
      throw new RuntimeException("unsupported move type: " + move.getType());
    }

    // This still needs to be filled in for non-play moves.
    if (! params.containsKey(MoveRequestParam.BOARD_CHECKSUM))
      params.put(MoveRequestParam.BOARD_CHECKSUM, computeBoardChecksum(board));

    // This still needs to be filled in for non-play moves.
    if (! params.containsKey(MoveRequestParam.PROMOTED))
      params.put(MoveRequestParam.PROMOTED, computePromotedValue(gameState, null));

    fillDefaultValues(params, MoveRequestParam.class);

    return params;
  }

  /**
   * Fill in default values for a request
   *
   * @param params
   * @param cls
   * @param <T>
   */
  protected static <T extends RequestParam> void fillDefaultValues(Multimap<T, Object> params, Class<T> cls) {
    for (RequestParam param : cls.getEnumConstants()) {
      if (! params.containsKey(param) && param.getDefaultValue() != null)
        params.put((T)param, param.getDefaultValue());
    }
  }

  /**
   * Compute the checksum. Note that any tiles placed on the board should have IDs that match
   * what the WWF API expects. In other words, only play tiles on the board if they're created
   * through the API.
   *
   * @param board
   * @return
   */
  protected static int computeBoardChecksum(WordsWithFriendsBoard board) {
    int a = 0;
    int b = 0;

    for (int row = 0; row < WordsWithFriendsBoard.DIMENSIONS; row++) {
      for (int col = 0; col < WordsWithFriendsBoard.DIMENSIONS; col++) {
        Tile tile = board.getSlot(row, col).getTile();
        int value = tile == null ? -1 : tile.getId();

        a ^= value;

        if (value != 0) {
          a ^= 1 << b;
        }

        if (++b == 32) {
          b = 0;
        }
      }
    }

    return a;
  }

  /**
   * Convenience method for constructing the game state Move rather than the MoveSubmission move (which is
   * compatible with WordsWithFriendsBoard's move() function. This is used to generate some of the metadata
   * needed for submitting a move with type PLAY.
   *
   * @param submission
   * @return
   */
  protected static Move buildBoardMove(MoveSubmission submission) {
    return new Move(
      submission.getTilesPlayed(),
      submission.getPlayStart().getY(),
      submission.getPlayStart().getX(),
      submission.getOrientation());
  }

  /**
   * A move type of PLAY has complicated parameters. These include the start and end position of a word played,
   * a board checksum, and a parameter "text" indicating which of the user's tiles were played. This method
   * builds some of those parameters.
   *
   * @param move
   * @param board
   * @return
   */
  protected static PlayParams buildPlayParams(Move move, WordsWithFriendsBoard board) {
    StringBuilder textBuilder = new StringBuilder();
    LinkedList<Tile> playedTiles = new LinkedList<Tile>(move.getTiles());

    int x = move.getCol();
    int y = move.getRow();
    int dx = move.getOrientation() == WordOrientation.HORIZONTAL ? 1 : 0;
    int dy = move.getOrientation() == WordOrientation.HORIZONTAL ? 0 : 1;

    while (! playedTiles.isEmpty()) {
      if (board.getSlot(y, x).getTile().getId() == playedTiles.getFirst().getId()) {
        Tile played = playedTiles.removeFirst();

        // Handle blanks
        if (played.getValue() == 0) {
          textBuilder
            .append(played.getId())
            .append(',')
            .append(played.getLetter().getValue().toLowerCase())
            .append(',');
        }
        else {
          textBuilder
            .append(played.getId())
            .append(',');
        }
      }
      // handle skips
      else {
        textBuilder.append("*,");
      }

      if (! playedTiles.isEmpty()) {
        x += dx;
        y += dy;
      }
    }

    return new PlayParams(
      move.getCol(),
      move.getRow(),
      x,
      y,
      textBuilder.toString());
  }

  /**
   * It's unclear what this is for. It doesn't actually convey any additional information. Maybe it's used for
   * verification or convenience on Zynga's end. I'm just mimicking the Javascript:
   *
   * promoted: this.tiles.length == 1 && this.words.length > 1 ? 3 : this.orientation() == "horizontal" ? 1 : 2,
   *
   * @param state
   * @param move
   * @return
   */
  protected static int computePromotedValue(GameState state, Move move) {
    int numPlayedTiles = move == null ? 0 : move.getTiles().size();
    int numResultingWords = move == null ? 0 : move.getResult().getResultingWords().size();

    if ( ((state.getRemainingTiles().size() - numPlayedTiles) == 1) && numResultingWords > 1 ) {
      return 3;
    }
    else if (move != null && move.getOrientation() == WordOrientation.HORIZONTAL) {
      return 1;
    }
    else {
      return 2;
    }
  }

  /**
   * When swapping tiles, move api expects a comma-separated list of tile IDs that were swapped
   *
   * @param swappedTiles
   * @return
   */
  protected static String buildSwapText(List<Tile> swappedTiles) {
    StringBuilder b = new StringBuilder();

    for (Tile swappedTile : swappedTiles) {
      b.append(swappedTile.getId()).append(',');
    }

    return b.toString();
  }

  protected static class PlayParams {
    private final int fromX;
    private final int fromY;
    private final int toX;
    private final int toY;
    private final String text;

    public PlayParams(int fromX, int fromY, int toX, int toY, String text) {
      this.fromX = fromX;
      this.fromY = fromY;
      this.toX = toX;
      this.toY = toY;
      this.text = text;
    }

    public int getFromX() {
      return fromX;
    }

    public int getFromY() {
      return fromY;
    }

    public int getToX() {
      return toX;
    }

    public int getToY() {
      return toY;
    }

    public String getText() {
      return text;
    }
  }
}
