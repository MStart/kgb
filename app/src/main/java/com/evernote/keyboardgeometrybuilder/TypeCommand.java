/**
 * Copyright (c) 2015 Evernote Corporation. All rights reserved.
 */
package com.evernote.keyboardgeometrybuilder;

import android.util.Log;

import com.evernote.espressokeyboard.Key;
import com.evernote.espressokeyboard.KeyInfo;

/**
 * Created by paour on 05/08/15.
 */
public class TypeCommand implements TouchCommand {
  public static final String TAG = "TypeCommand";
  private final KeyboardGeometry kgb;
  private final String text;
  private final Consumer<String> callback;
  private int index;
  private StringBuilder textReceived = new StringBuilder();

  public TypeCommand(KeyboardGeometry kgb, String text, Consumer<String> callback) {
    this.kgb = kgb;
    this.text = text;
    this.callback = callback;

    Log.d(TAG, "Typing " + text);
  }

  @Override
  public String getShellCommand() {
    KeyInfo existingKey = kgb.foundKeys.get(Key.getCharacter(Character.toString(text.charAt(index++))));

    if (existingKey != null) {
      return "input tap " + existingKey.getLocation().getAbsoluteX() + " " + existingKey.getLocation().getAbsoluteY();
    } else {
      return null;
    }
  }

  @Override
  public void setTextReceived(String charReceived) {
    Log.d(TAG, "setTextReceived " + charReceived);
    textReceived.append(charReceived);
    onDone();
  }

  @Override
  public void setKeyReceived(String textReceived, int keyReceived) {
    Log.d(TAG, "setKeyReceived " + keyReceived);
    onDone();
  }

  @Override
  public void onNothingReceived() {
    Log.d(TAG, "onNothingReceived");
    onDone();
  }

  public void onDone() {
    if (text.length() == index) {
      callback.accept(textReceived.toString());
    } else {
      kgb.addCommand(this);
    }
  }
}
