/**
 * Copyright (c) 2015 Evernote Corporation. All rights reserved.
 */
package com.evernote.keyboardgeometrybuilder;

import android.view.KeyEvent;

/**
 * Created by paour on 03/08/15.
 */
public class KeyInfo {
  public enum Type {
    STANDARD,
    SPECIAL,
    COMPLETION
  }

  int absoluteX, absoluteY;
  String character;
  int keyCode;
  Type type;

  public KeyInfo(int absoluteX, int absoluteY, String character) {
    this.absoluteX = absoluteX;
    this.absoluteY = absoluteY;
    this.character = character.toLowerCase();
    type = Type.STANDARD;
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
