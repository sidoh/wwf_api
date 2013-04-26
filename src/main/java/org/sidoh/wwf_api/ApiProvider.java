package org.sidoh.wwf_api;

import com.google.common.collect.Sets;
import org.sidoh.wwf_api.game_state.WordsWithFriendsBoard;
import org.sidoh.wwf_api.parser.ParserException;
import org.sidoh.wwf_api.parser.ResponseParser;
import org.sidoh.wwf_api.types.api.ChatMessage;
import org.sidoh.wwf_api.types.api.GameIndex;
import org.sidoh.wwf_api.types.api.GameState;
import org.sidoh.wwf_api.types.api.MoveSubmission;
import org.sidoh.wwf_api.types.api.MoveType;
import org.sidoh.wwf_api.types.game_state.Tile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

/**
 * Ties together Communication, RequestGenerator, and ResponseParser
 */
public class ApiProvider {
  private static final Logger LOG = LoggerFactory.getLogger(ApiProvider.class);

  private final StateReconstructor stateReconstructor;
  private final Communication comm;
  private final ResponseParser parser;
  private final RequestGenerator requestGenerator;

  public ApiProvider() {
    this.comm = new Communication();
    this.parser = new ResponseParser();
    this.stateReconstructor = new StateReconstructor();
    this.requestGenerator = new RequestGenerator();
  }

  /**
   * Return a list of chat messages assigned to a particular game
   *
   * @param accessToken
   * @param gameId
   * @return
   * @throws ApiRequestException, ParserException
   */
  public List<ChatMessage> getUnreadChats(String accessToken, long gameId) throws ApiRequestException, ParserException {
    return parser.parseUnreadChats(comm.getUnreadChats(accessToken, gameId));
  }

  /**
   * Get a list of games.
   *
   * @param accessToken
   * @return
   * @throws ApiRequestException, ParserException
   */
  public GameIndex getGameIndex(String accessToken) throws ApiRequestException, ParserException {
    return parser.parseGameIndex(comm.getGameIndex(accessToken));
  }

  /**
   * Get all of the data associated with a particular game.
   *
   * @param accessToken
   * @param gameId
   * @return
   * @throws ApiRequestException, ParserException
   */
  public GameState getGameState(String accessToken, long gameId) throws ApiRequestException, ParserException {
    GameState raw = parser.parseGameState(comm.getGameState(gameId, accessToken));

    return stateReconstructor.reconstructState(raw);
  }

  /**
   * Submit a move.
   *
   * @param accessToken
   * @param state
   * @param move
   * @return
   * @throws ApiRequestException, ParserException
   */
  public GameState makeMove(String accessToken, GameState state, MoveSubmission move)
    throws ApiRequestException, ParserException, MoveValidationException {
    validateMove(state, move);

    LOG.info("submitting move: " + move);
    comm.makeMove(accessToken, requestGenerator.generateMoveParams(state, move));

    // TODO: can reconstruct game state without making the request
    return getGameState(accessToken, state.getId());
  }

  /**
   * Request the creation of a matchmaking game.
   *
   * @param accessToken
   * @throws ApiRequestException, ParserException
   */
  public void createRandomGame(String accessToken) throws ApiRequestException, ParserException {
    LOG.info("creating random game...");

    comm.createRandomGame(accessToken);
  }

  /**
   * Create a game versus a person with a particular facebook Id.
   *
   * @param accessToken
   * @param userId
   * @throws ApiRequestException, ParserException
   */
  public void createFacebookGame(String accessToken, long userId) throws ApiRequestException, ParserException {
    LOG.info("creating game with user fb id = {}", userId);

    comm.createFacebookGame(accessToken, userId);
  }

  /**
   * Create a game versus a person with a particular Zynga Id.
   *
   * @param accessToken
   * @param userId
   * @throws ApiRequestException, ParserException
   */
  public void createZyngaGame(String accessToken, long userId) throws ApiRequestException, ParserException {
    LOG.info("creating game with user id = {}", userId);

    comm.createZyngaGame(accessToken, userId);
  }

  /**
   * Submit a chat message
   *
   * @param accessToken
   * @param gameId
   * @param message
   * @return
   * @throws ApiRequestException, ParserException
   */
  public ChatMessage submitChatMessage(String accessToken, long gameId, String message)
    throws ApiRequestException, ParserException {
    return parser.parseChatMessage(comm.submitChatMessage(accessToken, gameId, message));
  }

  /**
   * Check if the provided words are in the WWF dictionary
   *
   * @param accessToken
   * @param words
   * @return set of words that are NOT in the dictionary -- empty set if all are in the dictionary
   * @throws ApiRequestException, ParserException
   */
  public List<String> dictionaryLookup(String accessToken, List<String> words)
    throws ApiRequestException, ParserException {
    LOG.debug("Checking if the following words are in the WWF dictionary: " + words);

    return parser.parseDictionaryLookupResponse( comm.dictionaryLookup(words, accessToken) );
  }

  /**
   * Retrieves a partial game index, including only data about games that have had updates since the
   * provided timestamp.
   *
   * @param accessToken
   * @param timestamp
   * @return GameIndex with games having updates occurring after the provided timestamp
   * @throws ApiRequestException, ParserException
   */
  public GameIndex getGamesWithUpdates(String accessToken, int timestamp) throws ApiRequestException, ParserException {
    return parser.parseGameIndex( comm.getGamesWithUpdates(accessToken, timestamp) );
  }

  /**
   * Validates the provided move, ensuring that it's legal. This does <i>not</i> ensure that all of
   * the words formed are valid words. It instead checks that everything is sane. For example, it
   * ensures that all of the played tiles are actually in the rack of the user submitting the move.
   *
   * @param state the game state before the move is submitted
   * @param move the move to be validated
   */
  protected void validateMove(GameState state, MoveSubmission move) {
    // Ensure that coordinates are within expected bounds:
    if ( move.getType() == MoveType.PLAY ) {
      int x = move.getPlayStart().getX();
      int y = move.getPlayStart().getY();

      if (x < 0 || x >= WordsWithFriendsBoard.DIMENSIONS || y < 0 || y >= WordsWithFriendsBoard.DIMENSIONS) {
        throw new MoveValidationException("Coordinates not within expected bounds. Must be in the interval "
          + "(0," + WordsWithFriendsBoard.DIMENSIONS + "). Got: " + move.getPlayStart());
      }
    }

    // Ensure that the tiles played are in the user's rack.
    if ( move.getType() == MoveType.PLAY || move.getType() == MoveType.SWAP ) {
      List<Tile> available = state.getRacks().get(state.getMeta().getCurrentMoveUserId());
      Set<Integer> playedTileIds = Sets.newHashSet();
      Set<Integer> availableTileIds = Sets.newHashSet();

      for (Tile tile : move.getTilesPlayed()) {
        playedTileIds.add(tile.getValue());
      }
      for (Tile tile : available) {
        availableTileIds.add(tile.getValue());
      }

      if ( !Sets.difference(playedTileIds, availableTileIds).isEmpty()) {
        throw new MoveValidationException("Tried to play move including tiles that aren't in current player's rack. "
          + "Played tiles: " + move.getTilesPlayed() + " // available: " + available);
      }
    }
  }
}
