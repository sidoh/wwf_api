namespace java org.sidoh.wwf_api.types.api

include "game_state.thrift"

struct User {
  1: optional i64 fbId,
  2: required i64 id,
  3: required string name
}

enum MoveType {
  RESIGN = 1,
  PLAY = 2,
  SWAP = 3,
  PASS = 4,
  DECLINE = 5,
  GAME_OVER = 6,
  TIE = 7
}

struct ChatMessage {
  1: required i64 id,
  2: required i64 userId,
  3: required i64 gameId,
  4: required i32 code,
  5: required string message,
  6: required string createdAt
}

struct Coordinates {
  1: required i32 x,
  2: required i32 y
}

/**
 * Encapsulates metadata about a single move. This will represent moves that are
 * included as part of the game state.
 */
struct MoveData {
  1: required i32 boardChecksum,
  2: required string createdAt,
  3: optional Coordinates playStartPosition,
  4: optional Coordinates playEndPosition,
  5: required i64 gameId,
  6: required i64 id,
  7: required i32 moveIndex,
  8: required MoveType moveType,
  9: optional i32 points,
  10: optional i32 promoted,
  11: required list<string> words,
  12: optional string text,

  /**
   * If the move type was PLAY or SWAP, this will contain a list of tiles that
   * were either played or swapped.
   */
  13: optional list<game_state.Tile> tiles
}

/**
 * This class should be used to submit a move to the API. Note that you are not
 * required to compute the checksum, etc., so it's easier to use.
 */
struct MoveSubmission {
  1: required MoveType type,

  /**
   * Only necessary for PLAY. The (x, y) coordinates for the first tile played
   * (either left-to-right or top-to-bottom).
   */
  2: optional Coordinates playStart,

  /**
   * Only necessary for PLAY. The orientation of the word played (either vertical
   * or horizontal).
   */
  3: optional game_state.WordOrientation orientation,

  /**
   * If MoveType is PLAY or SWAP, then this should be a list of the tiles that were
   * included in the play. Note that this should only ever contain tiles that are in
   * the user's rack. If the move type is PLAY, they should be in the order that the
   * word was formed in, skipping tiles that were already on the board.
   */
  4: optional list<game_state.Tile> tilesPlayed
}

/**
 * Encapsulates metadata about a single game
 */
struct GameMeta {
  /**
   * The date at which the game was created
   */
  1: required string createdAt,

  /**
   * The Zynga user ID of the user who created the game
   */
  2: required i64 createdByUserId,

  /**
   * The Zynga user ID of the user whose turn it currently
   */
  3: optional i64 currentMoveUserId,

  /**
   * The ID of this game
   */
  4: required i64 id,

  /**
   * If true, this game was created by matchmaking
   */
  5: required bool matchmaking,

  /**
   * If true, this game is over (meaning all tiles have been played, or a player has
   * resigned).
   */
  6: required bool over,

  /**
   * Relevant move data for the last played move.
   */
  7: optional MoveData lastMove,

  /**
   * It's a little unclear what this means -- probably indicates whether or not the
   * opponent is currently online.
   */
  8: required bool opponentPresent,

  /**
   * The random seed + user actions entirely determine the game state. This is used 
   * to reconstruct tile draw orders, racks, and so on.
   */
  9: required i64 randomSeed,

  /**
   * A map going from zynga user ID -> User object, which contains some additional
   * data about the user.
   */
  10: required map<i64, User> usersById,

  /**
   * Contains only chats that haven't been read
   */
  11: required list<i64> unreadChatIds
}

/**
 * Gets extended information about a single game
 */
struct GameState {
  1: required i64 id,

  /**
   * A list of all of the moves made.
   */
  2: required list<MoveData> allMoves,

  /**
   * Map telling you the racks for each user (keyed by user id)
   */
  3: required map<i64, list<game_state.Tile>> racks,

  /**
   * The game board. Access (row, column) by row*15 + col. A blank slot will be
   * filled with null. List guaranteed to contain exactly 225 (15 * 15) values.
   */
  4: required list<game_state.Slot> board,

  /**
   * Scores by user ID
   */
  5: required map<i64, i32> scores,

  /**
   * All of the meta-information about this game state
   */
  6: required GameMeta meta,

  /**
   * List of the tiles remaining. The order of the tiles is guaranteed to be in the order that they'll
   * be retreived assuming no SWAP moves, which effectively shuffles the list.
   */
  7: required list<game_state.Tile> remainingTiles,

  /**
   * All chat messages for this game
   */
  8: required list<ChatMessage> chatMessages
}

/**
 * Metadata about a game - includes the user ID of the person making the request.
 */
struct GameIndex {
  /**
   * A list of games. Note that not necessarily all of these will be active (i.e., have moves
   * pending).
   */
  1: required list<GameMeta> games,

  /**
   * This corresponds to the user that requests the game index
   */
  2: required User user
}

enum GameType {
  MATCHMAKING = 1,
  SEARCH = 2
}

/**
 * Search can either be facebook or zynga user id
 */
union SearchGameParams {
  1: i64 zyngaId,
  2: i64 fbId
}

struct NewGameParams {
  1: required GameType gameType,

  /**
   * This only needs to be provided if GameType is search.
   */
  2: optional SearchGameParams params
}

service WwfApi {
  /**
   * Retrieves the full game index. The index contains metadata for each game currently visible to
   * the user requesting it.
   *
   * @param accessToken
   * @return
   */
  GameIndex getGameIndex(1: string accessToken),

  /**
   * Retrieves a partial game index, including only data about games that have had updates since the
   * provided timestamp.
   *
   * @param accessToken
   * @param timestamp
   * @return GameIndex with games having updates occurring after the provided timestamp
   */
  GameIndex getGamesWithUpdates(1: string accessToken, 2: i32 timestamp),

  /**
   * Gets the full game state for the provided game. GameState contains all information about a
   * particular game.
   *
   * @param accessToken
   * @param gameId the ID of the game being requested
   * @return
   */
  GameState getGameState(1: string accessToken, 2: i64 gameId),

  /**
   * Submits a move. 
   *
   * @param accessToken
   * @param currentState the current game state
   * @param move the move being submitted
   * @return the updated game state after making the provided move
   */
  GameState makeMove(1: string accessToken, 2: GameState currentState, 3: MoveSubmission move),

  /**
   * Creates a matchmaking game. Note that the game will immediately be visible in the index, but
   * will not necessarily have an opponent until Zynga assigns one to the game. If inviting a user,
   * the game should immediately have an opponent.
   *
   * @param accessToken
   * @param params 
   */
  void createMatchmakingGame(1: string accessToken, 2: NewGameParams params),

  /**
   * Send a chat message to an opponent for a particular game.
   *
   * @param accessToken
   * @param gameId the game to send the chat message to
   * @param message the message to send
   * @return the ChatMessage object containing the message Zynga persisted
   */
  ChatMessage sendChatMessage(1: string accessToken, 2: i64 gameId, 3: string message),

  /**
   * Gets a list of chat messages for a particular game that haven't been seen yet.
   *
   * @param accessToken
   * @param gameId
   * @return list of unseen chats for a game
   */
  list<ChatMessage> getUnseenChats(1: string accessToken, 2: i64 gameId),

  /**
   * Check whether or not the provided words are in the WWF dictionary.
   *
   * @param words set of words to be checked
   * @return set of words that are NOT in the WWF dictionary.
   */
  list<string> dictionaryLookup(1: string accessToken, 2: list<string> words)
}
