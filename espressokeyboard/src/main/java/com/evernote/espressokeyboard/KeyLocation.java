/**
 * Copyright (c) 2015 Evernote Corporation. All rights reserved.
 */
package com.evernote.espressokeyboard;

/**
 * Created by paour on 14/09/15.
 */
public class KeyLocation {
  private final int absoluteX, absoluteY;

  public static KeyLocation NONE = new KeyLocation(0, 0);

  public KeyLocation(int absoluteX, int absoluteY) {
    this.absoluteX = absoluteX;
    this.absoluteY = absoluteY;
  }

  public int getAbsoluteX() {
    return absoluteX;
  }

  public int getAbsoluteY() {
    return absoluteY;
  }

  public static KeyLocation average(KeyLocation l1, KeyLocation l2) {
    return new KeyLocation((l1.absoluteX + l2.absoluteX) / 2, (l1.absoluteY + l2.absoluteY) / 2);
  }
}
