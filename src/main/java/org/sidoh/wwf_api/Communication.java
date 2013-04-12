package org.sidoh.wwf_api;

import com.google.common.base.Joiner;
import com.google.common.collect.Multimap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Responsible for sending pre-formatted requests to Zynga's servers and returning the raw results.
 */
public class Communication {
  private static final Logger LOG = LoggerFactory.getLogger(Communication.class);

  private static final SimpleDateFormat ISO6801_DATE_FORMAT
    = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ'+00:00'", Locale.GERMANY);

  /**
   * URL prefix used for all requests
   */
  private static final String BASE_URL = "https://wwf-fb.zyngawithfriends.com/api/";

  /**
   * It seems that all requests are required to have a X-Product header, which specifies which
   * platform the WWF API is being accessed from.
   *
   * "WordsWithFriendsPaidiPad/3.21" seems to return the whole game state without additional
   * requests, but since it's kind of expensive to regenerate game state... let's do this
   * (saves bandwidth too).
   */
  private static final String DEFAULT_PRODUCT = "WordsWithFriendsHtmlDesktop/4.02";

  /**
   * Name of header used to specify product. DEFAULT_PRODUCT gives the value to be used for
   * this header.
   */
  private static final String PRODUCT_HEADER_NAME = "X-Product";

  /**
   * Every request needs to specify an access token. This is how Zynga authenticates users.
   */
  private static final String AUTH_TOKEN_HEADER_NAME = "X-Access-Token";

  /**
   * The User-Agent header sent along with every request. Should probably be something that
   * doesn't give away you're using a bot.
   */
  private static final String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_4) AppleWebKit/536.11 (KHTML, like Gecko) Chrome/20.0.1132.47 Safari/536.11";

  /**
   * Fetch all unread chats for a particular game
   *
   * @param authToken
   * @param gameId
   * @return
   */
  public Reader getUnreadChats(String authToken, long gameId) {
    return makeRequest( getUnreadChatsUrl(gameId), authToken );
  }

  /**
   * Get a list of all recent games, including those that have already completed. The
   * number of games that is returned by this call is controlled by Zynga.
   *
   * @param authToken
   * @return
   */
  public Reader getGameIndex(String authToken) {
    return makeRequest( getIndexUrl(), authToken );
  }

  /**
   * Fetch full data about a particular game. One should be able to reconstruct the game
   * state given the data returned by this call.
   *
   * @param gameId
   * @param authToken
   * @return
   */
  public Reader getGameState(long gameId, String authToken) {
    return makeRequest( getGameUrl(gameId), authToken );
  }

  /**
   * Check which of the provided words are contained in the WWF dictionary. Note that there
   * exist words in the ENABLE1 dictionary that are not in the WWF dictionary. Likewise,
   * some valid WWF words are not in the ENABLE1 dictionary.
   *
   * @param words
   * @param authToken
   * @return
   */
  public Reader dictionaryLookup(List<String> words, String authToken) {
    String requestUrl = Joiner.on(';').join(words).toLowerCase();

    // This is a bit of a silly bug on Zynga's end... if you send this request with a word list
    // starting with "api", that word will be trimmed to not include "api". I discovered this
    // when this call was returning with "ECE" is not a word when validating "APIECE". This is
    // a hack to get around that problem.
    if ( requestUrl.startsWith("api") ) {
      requestUrl = "cat;".concat(requestUrl);
    }

    return makeRequest(getDictionaryLookupUrl(requestUrl), authToken);
  }

  /**
   *
   * @param accessToken
   * @param timestamp
   */
  public Reader getGamesWithUpdates(String accessToken, int timestamp) {
    String formattedTimestamp = ISO6801_DATE_FORMAT.format(new Date(timestamp*1000L));
    URL url = getGamesWithUpdatesUrl(formattedTimestamp);

    return makeRequest( url, accessToken );
  }

  /**
   * Submit a move for a particular game. The params for this call should be built using
   * RequestGenerator.
   *
   * @param authToken
   * @param params move submission params -- use RequestGenerator to build this
   * @return
   */
  public Reader makeMove( String authToken, Multimap<RequestGenerator.MoveRequestParam, Object> params ) {
    String postData = buildPostData(params);

    return postRequest(getMoveUrl(), authToken, postData);
  }

  /**
   * Creates a random (matchmaking) game. Sometimes you'll be matched with another player
   * immediately. Other times, you'll be matched within a few minutes of creating a new
   * game.
   *
   * @param authToken
   * @return
   */
  public Reader createRandomGame(String authToken) {
    return postRequest( getIndexUrl(), authToken, "create_type=Matchmaking" );
  }

  /**
   * Create a game versus a player having a particular Facebook ID.
   *
   * @param authToken
   * @param userId
   * @return
   */
  public Reader createFacebookGame(String authToken, long userId) {
    return postRequest( getIndexUrl(), authToken, "create_type=Search&opponent_fb_id=".concat(String.valueOf(userId)) );
  }

  /**
   * Create a game versus a player having a particular Zynga ID. Game metadata from the API
   * contains Zynga user IDs.
   *
   * @param authToken
   * @param userId
   * @return
   */
  public Reader createZyngaGame(String authToken, long userId) {
    return postRequest( getIndexUrl(), authToken, "create_type=Search&opponent_id=".concat(String.valueOf(userId)) );
  }

  /**
   * Send a chat message to your opponent from a particular game.
   *
   * @param authToken
   * @param gameId
   * @param message
   * @return
   */
  public Reader submitChatMessage(String authToken, long gameId, String message) {
    return postRequest( getChatUrl(), authToken, "chat_message", "game_id", gameId, "code", 0, "message", message);
  }

  /**
   * Send an HTTP POST request.
   *
   * @param url
   * @param authToken
   * @param paramsKey
   * @param keyValues
   * @return
   */
  protected Reader postRequest(URL url, String authToken, String paramsKey,
                               Object... keyValues) {
    StringBuilder postData = new StringBuilder();

    for (int i = 0; i < keyValues.length; i += 2) {
      postData
        .append(urlEncodeSafe(paramsKey.concat("[").concat(keyValues[i].toString()).concat("]")))
        .append('=')
        .append(urlEncodeSafe(keyValues[i + 1].toString()));

      if ((i + 2) < keyValues.length) {
        postData.append('&');
      }
    }

    return postRequest(url, authToken, postData.toString());
  }

  /**
   * Send an HTTP POST request.
   *
   * @param url
   * @param authToken
   * @param data
   * @return
   */
  protected Reader postRequest(URL url, String authToken, String data) {
    try {
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();

      connection.setRequestMethod("POST");
      connection.setDoOutput(true);
      connection.setDoInput(true);
      setHeaders(connection, authToken);

      DataOutputStream out = new DataOutputStream(connection.getOutputStream());
      out.write(data.getBytes());
      out.flush();
      out.close();

      connection.connect();

      // TODO: figure this out... which response codes are actually errors?
      if (connection.getResponseCode() <= 350) {
        return new InputStreamReader(connection.getInputStream());
      }
      else {
        LOG.info("Failed to post request. Server returned: " + connection.getResponseCode());

        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
        String line = reader.readLine();

        while (line != null) {
          LOG.info(line);
          line = reader.readLine();
        }

        throw new RuntimeException("couldn't post request!");
      }
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Builds raw POST data from request params.
   *
   * @param params
   * @return
   */
  protected String buildPostData(Multimap<? extends RequestGenerator.RequestParam, Object> params) {
    StringBuilder b = new StringBuilder();

    for (Map.Entry<? extends RequestGenerator.RequestParam, Object> entry : params.entries()) {
      String key = entry.getKey().getKey();
      String value = entry.getValue() != null ? entry.getValue().toString() : null;

      if (value != null) {
        b.append(urlEncodeSafe(key))
          .append('=')
          .append(urlEncodeSafe(value))
          .append('&');
      }
    }

    if (b.length() > 0) {
      b.setLength(b.length() - 1);
    }

    return b.toString();
  }

  /**
   * Send an HTTP GET request.
   *
   * @param url
   * @param authToken
   * @return
   */
  protected Reader makeRequest(URL url, String authToken) {
    try {
      URLConnection connection = url.openConnection();

      connection.setDoOutput(true);
      connection.setDoInput(true);
      setHeaders(connection, authToken);
      connection.connect();

      return new InputStreamReader(connection.getInputStream());
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Sets headers for a given HTTP request. These include the user agent, auth token, and some
   * metadata required by Zynga.
   *
   * @param connection
   * @param authToken
   */
  protected void setHeaders(URLConnection connection, String authToken) {
    connection.setRequestProperty(PRODUCT_HEADER_NAME, DEFAULT_PRODUCT);
    connection.setRequestProperty(AUTH_TOKEN_HEADER_NAME, authToken);
    connection.setRequestProperty("X-Requested-With", "XMLHttpRequest");
    connection.setRequestProperty("game_type", "WordGame");
    connection.setRequestProperty("poll_type", "timer");
    connection.setRequestProperty("User-Agent", USER_AGENT);
    connection.setRequestProperty("Referer", "https://wwf-fb.zyngawithfriends.com/");
  }

  /**
   * Return the URL to be used when getting a partial game index
   *
   * @param formattedTimestamp
   * @return
   */
  private static URL getGamesWithUpdatesUrl(String formattedTimestamp) {
    return getUrl(BASE_URL + "games.json?get_current_user=true&games_since=" + urlEncodeSafe(formattedTimestamp) );
  }

  /**
   * Return the URL to be used for a dictionary lookup request.
   *
   * @param wordsRequest
   * @return
   */
  protected static URL getDictionaryLookupUrl( String wordsRequest ) {
    return getUrl(BASE_URL + "word_or_not/" + wordsRequest);
  }

  /**
   * Get the URL to be used when requesting chats for a game.
   *
   * @param gameId
   * @return
   */
  protected static URL getUnreadChatsUrl(long gameId) {
    return getUrl(BASE_URL + "games/" + gameId + "/chat_messages/unseen.json");
  }

  /**
   * Get the URL used when submitting a move.
   *
   * @return
   */
  protected static URL getMoveUrl() {
    return getUrl(BASE_URL + "moves.json");
  }

  /**
   * Get the URL to be used when requesting the game index.
   *
   * @return
   */
  protected static URL getIndexUrl() {
    return getUrl(BASE_URL + "games.json");
  }

  /**
   * Get the URL to be used when requesting all data for a game.
   *
   * @param id
   * @return
   */
  protected static URL getGameUrl(long id) {
    return getUrl(BASE_URL + "games/" + id + ".json");
  }

  /**
   * Get the URL to be used when submitting a chat message.
   *
   * @return
   */
  protected static URL getChatUrl() {
    return getUrl(BASE_URL + "chat_messages.json");
  }

  /**
   * Wrap a URI represented as a string in a URL object.
   *
   * @param url
   * @return
   */
  protected static URL getUrl(String url) {
    try {
      return new URL(url);
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * urlencode data
   *
   * @param data
   * @return
   */
  protected static String urlEncodeSafe(String data) {
    try {
      return URLEncoder.encode(data, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }
}
