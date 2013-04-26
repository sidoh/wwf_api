package org.sidoh.wwf_api.parser;

import net.minidev.json.JSONObject;
import net.minidev.json.JSONStyle;

import java.io.IOException;

/**
 * Wraps standard JSONObject and throws exceptions if requested keys are not present
 */
public class SafeJSONObject extends JSONObject {
  private final JSONObject wrapped;

  public SafeJSONObject(JSONObject wrapped) {
    this.wrapped = wrapped;
  }

  @Override
  public void writeJSONString(Appendable out) throws IOException {
    wrapped.writeJSONString(out);
  }

  @Override
  public void writeJSONString(Appendable out, JSONStyle compression) throws IOException {
    wrapped.writeJSONString(out, compression);
  }

  @Override
  public void merge(Object o2) {
    wrapped.merge(o2);
  }

  @Override
  public String toJSONString() {
    return wrapped.toJSONString();
  }

  @Override
  public String toJSONString(JSONStyle compression) {
    return wrapped.toJSONString(compression);
  }

  @Override
  public String toString(JSONStyle compression) {
    return wrapped.toString(compression);
  }

  @Override
  public String toString() {
    return wrapped.toString();
  }

  @Override
  public Object get(Object o) {
    if (! wrapped.containsKey(o)) {
      throw new ParserException("Required key '" + o + "' missing from object");
    }

    return wrapped.get(o);
  }
}
