package org.sidoh.wwf_api.game_state;

import java.util.ArrayList;
import java.util.List;

public class TileBagBuilder {
  private final List<TileBuilder> tiles;

  public TileBagBuilder() {
    this.tiles = new ArrayList<TileBuilder>();
  }

  public TileBagBuilder addLetter(String letter, int value, int duplicity) {
    for (int i = 1; i <= duplicity; i++) {
      tiles.add(new TileBuilder(tiles.size(), letter, value));
    }
    return this;
  }

  public List<TileBuilder> getTiles() {
    return new ArrayList<TileBuilder>(tiles);
  }
}
