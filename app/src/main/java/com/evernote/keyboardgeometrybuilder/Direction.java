/**
 * Copyright (c) 2015 Evernote Corporation. All rights reserved.
 */
package com.evernote.keyboardgeometrybuilder;

/**
 * Created by paour on 05/08/15.
 */
public enum Direction {
  UP_PIX(0, -KeyboardGeometry.PIXEL_PITCH, Pitch.PIXEL),
  DOWN_PIX(0, KeyboardGeometry.PIXEL_PITCH, Pitch.PIXEL),
  LEFT_PIX(-KeyboardGeometry.PIXEL_PITCH, 0, Pitch.PIXEL),
  RIGHT_PIX(KeyboardGeometry.PIXEL_PITCH, 0, Pitch.PIXEL),
  UP_HALF_KEY(0, -1, Pitch.HALF_KEY),
  DOWN_HALF_KEY(0, 1, Pitch.HALF_KEY),
  LEFT_HALF_KEY(-1, 0, Pitch.HALF_KEY),
  RIGHT_HALF_KEY(1, 0, Pitch.HALF_KEY),
  UP_KEY(0, -1, Pitch.KEY),
  DOWN_KEY(0, 1, Pitch.KEY),
  LEFT_KEY(-1, 0, Pitch.KEY),
  RIGHT_KEY(1, 0, Pitch.KEY);


  int dx, dy;
  Pitch p;

  Direction(int dx, int dy, Pitch p) {
    this.dx = dx;
    this.dy = dy;
    this.p = p;
  }

  public void apply(TapCommand command) {
    switch (p) {
      case PIXEL:
        command.x += dx;
        command.y += dy;
        break;

      case KEY:
        command.x += dx * command.kgb.minKeyWidth;
        command.y += dy * (command.kgb.keyHeight + command.kgb.rowPadding);
        break;

      case HALF_KEY:
        command.x += dx * command.kgb.minKeyWidth / 2;
        command.y += dy * (command.kgb.keyHeight + command.kgb.rowPadding) / 2;
        break;
    }
  }

  public enum Pitch {
    PIXEL,
    HALF_KEY,
    KEY
  }
}
