package org.sidoh.wwf_api;

import org.sidoh.wwf_api.game_state.GameStateHelper;
import org.sidoh.wwf_api.parser.ParserException;
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
   * @throws ApiRequestException, ParserException
   */
  public List<ChatMessage> getUnreadChats(long gameId) throws ApiRequestException, ParserException {
    return PROVIDER.getUnreadChats(accessToken, gameId);
  }

  /**
   * Get a list of games.
   *
   * @return
   * @throws ApiRequestException, ParserException
   */
  public GameIndex getGameIndex() throws ApiRequestException, ParserException {
    return PROVIDER.getGameIndex(accessToken);
  }

  /**
   * Get all of the data associated with a particular game.
   *
   * @param gameId
   * @return
   * @throws ApiRequestException, ParserException
   */
  public GameState getGameState(long gameId) throws ApiRequestException, ParserException {
    return PROVIDER.getGameState(accessToken, gameId);
  }

  /**
   * Submit a move.
   *
   * @param state
   * @param move
   * @return
   * @throws ApiRequestException, ParserException
   */
  public GameState makeMove(GameState state, MoveSubmission move)
    throws ApiRequestException, MoveValidationException, ParserException {
    return PROVIDER.makeMove(accessToken, state, move);
  }

  /**
   * Request the creation of a matchmaking game.
   *
   * @throws ApiRequestException, ParserException
   */
  public void createRandomGame() throws ApiRequestException, ParserException {
    PROVIDER.createRandomGame(accessToken);
  }

  /**
   * Create a game versus a person with a particular facebook Id.
   *
   * @param userId
   * @throws ApiRequestException, ParserException
   */
  public void createFacebookGame(long userId) throws ApiRequestException, ParserException {
    PROVIDER.createFacebookGame(accessToken, userId);
  }

  /**
   * Create a game versus a person with a particular Zynga Id.
   *
   * @param userId
   * @throws ApiRequestException, ParserException
   */
  public void createZyngaGame(long userId) throws ApiRequestException, ParserException {
    PROVIDER.createZyngaGame(accessToken, userId);
  }

  /**
   * Submit a chat message
   *
   * @param gameId
   * @param message
   * @return
   * @throws ApiRequestException, ParserException
   */
  public ChatMessage submitChatMessage(long gameId, String message) throws ApiRequestException, ParserException {
    return PROVIDER.submitChatMessage(accessToken, gameId, message);
  }

  /**
   * Check if the provided words are in the WWF dictionary
   *
   * @param words a list of words to look up
   * @return list of words that are NOT in the dictionary -- empty set if all are in the dictionary
   * @throws ApiRequestException, ParserException
   */
  public List<String> dictionaryLookup(List<String> words) throws ApiRequestException, ParserException {
    return PROVIDER.dictionaryLookup(accessToken, words);
  }

  /**
   * Retrieves a partial game index, including only data about games that have had updates since the
   * provided timestamp.
   *
   * @param timestamp
   * @return GameIndex with games having updates occurring after the provided timestamp
   * @throws ApiRequestException, ParserException
   */
  public GameIndex getGamesWithUpdates(int timestamp) throws ApiRequestException, ParserException {
    return PROVIDER.getGamesWithUpdates(accessToken, timestamp);
  }
}
