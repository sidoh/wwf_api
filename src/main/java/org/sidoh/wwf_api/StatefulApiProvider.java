package org.sidoh.wwf_api;

import org.sidoh.wwf_api.game_state.GameStateHelper;
import org.sidoh.wwf_api.types.api.ChatMessage;
import org.sidoh.wwf_api.types.api.GameIndex;
import org.sidoh.wwf_api.types.api.GameState;
import org.sidoh.wwf_api.types.api.MoveSubmission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Ties together Communication, RequestGenerator, and ResponseParser. This implementation
 * remembers the accessToken so that it doens't have to be passed to every call. Useful when
 * not in a stateless environment.
 */
public class StatefulApiProvider {
  private static final Logger LOG = LoggerFactory.getLogger(StatefulApiProvider.class);

  private static final ApiProvider PROVIDER = new ApiProvider();
  private final String accessToken;

  public StatefulApiProvider(String accessToken) {
    this.accessToken = accessToken;
  }

  /**
   * Return a list of chat messages assigned to a particular game
   *
   * @param gameId
   * @return
   */
  public List<ChatMessage> getUnreadChats(long gameId) {
    return PROVIDER.getUnreadChats(accessToken, gameId);
  }

  /**
   * Get a list of games.
   *
   * @return
   */
  public GameIndex getGameIndex() {
    return PROVIDER.getGameIndex(accessToken);
  }

  /**
   * Get all of the data associated with a particular game.
   *
   * @param gameId
   * @return
   */
  public GameState getGameState(long gameId) {
    return PROVIDER.getGameState(accessToken, gameId);
  }

  /**
   * Submit a move.
   *
   * @param state
   * @param move
   * @return
   */
  public GameState makeMove(GameState state, MoveSubmission move) {
    return PROVIDER.makeMove(accessToken, state, move);
  }

  /**
   * Request the creation of a matchmaking game.
   *
   */
  public void createRandomGame() {
    PROVIDER.createRandomGame(accessToken);
  }

  /**
   * Create a game versus a person with a particular facebook Id.
   *
   * @param userId
   */
  public void createFacebookGame(long userId) {
    PROVIDER.createFacebookGame(accessToken, userId);
  }

  /**
   * Create a game versus a person with a particular Zynga Id.
   *
   * @param userId
   */
  public void createZyngaGame(long userId) {
    PROVIDER.createZyngaGame(accessToken, userId);
  }

  /**
   * Submit a chat message
   *
   * @param gameId
   * @param message
   * @return
   */
  public ChatMessage submitChatMessage(long gameId, String message) {
    return PROVIDER.submitChatMessage(accessToken, gameId, message);
  }

  /**
   * Check if the provided words are in the WWF dictionary
   *
   * @param words a list of words to look up
   * @return list of words that are NOT in the dictionary -- empty set if all are in the dictionary
   */
  public List<String> dictionaryLookup(List<String> words) {
    return PROVIDER.dictionaryLookup(accessToken, words);
  }

  /**
   * Retrieves a partial game index, including only data about games that have had updates since the
   * provided timestamp.
   *
   * @param timestamp
   * @return GameIndex with games having updates occurring after the provided timestamp
   */
  public GameIndex getGamesWithUpdates(int timestamp) {
    return PROVIDER.getGamesWithUpdates(accessToken, timestamp);
  }
}
