/**
 * Copyright (c) 2015 Evernote Corporation. All rights reserved.
 */
package com.evernote.espressokeyboard;

import android.view.KeyEvent;

/**
 * Created by paour on 14/09/15.
 */
public class Key {
  public enum Type {
    STANDARD,
    SPECIAL,
    COMPLETION
  }

  /** either the key character, or a transcription of the keycode **/
  private String character;
  private int keyCode;
  private Type type;

  public Key(String character, int keyCode, Type type) {
    this.character = character;
    this.keyCode = keyCode;
    this.type = type;
  }

  public static Key getCharacter(String character) {
    if (character.equals("\n")) {
      return new Key(KeyEvent.keyCodeToString(KeyEvent.KEYCODE_ENTER), KeyEvent.KEYCODE_ENTER, Type.SPECIAL);
    } else {
      return new Key(character.toLowerCase(), 0, Type.STANDARD);
    }
  }

  public static Key getSpecial(int keyCode) {
    return new Key(KeyEvent.keyCodeToString(keyCode), keyCode, Type.SPECIAL);
  }

  public static Key getCompletion() {
    return new Key(Type.COMPLETION.name(), 0, Type.COMPLETION);
  }

  public String getCharacter() {
    return character;
  }

  public int getKeyCode() {
    return keyCode;
  }

  public Type getType() {
    return type;
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

  @Override
  public int hashCode() {
    int result = (character != null ? character.hashCode() : 0);
    result = 31 * result + keyCode;
    result = 31 * result + type.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Key key = (Key) o;

    return keyCode == key.keyCode
        && (character != null ? character.equals(key.character) : key.character == null)
        && type == key.type;
  }
}
