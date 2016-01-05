/**
 * Copyright (c) 2015 Evernote Corporation. All rights reserved.
 */
package com.evernote.espressokeyboard;

/*
 * Copyright (C) 2012, 2013 Fredia Huya-Kouadio <fhuyakou@gmail.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Changelog: 2015/09 ppaour@evernote.com: Updated for more error-checking, tap injection
 */

import android.accessibilityservice.AccessibilityService;
import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.UiAutomation;
import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class can function either as an AccessibilityService (you'll probably have to subclass it
 * as AccessibilityServiceProxy so you can add it to your AndroidManifest.xml), or as a
 * {@code UiAutomation#OnAccessibilityEventListener} delegate (for JellyBean MR2 and later)
 */
public class KeyboardSwitcher extends AccessibilityService {
  private static final String TAG = KeyboardSwitcher.class.getName();

  public static final String CLASS_NAME_ALERT_DIALOG = "android.app.AlertDialog";
  public static final String PACKAGE_NAME_ALERT_DIALOG = "android";

  public static final String PACKAGE_NAME_ANDROID_SYSTEM_UI_PREFIX = "com.android.systemui";

  private final static long IMP_RECEIPT_TIME_INTERVAL = 500L; // 0.5 seconds
  private static long sIMPLaunchTime;
  private static String sNextKeyboard = null;
  private static Runnable sIMESwitchListener;
  public static boolean sIsEnabled = false;

  private final static Map<String, InputMethodInfo> sImiCache = new HashMap<>();
  private final UiAutomation mAutomation;

  /**
   * When started as a real AccessibilityService
   */
  public KeyboardSwitcher() {
    super();
    mAutomation = null;
  }

  /**
   * When created manually as an accessibility event delegate
   *
   * @param baseContext the context this class will use
   * @param automation the automation instance use to inject taps
   */
  public KeyboardSwitcher(Context baseContext, UiAutomation automation) {
    mAutomation = automation;
    attachBaseContext(baseContext);
  }

  @Override
  public void onAccessibilityEvent(AccessibilityEvent event) {
    final String className = event.getClassName().toString();
    final String packageName = event.getPackageName().toString();
    //Log.d(TAG, "onAccessibilityEvent: " + event);
    //Log.d(TAG, "Package name: " + packageName);

    boolean isWithinIMPLaunchWindow = (System.currentTimeMillis() - sIMPLaunchTime) < IMP_RECEIPT_TIME_INTERVAL;
    if ((packageName.equals(PACKAGE_NAME_ALERT_DIALOG)
        || packageName.startsWith(PACKAGE_NAME_ANDROID_SYSTEM_UI_PREFIX))
        && className.equals(CLASS_NAME_ALERT_DIALOG) && isWithinIMPLaunchWindow) {
      handleInputMethodPicker(event);
    }
  }

  @Override
  public void onInterrupt() {
  }

  private void handleInputMethodPicker(final AccessibilityEvent event) {
    boolean dismissIMP = true;

    final AccessibilityNodeInfo nodeInfo = event.getSource();
    if (nodeInfo != null) {
      if (sNextKeyboard != null) {
        // Get the label from the keyboard id
        InputMethodInfo configImi = retrieveFromCache(sNextKeyboard);
        if (configImi != null) {
          CharSequence configKeyboardLabel = configImi.loadLabel(getPackageManager());

          if (configKeyboardLabel != null) {
            List<AccessibilityNodeInfo> keyboardList = nodeInfo
                .findAccessibilityNodeInfosByText(configKeyboardLabel.toString());

            if (keyboardList.size() > 0) {
              AccessibilityNodeInfo keyboardNodeInfo = keyboardList.get(0);
              AccessibilityNodeInfo keyboardContainer = keyboardNodeInfo.getParent();

              if (keyboardContainer.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                dismissIMP = false;
              } else {
                if (mAutomation != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                  Rect bounds = new Rect();
                  keyboardNodeInfo.getBoundsInScreen(bounds);
                  injectTap(bounds.centerX(), bounds.centerY(), mAutomation, true);

                  dismissIMP = false;
                } else {
                  Log.d(TAG, "Couldn't perform action, no automation");
                }
              }

              keyboardNodeInfo.recycle();
              keyboardContainer.recycle();

              if (sIMESwitchListener != null) {
                new Handler().postDelayed(sIMESwitchListener, 1000);
                sIMESwitchListener = null;
              }
            } else {
              Log.d(TAG, "Keyboard matching list empty");
            }
          } else {
            Log.d(TAG, "Keyboard label not found");
          }
        } else {
          Log.d(TAG, "Keyboard not found in cache");
        }
      } else {
        Log.d(TAG, "No next keyboard set");
      }

      nodeInfo.recycle();
    } else {
      Log.d(TAG, "event.getSource() is null");
    }

    if (dismissIMP) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
        Log.d(TAG, "Performing global back action.");

        if (!performGlobalAction(GLOBAL_ACTION_BACK) &&
            mAutomation != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
          mAutomation.performGlobalAction(GLOBAL_ACTION_BACK);
        }
      }
    }
  }

  @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
  public static void injectTap(int x, int y, UiAutomation uiAutomation, boolean sync) {
    long downTime = System.currentTimeMillis();
    MotionEvent eventDown = MotionEvent.obtain(downTime,
        downTime, MotionEvent.ACTION_DOWN,
        x, y, 0);
    eventDown.setSource(InputDevice.SOURCE_TOUCHSCREEN);
    Log.d(TAG, "Injecting " + eventDown);

    if (!uiAutomation.injectInputEvent(eventDown, sync)) {
      Log.d(TAG, "Injection failed");
    }

    MotionEvent eventUp = MotionEvent.obtain(eventDown);
    eventUp.setAction(MotionEvent.ACTION_UP);

    Log.d(TAG, "Injecting " + eventUp);
    if (!uiAutomation.injectInputEvent(eventUp, sync)) {
      Log.d(TAG, "Injection failed");
    }

    eventDown.recycle();
    eventUp.recycle();
  }

  public static void switchKeyboard(Context context,
                                    String nextKeyboard,
                                    Runnable listener) {
    sNextKeyboard = nextKeyboard;
    sIMESwitchListener = listener;
    sIMPLaunchTime = System.currentTimeMillis();
    final InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
    imm.showInputMethodPicker();
  }

  private void flushImiCache() {
    sImiCache.clear();
  }

  private void loadImiCache() {
    flushImiCache();

    final InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

    List<InputMethodInfo> imis = imm.getEnabledInputMethodList();
    for (InputMethodInfo imi : imis) {
      sImiCache.put(imi.getId(), imi);
    }
  }

  private InputMethodInfo retrieveFromCache(String imiId) {
    InputMethodInfo cachedValue = sImiCache.get(imiId);
    if (cachedValue == null) {
      // Reload the cache in case it's stale.
      loadImiCache();
      cachedValue = sImiCache.get(imiId);
    }

    return cachedValue;
  }

  public static List<String> getKeyboards(Context context) {
    InputMethodManager imeManager = (InputMethodManager) context
        .getSystemService(Context.INPUT_METHOD_SERVICE);
    if (imeManager == null) {
      return Collections.emptyList();
    }

    List<InputMethodInfo> imesInfo = imeManager.getEnabledInputMethodList();
    List<String> keyboards = new ArrayList<>(imesInfo.size());

    for (InputMethodInfo imi : imesInfo) {
      keyboards.add(imi.getId());
    }

    return keyboards;
  }

  @Override
  public void onServiceConnected() {
    Log.i(TAG, "onServiceConnected");
    sIsEnabled = true;
  }

  @Override
  public void onDestroy() {
    Log.i(TAG, "onDestroy");
    sIsEnabled = false;
  }

  public static boolean isAccessibilityServiceEnabled(Context context) {
    if (sIsEnabled) return true;

    ActivityManager manager = (ActivityManager) context
        .getSystemService(Context.ACTIVITY_SERVICE);
    for (ActivityManager.RunningServiceInfo service : manager
        .getRunningServices(Integer.MAX_VALUE)) {
      if (service.service.getPackageName().equals(context.getPackageName())
          && service.service.getShortClassName().contains("KeyboardSwitcher")) {
        // let's not forget the name can change (suggested name is KeyboardSwitcherProxy)
        return true;
      }
    }

    return false;
  }

  public static boolean nextKeyboard(Context context) {
    return nextKeyboard(context, null);
  }

  public static boolean nextKeyboard(Context context, Runnable listener) {
    String currentKeyboard = Settings.Secure.getString(context.getContentResolver(),
        Settings.Secure.DEFAULT_INPUT_METHOD);

    List<String> keyboards = getKeyboards(context);

    for (int i = 0; i < keyboards.size(); i++) {
      String keyboard = keyboards.get(i);
      if (keyboard.equals(currentKeyboard)) {
        if (i + 1 < keyboards.size()) {
          switchKeyboard(context, keyboards.get(i + 1), listener);
        } else {
          switchKeyboard(context, keyboards.get(0), listener);
        }
        return true;
      }
    }

    return false;
  }
}