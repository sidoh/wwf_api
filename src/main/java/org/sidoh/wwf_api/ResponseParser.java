package org.sidoh.wwf_api;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import org.sidoh.wwf_api.types.api.ChatMessage;
import org.sidoh.wwf_api.types.api.Coordinates;
import org.sidoh.wwf_api.types.api.GameIndex;
import org.sidoh.wwf_api.types.api.GameMeta;
import org.sidoh.wwf_api.types.api.GameState;
import org.sidoh.wwf_api.types.api.Move;
import org.sidoh.wwf_api.types.api.MoveData;
import org.sidoh.wwf_api.types.api.MoveType;
import org.sidoh.wwf_api.types.api.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Responsible for parsing raw JSON returned by the Communication class and constructing
 * thrift types that represent that data.
 */
public class ResponseParser {
  private static final Logger LOG = LoggerFactory.getLogger(ResponseParser.class);

  /**
   *
   * @param data
   * @return
   */
  public ChatMessage parseChatMessage(Reader data) {
    JSONObject json = (JSONObject) JSONValue.parse(data);

    return parseChatMessage(json);
  }

  /**
   *
   * @param data
   * @return
   */
  public List<ChatMessage> parseUnreadChats(Reader data) {
    JSONObject json = (JSONObject) JSONValue.parse(data);
    JSONArray chats = (JSONArray) json.get("chat_messages");
    List<ChatMessage> results = new ArrayList<ChatMessage>();

    for (Object chat : chats) {
      results.add(parseChatMessage((JSONObject) chat));
    }

    return results;
  }

  /**
   *
   * @param response
   * @return
   */
  public GameIndex parseGameIndex(Reader response) {
    GameIndex index = new GameIndex();

    JSONObject gameIndex = (JSONObject) JSONValue.parse(response);
    JSONArray games = (JSONArray) gameIndex.get("games");

    index.setUser(parseUser((JSONObject) gameIndex.get("user")));

    for (Object gameObj : games) {
      JSONObject gameJson = (JSONObject) gameObj;

      index.addToGames(parseGameMeta(gameJson));
    }

    return index;
  }

  /**
   * Parses the response to a dictionary lookup request.
   *
   * @param response
   * @return set of words that are NOT in the dictionary -- empty set if all are in the dictionary
   */
  public Set<String> parseDictionaryLookupResponse(Reader response) {
    Object parsedResponse = JSONValue.parse(response);

    // If all words in the query are present
    // in the dictionary, the response will be an empty JSON array. Otherwise, it'll be an
    // object that contains the field "failed_words", containing the list of words that aren't
    // in the dictionary.
    if ( parsedResponse instanceof JSONObject ) {
      JSONArray failedWords = (JSONArray)((JSONObject) parsedResponse).get("failed_words");
      Set<String> words = new HashSet<String>();

      for (Object failedWord : failedWords) {
        words.add(((String)failedWord).toUpperCase());
      }

      return words;
    }
    else {
      return Collections.emptySet();
    }
  }

  /**
   * Parses game state. Note that this does NOT rebuild racks.
   *
   * @param response
   * @return
   */
  public GameState parseGameState(Reader response) {
    GameState state = new GameState();

    JSONObject gameJson = (JSONObject) JSONValue.parse(response);
    gameJson = (JSONObject) gameJson.get("game");
    JSONArray movesArr = (JSONArray) gameJson.get("moves");

    state.setId(getLongValue(gameJson.get("id")));
    state.setAllMoves(new ArrayList<MoveData>());

    for (Object moveObj : movesArr) {
      state.addToAllMoves(parseMove((JSONObject) moveObj));
    }

    JSONArray chatsJson = (JSONArray) gameJson.get("chat_messages");
    state.setChatMessages(new ArrayList<ChatMessage>());

    for (Object chat : chatsJson) {
      state.addToChatMessages(parseChatMessage((JSONObject) chat));
    }

    state.setMeta(parseGameMeta(gameJson));

    return state;
  }

  /**
   * Parses the game data for a particular game
   *
   * @param gameJson
   * @return
   */
  protected GameMeta parseGameMeta(JSONObject gameJson) {
    GameMeta game = new GameMeta();

    game
      .setCreatedAt((String) gameJson.get("created_at"))
      .setCreatedByUserId(getLongValue(gameJson.get("created_by_user_id")))
      .setId(getLongValue(gameJson.get("id")))
      .setMatchmaking((Boolean) gameJson.get("is_matchmaking"))
      .setOver((Boolean) gameJson.get("is_over"))
      .setRandomSeed(getLongValue(gameJson.get("random_seed")))
      .setOpponentPresent((Boolean) gameJson.get("opponent_present"));

    JSONArray usersJson = (JSONArray) gameJson.get("users");

    for (Object userJson : usersJson) {
      User user = parseUser((JSONObject)userJson);
      game.putToUsersById(user.getId(), user);
    }

    JSONArray unreadChatsJson = (JSONArray) gameJson.get("unread_chat_ids");
    game.setUnreadChatIds(new ArrayList<Long>());

    for (Object unreadChat : unreadChatsJson) {
      game.addToUnreadChatIds((Long) unreadChat);
    }

    if (gameJson.containsKey("last_move") && gameJson.get("last_move") != null) {
      game.setLastMove(parseMove((JSONObject) gameJson.get("last_move")));
    }

    if (gameJson.containsKey("current_move_user_id") && gameJson.get("current_move_user_id") != null) {
      game.setCurrentMoveUserId(getLongValue(gameJson.get("current_move_user_id")));
    }

    return game;
  }

  /**
   * Parse a single chat message
   *
   * @param chat
   * @return
   */
  private ChatMessage parseChatMessage(JSONObject chat) {
    return new ChatMessage()
      .setCode((Integer) chat.get("code"))
      .setCreatedAt((String) chat.get("created_at"))
      .setGameId(getLongValue(chat.get("game_id")))
      .setId(getLongValue(chat.get("id")))
      .setMessage((String) chat.get("message"))
      .setUserId(getLongValue(chat.get("user_id")));
  }

  /**
   * Parse a single move
   *
   * @param moveJson
   * @return
   */
  protected MoveData parseMove(JSONObject moveJson) {
    MoveData move = new MoveData();

    move
      .setBoardChecksum((Integer) moveJson.get("board_checksum"))
      .setCreatedAt((String) moveJson.get("created_at"))
      .setGameId(getLongValue(moveJson.get("game_id")))
      .setId(getLongValue(moveJson.get("id")))
      .setMoveIndex((Integer) moveJson.get("move_index"))
      .setMoveType(parseMoveType((String) moveJson.get("move_type")))
      .setWords(parseWords((JSONArray) moveJson.get("words")));

    if (moveJson.containsKey("promoted") && moveJson.get("promoted") != null) {
      move.setPromoted((Integer) moveJson.get("promoted"));
    }

    if (moveJson.containsKey("text") && moveJson.get("text") != null && !"(null)".equals(moveJson.get("text"))) {
      move
        .setText((String) moveJson.get("text"));
    }

    if (moveJson.containsKey("points") && moveJson.get("points") != null) {
      move
        .setPoints((Integer) moveJson.get("points"));
    }

    if (moveJson.containsKey("from_x") && moveJson.get("from_x") != null) {
      move.setPlayStartPosition(new Coordinates()
        .setX((Integer) moveJson.get("from_x"))
        .setY((Integer) moveJson.get("from_y")));
    }

    if (moveJson.containsKey("to_x") && moveJson.get("to_x") != null) {
      move.setPlayEndPosition(new Coordinates()
        .setX((Integer) moveJson.get("to_x"))
        .setY((Integer) moveJson.get("to_y")));
    }

    return move;
  }

  /**
   * Parse an array of strings.
   *
   * @param wordsJson
   * @return
   */
  protected List<String> parseWords(JSONArray wordsJson) {
    List<String> words = new ArrayList<String>(wordsJson.size());

    for (Object word : wordsJson) {
      words.add((String) word);
    }

    return words;
  }

  /**
   *
   * @param rawType
   * @return
   */
  protected MoveType parseMoveType(String rawType) {
    return rawType == null ? null : MoveType.valueOf(rawType.toUpperCase());
  }

  /**
   *
   * @param userJson
   * @return
   */
  protected User parseUser(JSONObject userJson) {
    User user = new User();

    user
      .setName((String) userJson.get("name"))
      .setId((Integer) userJson.get("id"));

    if ( userJson.containsKey("fb_uid")) {
      user.setFbId((Integer) userJson.get("fb_uid"));
    }

    return user;
  }

  /**
   *
   * @param obj
   * @return
   */
  protected static long getLongValue(Object obj) {
    if (obj instanceof Integer) {
      return (long)(Integer)obj;
    }
    else if (obj instanceof Long) {
      return (Long)obj;
    }
    else {
      throw new RuntimeException("Unsupported type: " + obj.getClass());
    }
  }
}
