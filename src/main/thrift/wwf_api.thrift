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
  1: required string createdAt,
  2: required i64 createdByUserId,
  3: optional i64 currentMoveUserId,
  4: required i64 id,
  5: required bool matchmaking,
  6: required bool over,
  7: optional MoveData lastMove,
  8: required bool opponentPresent,
  9: required i64 randomSeed,
  10: required map<i64, User> usersById,
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
  1: required list<GameMeta> games,
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
  GameIndex getGameIndex(1: string accessToken),
  GameState getGameState(1: string accessToken, 2: i64 gameId),
  GameState makeMove(1: string accessToken, 2: GameState currentState, 3: MoveSubmission move),
  void createMatchmakingGame(1: string accessToken, 2: NewGameParams params),
  ChatMessage sendChatMessage(1: string accessToken, 2: i64 gameId, 3: string message),
  list<ChatMessage> getUnseenChats(1: string accessToken, 2: i64 gameId),

  /**
   * Check whether or not the provided words are in the WWF dictionary.
   *
   * @param words set of words to be checked
   * @return set of words that are NOT in the WWF dictionary.
   */
  list<string> dictionaryLookup(1: string accessToken, 2: list<string> words)
}
