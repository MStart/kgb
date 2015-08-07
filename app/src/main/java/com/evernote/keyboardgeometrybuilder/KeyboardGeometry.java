package com.evernote.keyboardgeometrybuilder;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.HashMap;
import java.util.List;

import eu.chainfire.libsuperuser.Shell;

public class KeyboardGeometry extends AppCompatActivity implements SoftKeyboardStateHelper.SoftKeyboardStateListener {
  public static final String TAG = "kgb";

  public static final int PIXEL_PITCH = 8;

  private TextView logView;
  private ScrollView scrollView;
  private EditText editText;
  ShellCommandRunner shellCommandRunner;
  private Handler handler;
  private boolean suppressEvents = false;

  TouchCommand currentCommand;

  Point navBarSize;
  Point screenSize;
  Point screenBottom;
  int keyboardTop;
  int spaceBarBottom;
  int keyHeight;
  int minKeyWidth;
  int rowPadding;
  int numRows;
  int orientation;
  boolean hasCompletion = false;

  public static final int EVENT_DELAY = 1000;

  HashMap<KeyInfo,KeyInfo> foundKeys = new HashMap<>();
  private SoftKeyboardStateHelper keyboardStateHelper;
  private TouchView touchView;
  boolean running = true;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_keyboard_geometry);

    keyboardStateHelper = new SoftKeyboardStateHelper(this);
    keyboardStateHelper.addSoftKeyboardStateListener(this);

    handler = new Handler();

    scrollView = (ScrollView) findViewById(R.id.scrollView);
    logView = (TextView) findViewById(R.id.log);
    editText = (EditText) findViewById(R.id.input);

    navBarSize = Util.getNavigationBarSize(this);
    screenSize = Util.getRealScreenSize(this);

    orientation = getResources().getConfiguration().orientation;
    if (orientation == Configuration.ORIENTATION_PORTRAIT) {
      screenBottom = new Point(screenSize.x / 2, screenSize.y - navBarSize.y);
    } else {
      screenBottom = new Point(screenSize.x / 2 - navBarSize.x, screenSize.y);
    }

    editText.requestFocus();

    editText.addTextChangedListener(new TextWatcher() {
      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        if (suppressEvents) return;

        Log.d(TAG, "beforeTextChanged '" + s + "' - " + start + " - " + after + " - " + count);
      }

      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (suppressEvents) return;

        Log.d(TAG, "onTextChanged '" + s + "' - " + start + " - " + before + " - " + count);
        if (count > before) {
          CharSequence added = s.subSequence(start + before, start + count);
          Log.d(TAG, "Text added '" + added + "'");
          onTextReceived(added.toString());
        } else if (count == before) {
          CharSequence replaced = s.subSequence(start, start + count);
          Log.d(TAG, "Text replaced '" + replaced + "'");
          onTextReceived(replaced.toString());
        } else {
          Log.d(TAG, "Text deleted");
          onTextDeleted();
        }
      }

      @Override
      public void afterTextChanged(Editable s) {

      }
    });
    editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
      @Override
      public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (suppressEvents) return false;

        Log.d(TAG, "onEditorAction " + actionId + " - " + event);

        onKeyReceived(KeyEvent.KEYCODE_ENTER);

        return false;
      }
    });
    editText.setOnKeyListener(new View.OnKeyListener() {
      @Override
      public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (suppressEvents) return false;

        Log.d(TAG, "onKey" + event);

        if (event.getAction() == KeyEvent.ACTION_DOWN) {
          onKeyReceived(event.getKeyCode());
        }
        return false;
      }
    });

    new AsyncTask<Void,Void,Boolean>() {
      @Override
      protected Boolean doInBackground(Void... params) {
        return Shell.SU.available();
      }

      @Override
      protected void onPostExecute(Boolean suAvailable) {
        if (suAvailable) {
          logLine(Html.fromHtml("<b>Su is available, running test</b><br/>Don't touch the screen!"));

          buildGeometry();
        } else {
          logLine(Html.fromHtml("<b>Su not available</b><br/>Please run this on a rooted device and allow root for the app"));
        }
      }
    }.execute();

    touchView = new TouchView(this);
    touchView.setKeyboardGeometry(this);
    WindowManager.LayoutParams params = new WindowManager.LayoutParams(
        WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT);
    params.gravity = Gravity.FILL_HORIZONTAL | Gravity.FILL_VERTICAL;
    WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
    wm.addView(touchView, params);
  }

  @Override
  protected void onPause() {
    super.onPause();

    if (running) {
      startActivity(new Intent().setClassName(getPackageName(), this.getClass().getName())
          .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_EXCLUDE_STOPPED_PACKAGES));
    }
  }

  @Override
  public void onBackPressed() {
    logLine("Pressed back key, stopping");
    currentCommand = null;
    running = false;

    super.onBackPressed();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (shellCommandRunner != null) {
      shellCommandRunner.shutdown();
      shellCommandRunner = null;
    }

    WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
    wm.removeViewImmediate(touchView);
  }

  private void buildGeometry() {
    shellCommandRunner = new ShellCommandRunner(new Shell.Builder().useSU().open(new Shell.OnCommandResultListener() {
      @Override
      public void onCommandResult(int i, int i1, List<String> list) {
        Log.d(TAG, i + " - " + i1 + " - " + list.toString());
      }
    }));

    handler.postDelayed(new Runnable() {
      public void run() {
        new AutomaticScenario(KeyboardGeometry.this);
      }
    }, 1000);
  }

  public void onScenarioDone(boolean success) {
    logLine("Done " + success);
    Log.i(TAG, foundKeys.values().toString());

    logLine("Uploading " + foundKeys + " keys");
    new AsyncTask<Void,Void,Void>() {
      @Override
      protected Void doInBackground(Void... params) {
        GoogleSpreadsheet.uploadSession(foundKeys);
        return null;
      }

      @Override
      protected void onPostExecute(Void aVoid) {

      }
    }.execute((Void[]) null);
  }

  public synchronized void addCommand(final TouchCommand command) {
    if (currentCommand != null) {
      throw new IllegalStateException("Trying to run two commands");
    }

    currentCommand = command;

    handler.removeCallbacks(eventFailed);
    if (currentCommand instanceof TapCommand) {
      touchView.drawTouch(((TapCommand) currentCommand).x, ((TapCommand) currentCommand).y);
    }

    // if a touch triggers more than one event (KeyEvent and EditorAction), let both events be handled
    // before sending the next touch (the second one will be suppressed anyway)
    handler.postDelayed(commandAdder, 500);
  }

  public void onTextReceived(String added) {
    eventReceived();

    if (currentCommand != null) {
      TouchCommand oldCommand = currentCommand;
      currentCommand = null;
      oldCommand.setTextReceived(added);
    } else {
      Log.w(TAG, "onTextReceived no currentCommand");
    }
  }

  public void onKeyReceived(int keyCode) {
    eventReceived();

    if (currentCommand != null) {
      TouchCommand oldCommand = currentCommand;
      currentCommand = null;
      oldCommand.setKeyReceived(keyCode);
    } else {
      Log.w(TAG, "onKeyReceived no currentCommand");
    }
  }

  private void onTextDeleted() {
    eventReceived();

    if (currentCommand != null) {
      TouchCommand oldCommand = currentCommand;
      currentCommand = null;
      oldCommand.setKeyReceived(KeyEvent.KEYCODE_BACK);
    } else {
      Log.w(TAG, "onTextDeleted no currentCommand");
    }
  }

  public void eventReceived() {
    handler.removeCallbacks(eventFailed);
    suppressEvents = true;
  }

  Runnable eventFailed = new Runnable() {
    @Override
    public void run() {
      Log.d(TAG, "event timeout");
      if (currentCommand != null) {
        TouchCommand oldCommand = currentCommand;
        currentCommand = null;
        oldCommand.onNothingReceived();
      } else {
        Log.w(TAG, "eventFailed no currentCommand");
      }
    }
  };

  Runnable scrollBottomRunnable = new Runnable() {
    public void run() {
      scrollView.fullScroll(View.FOCUS_DOWN);
    }
  };

  Runnable commandAdder = new Runnable() {
    public void run() {
      try {
        suppressEvents = false;
        Log.d(TAG, "Enabling events");
        shellCommandRunner.addCommand(currentCommand.getShellCommand());
      } catch (Exception e) {
        Log.e(TAG, "Concurrent commands", e);
        logLine("Please don't interfere with the test ;-)");
      }
      handler.postDelayed(eventFailed, EVENT_DELAY);
    }
  };

  public boolean addKey(int x, int y, String character) {
    return addKeyInfo(new KeyInfo(x, y, character));
  }

  public boolean addSpecial(int x, int y, int keyCode) {
    return addKeyInfo(new KeyInfo(x, y, keyCode));
  }

  public boolean addCompletion(int x, int y) {
    return addKeyInfo(new KeyInfo(x, y));
  }

  public boolean addKeyInfo(KeyInfo newKey) {
    KeyInfo existingKey = foundKeys.get(newKey);

    if (existingKey == null) {
      foundKeys.put(newKey, newKey);
      logLine("Found " + newKey.description());
      return true;
    } else {
      existingKey.absoluteX = (existingKey.absoluteX + newKey.absoluteX) / 2;
      existingKey.absoluteY = (existingKey.absoluteY + newKey.absoluteY) / 2;
      Log.d(TAG, "Moving " + existingKey.description());
      return false;
    }
  }

  public void logLine(CharSequence line) {
    Log.i(TAG, line.toString());
    logView.append("\n");
    logView.append(line);
    handler.post(scrollBottomRunnable);
  }

  public void logTitle(String title) {
    Log.d(TAG, "###");
    logLine(Html.fromHtml("<b>" + title + "</b>"));
  }

  @Override
  public void onSoftKeyboardStateChanged(boolean open) {
    keyboardTop = screenSize.y - keyboardStateHelper.getmLastSoftKeyboardHeightInPx();
    keyboardStateHelper.dispose();

    logLine("Top of keyboard: " + keyboardTop);
  }

  public void clearText() {
    boolean wasSuppressed = suppressEvents;
    suppressEvents = true;
    editText.setText("");
    suppressEvents = wasSuppressed;
  }

  public String getText() {
    return editText.getText().toString();
  }
}
