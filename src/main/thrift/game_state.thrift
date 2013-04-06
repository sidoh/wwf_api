namespace java org.sidoh.wwf_api.types.game_state

enum WordOrientation {
  HORIZONTAL = 1,
  VERTICAL = 2
}

struct Letter {
  /**
   * Value of the letter. e.g., "a"
   */
  1: required string value
}

struct Tile {
  /**
   * ID for this tile. Used to differentiate this tile from one with the same
   * letter.
   */
  1: required i32 id,

  /**
   * The letter represented by this tile
   */
  2: required Letter letter,

  /**
   * THe number of points this tile is worth
   */
  3: required i32 value
}

enum SlotModifier {
  NONE = 0,
  DOUBLE_LETTER = 1,
  DOUBLE_WORD = 2,
  TRIPLE_LETTER = 3,
  TRIPLE_WORD = 4
}

struct Slot {
  /**
   * A slot can have a modifier
   */
  1: required SlotModifier modifier,

  /**
   * A slot can have a tile placed on it (but won't always have one)
   */
  2: optional Tile tile
}

struct Rack {
  /**
   * The maximum number of tiles for this rack
   */
  1: required i32 capacity,

  /**
   * A list of tiles associated with this rack
   */
  2: required list<Tile> tiles
}

struct BoardStorage {
  /** 
   * A list of all of the slots available for play
   */
  1: required list<Slot> slots
}
