package org.sidoh.wwf_api;

import org.sidoh.wwf_api.types.api.ChatMessage;
import org.sidoh.wwf_api.types.api.GameIndex;
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
   * @param authToken
   * @param gameId
   * @return
   */
  public List<ChatMessage> getUnreadChats(String authToken, long gameId) {
    return parser.parseUnreadChats(comm.getUnreadChats(authToken, gameId));
  }

  /**
   * Get a list of games.
   *
   * @param authToken
   * @return
   */
  public GameIndex getGameIndex(String authToken) {
    return parser.parseGameIndex(comm.getGameIndex(authToken));
  }

  /**
   * Get all of the data associated with a particular game.
   *
   * @param gameId
   * @param authToken
   * @return
   */
  public GameState getGameState(long gameId, String authToken) {
    GameState raw = parser.parseGameState(comm.getGameState(gameId, authToken));

    return stateReconstructor.reconstructState(raw);
  }

  /**
   * Submit a move.
   *
   * @param state
   * @param move
   * @param authToken
   * @return
   */
  public GameState makeMove(GameState state, MoveSubmission move, String authToken) {
    LOG.info("submitting move: " + move);

    comm.makeMove(authToken, requestGenerator.generateMoveParams(state, move));

    // TODO: can reconstruct game state without making the request
    return getGameState(state.getId(), authToken);
  }

  /**
   * Request the creation of a matchmaking game.
   *
   * @param authToken
   */
  public void createRandomGame(String authToken) {
    LOG.info("creating random game...");

    comm.createRandomGame(authToken);
  }

  /**
   * Create a game versus a person with a particular facebook Id.
   *
   * @param authToken
   * @param userId
   */
  public void createFacebookGame(String authToken, long userId) {
    LOG.info("creating game with user fb id = {}", userId);

    comm.createFacebookGame(authToken, userId);
  }

  /**
   * Create a game versus a person with a particular Zynga Id.
   *
   * @param authToken
   * @param userId
   */
  public void createZyngaGame(String authToken, long userId) {
    LOG.info("creating game with user id = {}", userId);

    comm.createZyngaGame(authToken, userId);
  }

  /**
   * Submit a chat message
   *
   * @param authToken
   * @param gameId
   * @param message
   * @return
   */
  public ChatMessage submitChatMessage(String authToken, long gameId, String message) {
    return parser.parseChatMessage(comm.submitChatMessage(authToken, gameId, message));
  }

  /**
   * Check if the provided words are in the WWF dictionary
   *
   * @param authToken
   * @param words
   * @return set of words that are NOT in the dictionary -- empty set if all are in the dictionary
   */
  public Set<String> dictionaryLookup(String authToken, Set<String> words) {
    LOG.info("Checking if the following words are in the WWF dictionary: " + words);

    return parser.parseDictionaryLookupResponse( comm.dictionaryLookup(words, authToken) );
  }
}
