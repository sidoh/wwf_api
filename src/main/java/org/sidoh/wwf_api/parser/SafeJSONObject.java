package org.sidoh.wwf_api.parser;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONStyle;

import java.io.IOException;
import java.util.Map;

/**
 * Wraps standard JSONObject and throws exceptions if requested keys are not present
 */
public class SafeJSONObject extends JSONObject {
  private final JSONObject wrapped;

  public SafeJSONObject(JSONObject wrapped) {
    this.wrapped = wrapped;
  }

  public static String escape(String s) {
    return JSONObject.escape(s);
  }

  public static String toJSONString(Map<String, ? extends Object> map) {
    return JSONObject.toJSONString(map);
  }

  public static String toJSONString(Map<String, ? extends Object> map, JSONStyle compression) {
    return JSONObject.toJSONString(map, compression);
  }

  public static void writeJSONKV(String key, Object value, Appendable out, JSONStyle compression) throws IOException {
    JSONObject.writeJSONKV(key, value, out, compression);
  }

  public static void writeJSON(Map<String, Object> map, Appendable out) throws IOException {
    JSONObject.writeJSON(map, out);
  }

  public static void writeJSON(Map<String, ? extends Object> map, Appendable out, JSONStyle compression) throws IOException {
    JSONObject.writeJSON(map, out, compression);
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

  public static JSONObject merge(JSONObject o1, Object o2) {
    return JSONObject.merge(o1, o2);
  }

  public static JSONArray merge(JSONArray o1, Object o2) {
    return JSONObject.merge(o1, o2);
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
