package org.sidoh.wwf_api.game_state;

import org.sidoh.wwf_api.types.game_state.WordOrientation;

/**
 * A convenient way to iterate over slots in play order.
 */
public class SlotIterator implements Iterable<Integer> {
  private int row;
  private int col;
  private final int count;
  private final int dimensions;
  private final WordOrientation orientation;
  private final Direction dir;

  public SlotIterator(int index, int count, int dimensions, WordOrientation orientation, Direction dir) {
    this(index / dimensions, index % dimensions, count, dimensions, orientation, dir);
  }

  public SlotIterator(int startRow, int startCol, int count, int dimensions, WordOrientation orientation, Direction dir) {
    this.row = startRow;
    this.col = startCol;
    this.count = count;
    this.dimensions = dimensions;
    this.orientation = orientation;
    this.dir = dir;
  }

  @Override
  public Iterator iterator() {
    return new Iterator(row, col, count, dimensions, orientation, dir);
  }

  public SlotIterator reverse() {
    return new SlotIterator(row, col, count, dimensions, orientation, dir == Direction.BACKWARDS ? Direction.FORWARDS : Direction.BACKWARDS);
  }

  public SlotIterator setStartPosition(int row, int col) {
    return new SlotIterator(row, col, count, dimensions, orientation, dir);
  }

  public static class Iterator implements java.util.Iterator<Integer> {
    private int row;
    private int col;
    private final int count;
    private final int dimensions;
    private final WordOrientation orientation;
    private final Direction dir;
    private int movesMade;
    private final int startRow;
    private final int startCol;

    public Iterator(int startRow, int startCol, int count, int dimensions, WordOrientation orientation, Direction dir) {
      this.startRow = startRow;
      this.startCol = startCol;
      this.row = startRow;
      this.col = startCol;
      this.count = count;
      this.dimensions = dimensions;
      this.orientation = orientation;
      this.dir = dir;
      this.movesMade = 0;
    }

    public void stepForwards() {
      next(Direction.FORWARDS);
    }

    public void stepBackwards() {
      next(Direction.BACKWARDS);
    }

    public Iterator backToStart() {
      return new Iterator(startRow, startCol, count, dimensions, orientation, dir);
    }

    public Iterator clone() {
      Iterator itr = new Iterator(startRow, startCol, count, dimensions, orientation, dir);
      itr.row = row;
      itr.col = col;
      itr.movesMade = movesMade;

      return itr;
    }

    public Integer offset() {
      if (orientation == WordOrientation.HORIZONTAL) {
        return col - startCol;
      }
      else {
        return row - startRow;
      }
    }

    public Iterator atOffset(int offset) {
      Iterator itr = clone();

      if (orientation == WordOrientation.HORIZONTAL) {
        itr.col = startCol + offset;
      }
      else {
        itr.row = startRow + offset;
      }

      return itr;
    }

    public Iterator withDirection(Direction dir) {
      return new Iterator(startRow, startCol, count, dimensions, orientation, dir);
    }

    public Integer current() {
      return row*dimensions + col;
    }

    @Override
    public boolean hasNext() {
      return row < dimensions && row >= 0 && col < dimensions && col >= 0 && movesMade < count;
    }

    @Override
    public Integer next() {
      if (row >= dimensions || col >= dimensions)
        throw new RuntimeException("Gone past boundaries.");

      int ret = row*dimensions + col;

      next(dir);

      return ret;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }

    private void next(Direction dir) {
      if (orientation == WordOrientation.HORIZONTAL) {
        col += dir.getMoveValue();
      }
      else if (orientation == WordOrientation.VERTICAL) {
        row += dir.getMoveValue();
      }
      else throw new RuntimeException("unsupported orientation");
    }
  }
}
