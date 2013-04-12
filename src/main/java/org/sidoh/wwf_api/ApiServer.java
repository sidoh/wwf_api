package org.sidoh.wwf_api;

import org.apache.thrift.TException;
import org.apache.thrift.server.THsHaServer;
import org.apache.thrift.server.TServer;
import org.apache.thrift.transport.TNonblockingServerSocket;
import org.apache.thrift.transport.TTransportException;
import org.sidoh.wwf_api.types.api.ChatMessage;
import org.sidoh.wwf_api.types.api.GameIndex;
import org.sidoh.wwf_api.types.api.GameState;
import org.sidoh.wwf_api.types.api.GameType;
import org.sidoh.wwf_api.types.api.MoveSubmission;
import org.sidoh.wwf_api.types.api.NewGameParams;
import org.sidoh.wwf_api.types.api.WwfApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

public class ApiServer {
  private static final Logger LOG = LoggerFactory.getLogger(ApiServer.class);

  private static class WwfApiHandler implements WwfApi.Iface {
    private static final ApiProvider PROVIDER = new ApiProvider();

    @Override
    public List<ChatMessage> getUnseenChats(String accessToken, long gameId) throws TException {
      return PROVIDER.getUnreadChats(accessToken, gameId);
    }

    @Override
    public List<String> dictionaryLookup(String accessToken, List<String> words) throws TException {
      return PROVIDER.dictionaryLookup(accessToken, words);
    }

    @Override
    public GameIndex getGameIndex(String accessToken) throws TException {
      return PROVIDER.getGameIndex(accessToken);
    }

    @Override
    public GameIndex getGamesWithUpdates(String accessToken, int timestamp) throws TException {
      return PROVIDER.getGamesWithUpdates(accessToken, timestamp);
    }

    @Override
    public GameState getGameState(String accessToken, long gameId) throws TException {
      return PROVIDER.getGameState(accessToken, gameId);
    }

    @Override
    public GameState makeMove(String accessToken, GameState currentState, MoveSubmission move) throws TException {
      return PROVIDER.makeMove(accessToken, currentState, move );
    }

    @Override
    public void createMatchmakingGame(String accessToken, NewGameParams params) throws TException {
      if (params.getGameType() == GameType.MATCHMAKING) {
        PROVIDER.createRandomGame(accessToken);
      }
      else if (params.getGameType() == GameType.SEARCH) {
        if (params.getParams().isSetFbId()) {
          PROVIDER.createFacebookGame(accessToken, params.getParams().getFbId());
        }
        else {
          PROVIDER.createZyngaGame(accessToken, params.getParams().getZyngaId());
        }
      }
      else {
        throw new RuntimeException("unsupported game type: " + params.getGameType());
      }
    }

    @Override
    public ChatMessage sendChatMessage(String accessToken, long gameId, String message) throws TException {
      return PROVIDER.submitChatMessage(accessToken, gameId, message);
    }
  }

  public static void main(String[] args) throws TTransportException {
    int port = Integer.parseInt(args[0]);

    WwfApi.Iface handler = new WwfApiHandler();
    WwfApi.Processor<WwfApi.Iface> processor = new WwfApi.Processor<WwfApi.Iface>(handler);
    TNonblockingServerSocket transport = new TNonblockingServerSocket(port);
    TServer server = new THsHaServer(new THsHaServer.Args(transport).processor(processor));

    LOG.info("starting server on port {}", port);

    server.serve();
  }
}
