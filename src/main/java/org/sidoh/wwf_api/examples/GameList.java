package org.sidoh.wwf_api.examples;

import org.sidoh.wwf_api.AccessTokenRetriever;
import org.sidoh.wwf_api.ApiProvider;
import org.sidoh.wwf_api.StatefulApiProvider;
import org.sidoh.wwf_api.game_state.GameStateHelper;
import org.sidoh.wwf_api.game_state.WordsWithFriendsBoard;
import org.sidoh.wwf_api.types.api.GameIndex;
import org.sidoh.wwf_api.types.api.GameMeta;
import org.sidoh.wwf_api.types.api.GameState;
import org.sidoh.wwf_api.types.api.User;
import org.sidoh.wwf_api.types.game_state.BoardStorage;

import java.io.Console;
import java.io.IOException;

public class GameList {
  public static void main(String[] args) throws IOException {
    Console console = System.console();

    // Retrieve access token from facebook
    System.out.println("Before continuing, please enter your Facebook credentials.");
    AccessTokenRetriever tokenRetriever = new AccessTokenRetriever();
    String accessToken = tokenRetriever.promptForAccessToken();

    if ( accessToken != null ) {
      System.out.println("Successfully retrieved access token: " + accessToken.substring(0,10) + "...");
    }
    else {
      System.out.println("Couldn't retrieve access token. Please ensure that you've authorized the WWF app on facebook.");
      System.exit(1);
    }

    // Retrieve list of games and display the game state for each
    StatefulApiProvider api = new StatefulApiProvider(accessToken);
    GameStateHelper helper = GameStateHelper.getInstance();

    GameIndex index = api.getGameIndex();
    User me = index.getUser();

    for (GameMeta gameMeta : index.getGames()) {
      GameState state = api.getGameState(gameMeta.getId());

      User opponent = helper.getOtherUser(me, state);

      System.out.println("Game state : " + (gameMeta.isOver() ? "OVER" : "OPEN"));
      System.out.printf("Game  : %20s vs. %20s\n", me.getName(), opponent.getName());
      System.out.printf("Score : %20d to  %20d\n", helper.getScore(me, state), helper.getScore(opponent, state));

      WordsWithFriendsBoard board = new WordsWithFriendsBoard(new BoardStorage(state.getBoard()));
      System.out.println(board);

      System.out.println("----------------------------------------");
    }
  }
}
