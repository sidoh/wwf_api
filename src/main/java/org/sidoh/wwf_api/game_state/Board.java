package org.sidoh.wwf_api.game_state;

import org.sidoh.wwf_api.types.game_state.BoardStorage;
import org.sidoh.wwf_api.types.game_state.Slot;
import org.sidoh.wwf_api.types.game_state.SlotModifier;

import java.util.ArrayList;
import java.util.List;

public abstract class Board {

  public static abstract class Builder {
    public abstract Board build();
    public abstract Board build(BoardStorage storage);
  }

  protected final int size;
  protected final BoardStorage storage;

  public Board(int size, Iterable<SlotBuilder> initialSlots) {
    this.size = size;
    this.storage = new BoardStorage();

    for (SlotBuilder initialSlot : initialSlots) {
      storage.addToSlots(initialSlot.build());
    }
  }

  public Board(BoardStorage storage) {
    this.size = storage.getSlotsSize();
    this.storage = storage;
  }

  public BoardStorage getStorage() {
    return storage;
  }

  protected Slot getSlotSafe(int index) {
    if (index < 0 || index >= storage.getSlotsSize())
      return null;

    return storage.getSlots().get(index);
  }

  /**
   * lol
   *
   * @return
   */
  public boolean hasTiles() {
    for (Slot slot : storage.getSlots()) {
      if ( slot.getTile() != null ) {
        return true;
      }
    }
    return false;
  }

  protected Slot getSlot(int index) {
    if (index < 0 || index >= storage.getSlotsSize())
      throw new IllegalArgumentException("tried to access out of bounds slot");

    return storage.getSlots().get(index);
  }

  protected static class SlotBuilder {
    private final SlotModifier modifier;

    private static final SlotBuilder NO_MODIFIER = new SlotBuilder(SlotModifier.NONE);
    private static final SlotBuilder DOUBLE_LETTER = new SlotBuilder(SlotModifier.DOUBLE_LETTER);
    private static final SlotBuilder DOUBLE_WORD = new SlotBuilder(SlotModifier.DOUBLE_WORD);
    private static final SlotBuilder TRIPLE_LETTER = new SlotBuilder(SlotModifier.TRIPLE_LETTER);
    private static final SlotBuilder TRIPLE_WORD = new SlotBuilder(SlotModifier.TRIPLE_WORD);

    public SlotBuilder(SlotModifier modifier) {
      this.modifier = modifier;
    }

    public Slot build() {
      return new Slot().setModifier(modifier);
    }
  }

  protected static Iterable<SlotBuilder> getSlotBuilders(String serializedBoard) {
    List<SlotBuilder> builders = new ArrayList<SlotBuilder>();

    for (int i = 0; i < serializedBoard.length(); i++) {
      char id = serializedBoard.charAt(i);

      if (id == 'N') builders.add(SlotBuilder.NO_MODIFIER);
      else if (id == 'd') builders.add(SlotBuilder.DOUBLE_LETTER);
      else if (id == 'D') builders.add(SlotBuilder.DOUBLE_WORD);
      else if (id == 't') builders.add(SlotBuilder.TRIPLE_LETTER);
      else if (id == 'T') builders.add(SlotBuilder.TRIPLE_WORD);
      else throw new InvalidGameStateException("Invalid slot type: " + id);
    }

    return builders;
  }

  public static WordsWithFriendsBoard.Builder wordsWithFriendsBoard() {
    return new WordsWithFriendsBoard.Builder();
  }
}
