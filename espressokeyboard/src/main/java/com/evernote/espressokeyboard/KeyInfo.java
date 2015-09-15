/**
 * Copyright (c) 2015 Evernote Corporation. All rights reserved.
 */
package com.evernote.espressokeyboard;

import android.graphics.Point;
import android.support.annotation.NonNull;
import android.view.KeyEvent;

import com.evernote.espressokeyboard.Key.Type;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by paour on 03/08/15.
 */
public class KeyInfo {
  private KeyLocation location;
  private Key key;

  public static final String ABSOLUTE_X = "absolute_x";
  public static final String ABSOLUTE_Y = "absolute_y";
  public static final String KEY = "key";

  public KeyInfo(@NonNull JSONObject jsonObject, @NonNull Point translate, float scaleX, float scaleY) throws JSONException {
    int absoluteX = jsonObject.getInt(ABSOLUTE_X);
    int absoluteY = jsonObject.getInt(ABSOLUTE_Y);

    absoluteX = (int) (absoluteX * scaleX + translate.x);
    absoluteY = (int) (absoluteY * scaleY + translate.y);

    location = new KeyLocation(absoluteX, absoluteY);

    String character = jsonObject.getString(KEY);
    int keyCode = KeyEvent.keyCodeFromString(character);
    Type type;

    if (character.equals(Type.COMPLETION.name())) {
      type = Type.COMPLETION;
    } else if (keyCode != KeyEvent.KEYCODE_UNKNOWN && !character.equals("" + keyCode)) {
      type = Type.SPECIAL;
    } else {
      type = Type.STANDARD;
      keyCode = KeyEvent.KEYCODE_UNKNOWN;
    }

    this.key = new Key(character, keyCode, type);
  }

  public JSONObject toJson() {
    try {
      return new JSONObject()
          .put(ABSOLUTE_X, location.getAbsoluteX())
          .put(ABSOLUTE_Y, location.getAbsoluteY())
          .put(KEY, key.getCharacter())
          ;
    } catch (JSONException e) {
      e.printStackTrace();
      return new JSONObject();
    }
  }

  private KeyInfo(KeyLocation location, Key key) {
    this.location = location;
    this.key = key;
  }

  public static KeyInfo getCharacterAt(int absoluteX, int absoluteY, String character) {
    return new KeyInfo(
        new KeyLocation(absoluteX, absoluteY),
        Key.getCharacter(character));
  }

  public static KeyInfo getSpecialAt(int absoluteX, int absoluteY, int keyCode) {
    return new KeyInfo(
        new KeyLocation(absoluteX, absoluteY),
        Key.getSpecial(keyCode));
  }

  public static KeyInfo getCompletionAt(int absoluteX, int absoluteY) {
    return new KeyInfo(
        new KeyLocation(absoluteX, absoluteY),
        Key.getCompletion());
  }

  public KeyLocation getLocation() {
    return location;
  }

  public Key getKey() {
    return key;
  }

  public void averageLocationWith(KeyInfo other) {
    location = KeyLocation.average(location, other.location);
  }

  @Override
  public String toString() {
    return "KeyInfo{" +
        "absoluteX=" + location.getAbsoluteX() +
        ", absoluteY=" + location.getAbsoluteY() +
        ", character='" + key.getCharacter() + '\'' +
        ", keyCode=" + key.getKeyCode() +
        ", type=" + key.getType() +
        '}';
  }
}
