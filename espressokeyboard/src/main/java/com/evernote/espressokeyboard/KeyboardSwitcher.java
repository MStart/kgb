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
 */

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
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
 * Service in charge of switching the IME during orientation change
 *
 * @author ne0fhyk
 */
public class KeyboardSwitcher extends AccessibilityService {
  private static final String TAG = KeyboardSwitcher.class.getName();

  public static final String CLASS_NAME_ALERT_DIALOG = "android.app.AlertDialog";
  public static final String PACKAGE_NAME_ALERT_DIALOG = "android";

  public static final String PACKAGE_NAME_ANDROID_SYSTEM_UI_PREFIX = "com.android.systemui";

  private final static long IMP_RECEIPT_TIME_INTERVAL = 500l; // 0.5 seconds
  private static long sIMPLaunchTime;
  private static String sNextKeyboard = null;
  private static Runnable sIMESwitchListener;
  public static boolean sIsEnabled = false;

  private final static Map<String, InputMethodInfo> sImiCache = new HashMap<>();

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
              }

              keyboardNodeInfo.recycle();
              keyboardContainer.recycle();

              if (sIMESwitchListener != null) {
                new Handler().postDelayed(sIMESwitchListener, 1000);
                sIMESwitchListener = null;
              }
            }
          }
        }
      }

      nodeInfo.recycle();
    }

    if (dismissIMP) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
        Log.d(TAG, "Performing global back action.");
        performGlobalAction(GLOBAL_ACTION_BACK);
      }
    }
  }

  public static void launchInputMethodPicker(Context context,
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

  public static boolean isAccessibilityServiceEnabled() {
    return sIsEnabled;
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
          launchInputMethodPicker(context, keyboards.get(i + 1), listener);
          return true;
        } else {
          return false;
        }
      }
    }

    return false;
  }
}