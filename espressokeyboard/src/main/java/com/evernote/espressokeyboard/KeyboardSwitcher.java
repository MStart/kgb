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
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;

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

  /*public static boolean updateIme(Activity activity, String newKeyboardId, Class<? extends Activity> newActivity) {
    try {
      String currId = Settings.Secure.getString(activity.getContentResolver(),
          Settings.Secure.DEFAULT_INPUT_METHOD);
      if (!newKeyboardId.equals(currId)) {
        Intent activityIntent = new Intent(activity,
            newActivity);
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION);

        activity.finish();
        activity.startActivity(activityIntent);

        final InputMethodManager imm = (InputMethodManager) activity
            .getSystemService(Context.INPUT_METHOD_SERVICE);

        // stop the current ime
        stopIme(activity, currId);

        imm.setInputMethod(null, newKeyboardId);

        // Send a notification broadcast for the change in IMEs
        Intent updateImeIntent = new Intent(Intent.ACTION_INPUT_METHOD_CHANGED);
        updateImeIntent.putExtra("input_method_id", newKeyboardId);
      }
    } catch (Exception e) {
      Log.e(TAG,
          "Error occurred while switching keyboards to ".concat(newKeyboardId),
          e);
      return false;
    }

    return true;
  }

  public static void stopIme(Context caller, String imeId) {
    List<String> pidList = retrieveImePid(caller, imeId);
    if (pidList == null) {
      Log.e(TAG, "Error occurred while trying to retrieve pids for ime id: "
          .concat(imeId));
      return;
    }

    int numPids = pidList.size();
    String[] killCommands = new String[numPids];
    for (int i = 0; i < numPids; i++)
      killCommands[i] = "kill -".concat(KillSignal.TERM.name()).concat(" ")
          .concat(pidList.get(i));

    Shell.SU.run(killCommands);
  }

  public static List<String> retrieveImePid(Context caller, String imeId) {
    Map<String, InputMethodInfo> imeIdInfo = getIdLabelMap(caller);
    if (!imeIdInfo.containsKey(imeId))
      return null;

    InputMethodInfo currIme = imeIdInfo.get(imeId);
    if (currIme == null)
      return null;

    // Get the current ime process name.
    String currImeProcessName = currIme.getServiceInfo().processName;
    return retrieveProcessPids(currImeProcessName);
  }

  public static int getPsPidIndex() {
    String command = "ps -h";
    List<String> output = Shell.SH.run(command);
    if (output == null) {
      Log.e(TAG, "Unable to run command ".concat(command));
      return DEFAULT_PS_PID_INDEX;
    }

    int numParts;
    for (String line : output) {
      String[] lineParts = line.split("\\s+");
      numParts = lineParts.length;

      for (int i = 0; i < numParts; i++) {
        if (lineParts[i].equals(PID_COL))
          return i;
      }
    }

    return DEFAULT_PS_PID_INDEX;
  }

  public static List<String> retrieveProcessPids(String processName) {
    String command = "ps|grep ".concat(processName);
    List<String> output = Shell.SH.run(command);
    if (output == null) {
      Log.e(TAG, "Unable to run command ".concat(command));
      return Collections.emptyList();
    }

    int pidColIndex = getPsPidIndex();

    List<String> pidList = new ArrayList<String>(output.size());
    for (String line : output) {
      String[] lineParts = line.split("\\s+");
      if (lineParts.length < 2)
        continue;

      pidList.add(lineParts[pidColIndex]);
    }

    return pidList;
  }

  public static Pair<CharSequence[], CharSequence[]> getKeyboardsInfo(Context context) {
    // Retrieve list of input method
    InputMethodManager imeManager = (InputMethodManager) context
        .getSystemService(Context.INPUT_METHOD_SERVICE);
    List<InputMethodInfo> imes = imeManager.getEnabledInputMethodList();
    int imesSize = imes.size();

    // Store input method labels for display in view menu
    CharSequence[] keyboardLabels = new CharSequence[imesSize + 1];
    CharSequence[] keyboardIds = new CharSequence[imesSize + 1];

    keyboardLabels[0] = DEFAULT_KEYBOARD_LABEL;
    keyboardIds[0] = DEFAULT_KEYBOARD_ID;

    PackageManager pm = context.getPackageManager();
    for (int i = 1; i < imesSize + 1; i++) {
      InputMethodInfo imi = imes.get(i - 1);
      keyboardLabels[i] = imi.loadLabel(pm);
      keyboardIds[i] = imi.getId();
    }

    return new Pair<CharSequence[], CharSequence[]>(keyboardLabels, keyboardIds);
  }*/

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

  /*public static boolean isAccessibilityEnabled(Context context) {
    return isAccessibilityEnabled(context, "com.evernote.keyboardgeometrybuilder/com.evernote.espressokeyboard.KeyboardSwitcher");
  }

  public static boolean isAccessibilityEnabled(Context context, String id) {
    AccessibilityManager am = (AccessibilityManager) context
        .getSystemService(Context.ACCESSIBILITY_SERVICE);

    List<AccessibilityServiceInfo> runningServices = am
        .getInstalledAccessibilityServiceList();
    for (AccessibilityServiceInfo service : runningServices) {
      Log.i(TAG, service.getId());
      if (id.equals(service.getId())) {
        return true;
      }
    }

    return false;
  }*/

  public static Map<String, InputMethodInfo> getIdLabelMap(Context context) {
    InputMethodManager imeManager = (InputMethodManager) context
        .getSystemService(Context.INPUT_METHOD_SERVICE);
    if (imeManager == null) {
      return Collections.emptyMap();
    }

    List<InputMethodInfo> imesInfo = imeManager.getEnabledInputMethodList();
    HashMap<String, InputMethodInfo> idLabelMap = new HashMap<>(imesInfo.size());

    for (InputMethodInfo imi : imesInfo) {
      idLabelMap.put(imi.getId(), imi);
    }

    return idLabelMap;
  }

  public static boolean sIsEnabled = false;

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

  public static boolean enabled() {
    return sIsEnabled;
  }
}