/**
 * Copyright (c) 2015 Evernote Corporation. All rights reserved.
 */
package com.evernote.keyboardgeometrybuilder;

import android.util.Log;
import android.view.KeyEvent;

/**
 * Created by paour on 04/08/15.
 */
public class AutomaticScenario {
  public static final String TAG = "AutomaticScenario";
  private final KeyboardGeometry kgb;

  public AutomaticScenario(KeyboardGeometry kgb) {
    this.kgb = kgb;

    findSpaceBar();
  }

  public void findSpaceBar() {
    kgb.logTitle("Looking for space bar");
    kgb.addCommand(new TapCommand.FindAKey(kgb, kgb.navBarSize.x / 2, kgb.screenSize.y - kgb.navBarSize.y, Direction.UP_PIX, " ") {
      public static final String TAG = "findSpaceBar";
      int spaceBarTop;

      @Override
      public void foundKey() {
        if (kgb.spaceBarBottom == 0) {
          kgb.spaceBarBottom = y;
          Log.i(TAG, "spaceBarBottom " + kgb.spaceBarBottom);
        }

        again();
      }

      @Override
      public void wrongKey() {
        if (kgb.spaceBarBottom != 0) {
          spaceBarTop = y;

          Log.i(TAG, "spaceBarTop " + spaceBarTop);

          kgb.keyHeight = kgb.spaceBarBottom - spaceBarTop;

          kgb.logLine("Found space bar and key height: " + kgb.keyHeight);

          kgb.addKey(x, (kgb.spaceBarBottom + spaceBarTop) / 2, " ");

          findRowPadding(spaceBarTop);
        } else {
          again();
        }
      }
    });
  }

  public void findRowPadding(final int spaceBarTop) {
    kgb.logTitle("Looking for row padding");

    kgb.addCommand(new TapCommand.FindAnyKey(kgb, kgb.navBarSize.x / 2, spaceBarTop - 1, Direction.UP_PIX) {
      public static final String TAG = "findRowPadding";

      @Override
      public void onDone(boolean success) {
        if (success) {
          kgb.rowPadding = spaceBarTop - y - 1;

          kgb.logLine("Row padding: " + kgb.rowPadding);

          findTopRow();
        } else {
          again();
        }
      }
    });
  }

  public void findTopRow() {
    kgb.clearText();
    kgb.logTitle("Looking for top row");
    kgb.addCommand(new TapCommand.FindAnyKey(kgb, kgb.navBarSize.x / 2, kgb.spaceBarBottom - kgb.keyHeight / 2, Direction.UP_KEY) {
      public static final String TAG = "findTopRow";

      String lastTextReceived;

      @Override
      public void onDone(boolean success) {
        Log.d(TAG, "onDone '" + textReceived + "'");

        if (!success) {
          adjustTopRow(lastTextReceived, y);
        } else {
          if (textReceived.length() == 1) {
            lastTextReceived = textReceived;
            kgb.addKey(x, y, textReceived);
            again();
          } else {
            // completion?
            kgb.hasCompletion = true;
            kgb.logLine("Has completion");

            adjustTopRow(lastTextReceived, y);
          }
        }
      }

      @Override
      public void outOfKeyboard() {
        adjustTopRow(lastTextReceived, y);
      }
    });
  }

  public void adjustTopRow(String character, int maxKeyboardTop) {
    kgb.logTitle("Adjusting top row");
    kgb.addCommand(new TapCommand.FindAKey(kgb, kgb.navBarSize.x / 2, maxKeyboardTop, Direction.DOWN_PIX, character) {
      public static final String TAG = "adjustTopRow";

      @Override
      public void foundKey() {
        foundTopRow();
      }

      @Override
      public void wrongKey() {
        again();
      }

      @Override
      public void outOfKeyboard() {
        again();
      }

      private void foundTopRow() {
        int keyboardHeight = kgb.spaceBarBottom - y;
        kgb.numRows = keyboardHeight / kgb.keyHeight;

        kgb.rowPadding = Math.max(kgb.rowPadding, (keyboardHeight % kgb.keyHeight) / kgb.numRows);
        kgb.keyboardTop = y;

        Log.d(TAG, "numRows: " + kgb.numRows + " rowPadding " + kgb.rowPadding + " keyboardTop " + kgb.keyboardTop);

        kgb.logLine(kgb.numRows + " rows of keys");
        kgb.logLine("Top of keyboard: " + kgb.keyboardTop);

        findKeyMinWidth();
      }
    });
  }

  public void findKeyMinWidth() {
    kgb.clearText();
    kgb.logTitle("Looking for key width");
    kgb.addCommand(new TapCommand.FindAnyKey(kgb, 0, kgb.keyboardTop + kgb.keyHeight / 2, Direction.RIGHT_PIX) {
      public static final String TAG = "findKeyMinWidth";
      int xMin;

      @Override
      public void onDone(boolean success) {
        if (!success) {
          again();
        } else if (textReceived.length() > 1) {
          // in suggestions?
          kgb.logLine("Incorrect keyboardTop");
          kgb.onScenarioDone(false);
        } else {
          xMin = x;
          kgb.addCommand(new FindAKey(kgb, x, kgb.keyboardTop + kgb.keyHeight / 2, Direction.RIGHT_PIX, textReceived) {
            @Override
            public void foundKey() {
              again();
            }

            @Override
            public void wrongKey() {
              kgb.minKeyWidth = x - xMin;

              kgb.logLine("Key width: " + kgb.minKeyWidth);

              findAllKeys();
            }
          });
        }
      }
    });
  }

  public void findAllKeys() {
    kgb.logTitle("Finding all keys");
    kgb.addCommand(new TapCommand.FindAnyKey(kgb, kgb.minKeyWidth / 2, kgb.keyboardTop + kgb.keyHeight / 2, Direction.RIGHT_HALF_KEY) {
      public static final String TAG = "findAllKeys";
      int row = 0;

      @Override
      public void onDone(boolean success) {
        Log.d(TAG, "onDone " + success);
        if (success) {
          if (textReceived != null) {
            if (textReceived.length() == 1) {
              kgb.addKey(x, y, textReceived);
            }
          } else if (keyReceived != -1) {
            kgb.addSpecial(x, y, keyReceived);
          }

          again();
        } else {
          // probably a dead key or shift
          kgb.addCommand(new TypeCommand(kgb, "a", new Consumer<String>() {
            @Override
            public void accept(String s) {
              Log.d(TAG, "accept " + s);
              if ("A".equals(s)) {
                kgb.addSpecial(x, y, KeyEvent.KEYCODE_SHIFT_LEFT);
              }

              again();
            }
          }));
        }
      }

      @Override
      public void outOfKeyboard() {
        Log.d(TAG, "outOfKeyboard");
        if (++row == kgb.numRows) {
          findCompletion();
        } else {
          x = kgb.minKeyWidth / 2;
          again(Direction.DOWN_KEY);
        }
      }
    });
  }

  public void findCompletion() {
    kgb.clearText();
    kgb.logTitle("Looking for completion");
    kgb.addCommand(new TypeCommand(kgb, "octob", new Consumer<String>() {
      @Override
      public void accept(String textReceived) {
        kgb.addCommand(new TapCommand(kgb, kgb.minKeyWidth, kgb.keyboardTop - kgb.keyHeight / 2) {
          @Override
          public void onDone(boolean success) {
            Log.d(TAG, "onDone " + success);
            if (success || kgb.getText().equals("octob")) {
              kgb.hasCompletion = false;
              kgb.logLine("Doesn't seem to have completion");
            } else {
              kgb.hasCompletion = false;
              kgb.addCompletion(x, y);
            }

            kgb.onScenarioDone(true);
          }
        });
      }
    }));
  }
}
