package org.sidoh.wwf_api.game_state;

import org.sidoh.wwf_api.WwfApiTestCase;
import org.sidoh.wwf_api.types.game_state.WordOrientation;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class TestMove extends WwfApiTestCase {
  public void testEquals() {
    Move move1 = new Move(
      Arrays.asList(
        TileBuilder.getTile("B", 1),
        TileBuilder.getTile("O", 2),
        TileBuilder.getTile("B", 3)
      ),
      7, 7,
      WordOrientation.HORIZONTAL
    );
    Move move2 = new Move(
      Arrays.asList(
        TileBuilder.getTile("B", 6),
        TileBuilder.getTile("O", 8),
        TileBuilder.getTile("B", 1)
      ),
      7, 7,
      WordOrientation.HORIZONTAL
    ).setResult(new Move.Result(10, 1, "BOB", Collections.singletonList("BOB")));

    assertTrue("two moves with same letters, play position, and orientation should be equal",
      move1.equals(move2));
    assertTrue("two moves with same letters, play position, and orientation should have the same hashcode",
      move1.hashCode() == move2.hashCode());

    Set<Move> moveSet = new HashSet<Move>();
    moveSet.add(move1);
    moveSet.add(move2);

    assertTrue("after inserting the same move twice, set should only contain one move",
      1 == moveSet.size());
  }
}
