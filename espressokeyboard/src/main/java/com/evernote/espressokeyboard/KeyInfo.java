/**
 * Copyright (c) 2015 Evernote Corporation. All rights reserved.
 */
package com.evernote.espressokeyboard;

import android.view.KeyEvent;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by paour on 03/08/15.
 */
public class KeyInfo {
  public JSONObject toJson() {
    try {
      return new JSONObject()
          .put("absolute_x", absoluteX)
          .put("absolute_y", absoluteY)
          .put("key", character)
          ;
    } catch (JSONException e) {
      e.printStackTrace();
      return new JSONObject();
    }
  }

  public enum Type {
    STANDARD,
    SPECIAL,
    COMPLETION
  }

  public int absoluteX, absoluteY;
  public String character;
  public int keyCode;
  public Type type;

  public KeyInfo(int absoluteX, int absoluteY, String character) {
    this.absoluteX = absoluteX;
    this.absoluteY = absoluteY;

    if (character.equals("\n")) {
      this.keyCode = KeyEvent.KEYCODE_ENTER;
      this.character = KeyEvent.keyCodeToString(keyCode);
      type = Type.SPECIAL;
    } else {
      this.character = character.toLowerCase();
      type = Type.STANDARD;
    }
  }

  public KeyInfo(String character) {
    this(0, 0, character);
  }

  public KeyInfo(int absoluteX, int absoluteY, int keyCode) {
    this.absoluteX = absoluteX;
    this.absoluteY = absoluteY;
    this.keyCode = keyCode;
    character = KeyEvent.keyCodeToString(keyCode);
    type = Type.SPECIAL;
  }

  public KeyInfo(int absoluteX, int absoluteY) {
    this.absoluteX = absoluteX;
    this.absoluteY = absoluteY;
    type = Type.COMPLETION;
    character = Type.COMPLETION.name();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    KeyInfo keyInfo = (KeyInfo) o;

    return keyCode == keyInfo.keyCode
        && !(character != null ? !character.equals(keyInfo.character) : keyInfo.character != null)
        && type == keyInfo.type;
  }

  @Override
  public int hashCode() {
    int result = (character != null ? character.hashCode() : 0);
    result = 31 * result + keyCode;
    result = 31 * result + type.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "KeyInfo{" +
        "absoluteX=" + absoluteX +
        ", absoluteY=" + absoluteY +
        ", character='" + character + '\'' +
        ", keyCode=" + keyCode +
        ", type=" + type +
        '}';
  }

  public String description() {
    switch (type) {
      case STANDARD:
      case SPECIAL:
        return "key '" + character + "'";

      case COMPLETION:
        return "completion slot";

      default:
        return "Unknown";
    }
  }
}
