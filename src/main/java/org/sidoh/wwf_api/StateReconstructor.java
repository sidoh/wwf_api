package org.sidoh.wwf_api;

import com.google.common.collect.Lists;
import org.sidoh.wwf_api.game_state.GameStateHelper;
import org.sidoh.wwf_api.game_state.WordsWithFriendsBoard;
import org.sidoh.wwf_api.types.api.GameMeta;
import org.sidoh.wwf_api.types.api.GameState;
import org.sidoh.wwf_api.types.api.MoveData;
import org.sidoh.wwf_api.types.api.MoveType;
import org.sidoh.wwf_api.types.api.User;
import org.sidoh.wwf_api.types.game_state.Tile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class StateReconstructor {
  private static final Logger LOG = LoggerFactory.getLogger(StateReconstructor.class);

  private static final int BOARD_SIZE = 15;
  private static final int TILES_PER_PLAYER = 7;
  private static final String SKIPPED_TILE_INDICATOR = "*";

  private static final GameStateHelper stateHelper = GameStateHelper.getInstance();

  /**
   * Reconstructs the game state from a list of rules. This includes which tiles have
   * been played and both player's racks.
   *
   * @param state
   */
  public GameState reconstructState(GameState state) {
    GameMeta meta = state.getMeta();
    Bag bag = new Bag(meta.getRandomSeed());

    User player1 = null;
    User player2 = null;

    Map<Long, List<Tile>> tiles = new HashMap<Long, List<Tile>>();
    CountingHashMap<Long> scores = new CountingHashMap<Long>();

    for (User user : meta.getUsersById().values()) {
      if (user.getId() == meta.getCreatedByUserId()) {
        player1 = user;
      }
      else {
        player2 = user;
      }
    }

    if (player1 == null || player2 == null)
      throw new RuntimeException("couldn't find both players!");

    // Initialize tile lists for both players
    tiles.put(player1.getId(), bag.pullTiles(TILES_PER_PLAYER));
    tiles.put(player2.getId(), bag.pullTiles(TILES_PER_PLAYER));

    // Use this to keep track of scores
    WordsWithFriendsBoard scoringBoard = new WordsWithFriendsBoard();

    User currentUser = player1;
    User otherUser = player2;

    for (MoveData move : state.getAllMoves()) {
      List<Tile> playerTiles = tiles.get(currentUser.getId());
      int totalPlayedTiles = 0;

      // A list of tiles returned to the bag (done after replacing them)
      List<Tile> returnedTiles = Lists.newLinkedList();

      // Only need to do anything if tiles are played/swapped
      if (move.getMoveType() == MoveType.PLAY || move.getMoveType() == MoveType.SWAP) {
        int x = move.isSetPlayStartPosition() ? move.getPlayStartPosition().getX() : 0;
        int y = move.isSetPlayStartPosition() ? move.getPlayStartPosition().getY() : 0;

        int dx = (!move.isSetPlayEndPosition() || move.getPlayEndPosition().getX() == x ? 0 : 1);
        int dy = (!move.isSetPlayEndPosition() || move.getPlayEndPosition().getY() == y ? 0 : 1);

        if ( move.getMoveType() == MoveType.PLAY && dx == 1 && dy == 1 )
          throw new RuntimeException("move can only be horizontal or vertical");

        // Seems like there's a "null" move at the end that's equivalent to RESIGN
        // when the server detects that it's impossible for you to play a word
        // and there are no tiles left
        if (move.getText() != null && !( move.getText().isEmpty() )) {
          String[] playedTiles = move.getText().split(",");

          // Construct a list of the played tiles with all of the metadata attached
          List<Tile> constructedTiles = new ArrayList<Tile>();

          for (int i = 0; i < playedTiles.length; i++) {
            // '*' indicates that a player jumped over an already played tile
            if (SKIPPED_TILE_INDICATOR.equals(playedTiles[i])) {
              x += dx;
              y += dy;

              continue;
            }

            int tileId = Integer.parseInt(playedTiles[i]);
            Tile tile = Bag.getTileWithId(tileId);
            int sizeBefore = playerTiles.size();

            playerTiles.remove(tile);

            if (playerTiles.size() == sizeBefore)
              throw new RuntimeException("tried to remove: " + tile + " from: " + playerTiles + ", but couldn't find it");

            // If the tile was a blank, the next split will be what letter the blank was
            // assigned to.
            if ( stateHelper.tileIsBlank(tile) ) {
              tile.getLetter().setValue(playedTiles[++i].toUpperCase());
            }

            // If this is a swap, put the tile back.
            if (move.getMoveType() == MoveType.SWAP) {
              returnedTiles.add(tile);
            }
            // If it was a play, put it on the board.
            else if (move.getMoveType() == MoveType.PLAY) {
              x += dx;
              y += dy;
            }

            totalPlayedTiles++;
            constructedTiles.add(tile);
          }

          move.setTiles(constructedTiles);

          // Score the move if it's a play
          if (move.getMoveType() == MoveType.PLAY) {
            int points = scoringBoard.move(stateHelper.buildGameStateMove(move)).getScore();
            scores.increment(currentUser.getId(), points);

            if (move.isSetPoints() && move.getPoints() != points)
              LOG.debug("json move's points don't match computed points. saw: " + move.getPoints() + ", expected: " + points);
          }
        }

        // Give this player an appropriate number of tiles back
        for (int i = 0; i < totalPlayedTiles && bag.tilesLeft(); i++) {
          if (playerTiles.size() >= TILES_PER_PLAYER)
            throw new RuntimeException("tried to add more than 7 tiles to a rack");

          playerTiles.add(bag.pullTile());
        }

        // Return tiles that were swapped
        bag.returnTiles(returnedTiles);
      }

      // swap players
      User tmp = currentUser;
      currentUser = otherUser;
      otherUser = tmp;
    }

    state.setRacks(tiles);
    state.setBoard(scoringBoard.getStorage().getSlots());
    state.setScores(scores.getCounts());
    state.setRemainingTiles(bag.getRemainingTilesInPullOrder());

    return state;
  }

  private static int getIndex(int row, int col) {
    return row * BOARD_SIZE + col;
  }

  private static class CountingHashMap<T> extends HashMap<T, CountingHashMap.CountingInteger> {
    @Override
    public CountingInteger get(Object key) {
      CountingInteger value = super.get(key);

      if (value == null) {
        value = new CountingInteger();
        super.put((T) key, value);
      }

      return value;
    }

    public void increment(Object key) {
      get(key).increment();
    }

    public void increment(Object key, int by) {
      get(key).increment(by);
    }

    public Map<T, Integer> getCounts() {
      Map<T, Integer> r = new HashMap<T, Integer>();

      for (Map.Entry<T, CountingInteger> entry : entrySet()) {
        r.put(entry.getKey(), entry.getValue().getValue());
      }

      return r;
    }

    public static final class CountingInteger {
      private int value = 0;

      public void increment() {
        value++;
      }

      public int getValue() {
        return value;
      }

      public void increment(int by) {
        value += by;
      }
    }
  }
}
