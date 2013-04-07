package org.sidoh;

import org.sidoh.random.MersenneTwister;
import junit.framework.TestCase;

public class TestMersenneTwister extends TestCase {
  public void testWorks() {
    MersenneTwister twister = new MersenneTwister();
    twister.setSeed(123);

    assertEquals(2991312382L, twister.nextUnsignedI32());

    twister.setSeed(768248463L);

    assertEquals(4023294457L, twister.nextUnsignedI32());
  }
}
