/**
 * Copyright (c) 2015 Evernote Corporation. All rights reserved.
 */
package com.evernote.keyboardgeometrybuilder;

/*
 * Copyright (C) 2012, 2013 Fredia Huya-Kouadio <fhuyakou@gmail.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;
import android.util.Pair;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.chainfire.libsuperuser.Shell;

/**
 * Service in charge of switching the IME during orientation change
 *
 * @author ne0fhyk
 */
public class KeyboardSwitcher {
  private static final String TAG = KeyboardSwitcher.class.getName();

  public enum KillSignal {
    STOP,
    CONT,
    KILL,
    HUP,
    TERM
  }

  public static final String DEFAULT_KEYBOARD_LABEL = "Default";
  public static final String DEFAULT_KEYBOARD_ID = "default_id";
  public static final int DEFAULT_PS_PID_INDEX = 1;
  public static final String PID_COL = "PID";

  // Keyboard related settings
  protected String mCurrId = "";

  protected boolean mKillPrevIme;
  protected boolean mIsKeyEnabled;

  protected boolean updateIme(Context context, String newKeyboardId, Class<? extends Activity> newActivity) {
    final String currId = this.mCurrId;

    try {
      if (!newKeyboardId.equals(currId)) {
        /*
         * Experimentation showed that switching the IMEs on the
         * foreground activity through Keyboard Manager without some
         * kind of transition (switching to another activity/dialog
         * and back) would often cause the IME to appear in an
         * invalid/unusable state.
         */
        Intent activityIntent = new Intent(context,
            newActivity);
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION);

        context.startActivity(activityIntent);

        final InputMethodManager imm = (InputMethodManager) context
            .getSystemService(Context.INPUT_METHOD_SERVICE);

        // stop the current ime
        if (mKillPrevIme)
          stopIme(context, currId);

                /*
                 * Switch the current IME to the new one. It's only possible to
                 * use setInputMethod with a null argument for the IBinder token
                 * if the caller is a system application
                 */
        imm.setInputMethod(null, newKeyboardId);

        // Send a notification broadcast for the change in IMEs
        Intent updateImeIntent = new Intent(Intent.ACTION_INPUT_METHOD_CHANGED);
        updateImeIntent.putExtra("input_method_id", newKeyboardId);

        this.mCurrId = newKeyboardId;
      }
    } catch (Exception e) {
      Log.e(TAG,
          "Error occurred while switching keyboards to ".concat(newKeyboardId),
          e);
      return false;
    }

    return true;
  }

  public static HashMap<String, InputMethodInfo> getIdLabelMap(Context context) {
    InputMethodManager imeManager = (InputMethodManager) context
        .getSystemService(Context.INPUT_METHOD_SERVICE);
    if ( imeManager == null )
      return null;

    List<InputMethodInfo> imesInfo = imeManager.getEnabledInputMethodList();
    HashMap<String, InputMethodInfo> idLabelMap = new HashMap<String, InputMethodInfo>(
        imesInfo.size());

    for ( InputMethodInfo imi : imesInfo )
      idLabelMap.put(imi.getId(), imi);

    return idLabelMap;
  }

  public static void stopIme(Context caller, String imeId) {
    List<String> pidList = retrieveImePid(caller, imeId);
    if ( pidList == null ) {
      Log.e(TAG, "Error occurred while trying to retrieve pids for ime id: "
          .concat(imeId));
      return;
    }

    int numPids = pidList.size();
    String[] killCommands = new String[numPids];
    for ( int i = 0; i < numPids; i++ )
      killCommands[i] = "kill -".concat(KillSignal.TERM.name()).concat(" ")
          .concat(pidList.get(i));

    Shell.SU.run(killCommands);
  }

  public static List<String> retrieveImePid(Context caller, String imeId) {
    Map<String, InputMethodInfo> imeIdInfo = getIdLabelMap(caller);
    if ( !imeIdInfo.containsKey(imeId) )
      return null;

    InputMethodInfo currIme = imeIdInfo.get(imeId);
    if ( currIme == null )
      return null;

    // Get the current ime process name.
    String currImeProcessName = currIme.getServiceInfo().processName;
    return retrieveProcessPids(currImeProcessName);
  }

  public static int getPsPidIndex() {
    String command = "ps -h";
    List<String> output = Shell.SH.run(command);
    if ( output == null ) {
      Log.e(TAG, "Unable to run command ".concat(command));
      return DEFAULT_PS_PID_INDEX;
    }

    int numParts;
    for ( String line : output ) {
      String[] lineParts = line.split("\\s+");
      numParts = lineParts.length;

      for ( int i = 0; i < numParts; i++ ) {
        if ( lineParts[i].equals(PID_COL) )
          return i;
      }
    }

    return DEFAULT_PS_PID_INDEX;
  }

  public static List<String> retrieveProcessPids(String processName) {
    String command = "ps|grep ".concat(processName);
    List<String> output = Shell.SH.run(command);
    if ( output == null ) {
      Log.e(TAG, "Unable to run command ".concat(command));
      return Collections.emptyList();
    }

    int pidColIndex = getPsPidIndex();

    List<String> pidList = new ArrayList<String>(output.size());
    for ( String line : output ) {
      String[] lineParts = line.split("\\s+");
      if ( lineParts.length < 2 )
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
    for ( int i = 1; i < imesSize + 1; i++ ) {
      InputMethodInfo imi = imes.get(i - 1);
      keyboardLabels[i] = imi.loadLabel(pm);
      keyboardIds[i] = imi.getId();
    }

    return new Pair<CharSequence[], CharSequence[]>(keyboardLabels, keyboardIds);
  }
}