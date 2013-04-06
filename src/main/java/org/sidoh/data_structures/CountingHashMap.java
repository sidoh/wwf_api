package org.sidoh.data_structures;

import java.util.HashMap;
import java.util.Map;

public class CountingHashMap<T> extends HashMap<T, CountingHashMap.CountingInteger> {

  @Override
  public CountingInteger get(Object key) {
    CountingInteger value = super.get(key);

    if (value == null) {
      value = new CountingInteger();
      super.put((T) key, value);
    }

    return value;
  }

  public void increment(Object key) {
    get(key).increment();
  }

  public void increment(Object key, int by) {
    get(key).increment(by);
  }

  public Map<T, Integer> getCounts() {
    Map<T, Integer> r = new HashMap<T, Integer>();

    for (Map.Entry<T, CountingInteger> entry : entrySet()) {
      r.put(entry.getKey(), entry.getValue().getValue());
    }

    return r;
  }

  public static final class CountingInteger {
    private int value = 0;

    public void increment() {
      value++;
    }

    public int getValue() {
      return value;
    }

    public void increment(int by) {
      value += by;
    }
  }
}
