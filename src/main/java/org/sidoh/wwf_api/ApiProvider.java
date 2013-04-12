package org.sidoh.wwf_api;

import org.sidoh.wwf_api.types.api.ChatMessage;
import org.sidoh.wwf_api.types.api.GameIndex;
import org.sidoh.wwf_api.types.api.GameMeta;
import org.sidoh.wwf_api.types.api.GameState;
import org.sidoh.wwf_api.types.api.MoveSubmission;
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
   */
  public List<ChatMessage> getUnreadChats(String accessToken, long gameId) {
    return parser.parseUnreadChats(comm.getUnreadChats(accessToken, gameId));
  }

  /**
   * Get a list of games.
   *
   * @param accessToken
   * @return
   */
  public GameIndex getGameIndex(String accessToken) {
    return parser.parseGameIndex(comm.getGameIndex(accessToken));
  }

  /**
   * Get all of the data associated with a particular game.
   *
   * @param accessToken
   * @param gameId
   * @return
   */
  public GameState getGameState(String accessToken, long gameId) {
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
   */
  public GameState makeMove(String accessToken, GameState state, MoveSubmission move) {
    LOG.info("submitting move: " + move);

    comm.makeMove(accessToken, requestGenerator.generateMoveParams(state, move));

    // TODO: can reconstruct game state without making the request
    return getGameState(accessToken, state.getId());
  }

  /**
   * Request the creation of a matchmaking game.
   *
   * @param accessToken
   */
  public void createRandomGame(String accessToken) {
    LOG.info("creating random game...");

    comm.createRandomGame(accessToken);
  }

  /**
   * Create a game versus a person with a particular facebook Id.
   *
   * @param accessToken
   * @param userId
   */
  public void createFacebookGame(String accessToken, long userId) {
    LOG.info("creating game with user fb id = {}", userId);

    comm.createFacebookGame(accessToken, userId);
  }

  /**
   * Create a game versus a person with a particular Zynga Id.
   *
   * @param accessToken
   * @param userId
   */
  public void createZyngaGame(String accessToken, long userId) {
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
   */
  public ChatMessage submitChatMessage(String accessToken, long gameId, String message) {
    return parser.parseChatMessage(comm.submitChatMessage(accessToken, gameId, message));
  }

  /**
   * Check if the provided words are in the WWF dictionary
   *
   * @param accessToken
   * @param words
   * @return set of words that are NOT in the dictionary -- empty set if all are in the dictionary
   */
  public List<String> dictionaryLookup(String accessToken, List<String> words) {
    LOG.info("Checking if the following words are in the WWF dictionary: " + words);

    return parser.parseDictionaryLookupResponse( comm.dictionaryLookup(words, accessToken) );
  }

  /**
   * Retrieves a partial game index, including only data about games that have had updates since the
   * provided timestamp.
   *
   * @param accessToken
   * @param timestamp
   * @return GameIndex with games having updates occurring after the provided timestamp
   */
  public GameIndex getGamesWithUpdates(String accessToken, int timestamp) {
    return parser.parseGameIndex( comm.getGamesWithUpdates(accessToken, timestamp) );
  }
}
