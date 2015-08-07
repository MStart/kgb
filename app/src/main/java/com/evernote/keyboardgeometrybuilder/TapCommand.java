/**
 * Copyright (c) 2015 Evernote Corporation. All rights reserved.
 */
package com.evernote.keyboardgeometrybuilder;

import android.util.Log;

/**
 * Created by paour on 03/08/15.
 */
public abstract class TapCommand implements TouchCommand {
  public static final String TAG = "TouchCommand";

  int x;
  int y;
  String textReceived;
  int keyReceived = -1;
  KeyboardGeometry kgb;

  public TapCommand(KeyboardGeometry kgb, int x, int y) {
    this.kgb = kgb;
    this.x = x;
    this.y = y;
  }

  @Override
  public String getShellCommand() {
    return "input tap " + x + " " + y;
  }

  public TapCommand(KeyboardGeometry kgb, KeyInfo existingKey) {
    this(kgb, existingKey.absoluteX, existingKey.absoluteY);
  }

  public void setTextReceived(String textReceived) {
    this.textReceived = textReceived;
    onDone(true);
  }

  public void setKeyReceived(int keyReceived) {
    this.keyReceived = keyReceived;
    onDone(true);
  }

  @Override
  public void onNothingReceived() {
    onDone(false);
  }

  public abstract void onDone(boolean success);

  public static abstract class FindAnyKey extends TapCommand {
    public static final String TAG = "FindAnyKey";

    private final Direction d;

    public FindAnyKey(KeyboardGeometry kgb, int x, int y, Direction d) {
      super(kgb, x, y);
      this.d = d;
    }

    public void again() {
      again(d);
    }

    public void again(Direction tempD) {
      tempD.apply(this);
      textReceived = null;
      keyReceived = -1;

      if (x < 0 || x > kgb.screenSize.x
          || y < kgb.keyboardTop || y > kgb.screenSize.y) {
        outOfKeyboard();
      } else {
        kgb.addCommand(this);
      }
    }

    public void outOfKeyboard() {
      kgb.onScenarioDone(false);
    }
  }

  public static abstract class FindAKey extends FindAnyKey {
    public static final String TAG = "FindAnyKey";

    private final String textExpected;

    public FindAKey(KeyboardGeometry kgb, int x, int y, Direction d, String textExpected) {
      super(kgb, x, y, d);
      this.textExpected = textExpected;
      Log.d(TAG, "Looking for key '" + textExpected + "'");
    }

    @Override
    public void onDone(boolean success) {
      if (success) {
        if (textExpected != null) {
          if (textExpected.equalsIgnoreCase(textReceived)) {
            foundKey();
          } else {
            wrongKey();
          }
        } else {
          foundKey();
        }
      } else {
        again();
      }
    }

    public abstract void foundKey();
    public abstract void wrongKey();
  }
}


