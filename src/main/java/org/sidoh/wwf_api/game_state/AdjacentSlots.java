package org.sidoh.wwf_api.game_state;

import org.sidoh.wwf_api.types.game_state.Slot;

/**
 * Convenience class for accessing slots adjacent to a given slot.
 */
public class AdjacentSlots {
  private Slot center;
  private Slot topLeft;
  private Slot topMiddle;
  private Slot topRight;
  private Slot left;
  private Slot right;
  private Slot bottomLeft;
  private Slot bottomMiddle;
  private Slot bottomRight;

  private AdjacentSlots() { }

  protected AdjacentSlots(Slot center,
                       Slot topLeft, Slot topMiddle, Slot topRight,
                       Slot left, Slot right,
                       Slot bottomLeft, Slot bottomMiddle, Slot bottomRight) {
    this.center = center;
    this.topLeft = topLeft;
    this.topMiddle = topMiddle;
    this.topRight = topRight;
    this.left = left;
    this.right = right;
    this.bottomLeft = bottomLeft;
    this.bottomMiddle = bottomMiddle;
    this.bottomRight = bottomRight;
  }

  protected AdjacentSlots setCenter(Slot center) {
    this.center = center;
    return this;
  }

  protected AdjacentSlots setTopLeft(Slot topLeft) {
    this.topLeft = topLeft;
    return this;
  }

  protected AdjacentSlots setTopMiddle(Slot topMiddle) {
    this.topMiddle = topMiddle;
    return this;
  }

  protected AdjacentSlots setTopRight(Slot topRight) {
    this.topRight = topRight;
    return this;
  }

  protected AdjacentSlots setLeft(Slot left) {
    this.left = left;
    return this;
  }

  protected AdjacentSlots setRight(Slot right) {
    this.right = right;
    return this;
  }

  protected AdjacentSlots setBottomLeft(Slot bottomLeft) {
    this.bottomLeft = bottomLeft;
    return this;
  }

  protected AdjacentSlots setBottomMiddle(Slot bottomMiddle) {
    this.bottomMiddle = bottomMiddle;
    return this;
  }

  protected AdjacentSlots setBottomRight(Slot bottomRight) {
    this.bottomRight = bottomRight;
    return this;
  }

  public Slot center() {
    return center;
  }

  public Slot topLeft() {
    return topLeft;
  }

  public Slot topMiddle() {
    return topMiddle;
  }

  public Slot topRight() {
    return topRight;
  }

  public Slot left() {
    return left;
  }

  public Slot right() {
    return right;
  }

  public Slot bottomLeft() {
    return bottomLeft;
  }

  public Slot bottomMiddle() {
    return bottomMiddle;
  }

  public Slot bottomRight() {
    return bottomRight;
  }

  public boolean hasAnyTouching() {
    return (left() != null && left().getTile() != null)
      || (topMiddle() != null && topMiddle().getTile() != null)
      || (right() != null && right().getTile() != null)
      || (bottomMiddle() != null && bottomMiddle().getTile() != null);
  }

  public static AdjacentSlots adjacentTo(Slot center) {
    return new AdjacentSlots().setCenter(center);
  }
}
