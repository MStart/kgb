/**
 * Copyright (c) 2015 Evernote Corporation. All rights reserved.
 */
package com.evernote.espressokeyboard;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Point;
import android.os.Build;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

/**
 * Created by paour on 27/08/15.
 */
public class ConfigHelper {
  public static JSONObject getConfig(Context context) throws JSONException {
    DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
    Point screenSize = getRealScreenSize(context);
    String keyboard = Settings.Secure.getString(context.getContentResolver(),
        Settings.Secure.DEFAULT_INPUT_METHOD);
    String keyboardShort = keyboard.substring(0, keyboard.indexOf('/'));
    String keyboardVersionName = null;
    int keyboardVersionCode = 0;
    try {
      PackageInfo packageInfo = context.getPackageManager().getPackageInfo(keyboardShort, 0);
      keyboardVersionName = packageInfo.versionName;
      keyboardVersionCode = packageInfo.versionCode;
    } catch (PackageManager.NameNotFoundException e) {
      e.printStackTrace();
    }

    return new JSONObject()
        .put("device", Build.MODEL)
        .put("manufacturer", Build.BRAND)
        .put("android_version", Build.VERSION.RELEASE)
        .put("orientation", context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT ?
            "Portrait" : "Landscape")
        .put("language", Locale.getDefault().getLanguage())
        .put("country", Locale.getDefault().getCountry())
        .put("keyboard", keyboard)
        .put("keyboard_version_name", keyboardVersionName)
        .put("keyboard_version_code", keyboardVersionCode)
        .put("density", displayMetrics.density)
        .put("density_dpi", displayMetrics.densityDpi)
        .put("density_scaled", displayMetrics.scaledDensity)
        .put("font_scale", context.getResources().getConfiguration().fontScale)
        .put("screen_w", screenSize.x)
        .put("screen_h", screenSize.y)
        .put("kgb_version", BuildConfig.VERSION_CODE);
  }

  public static Point getRealScreenSize(Context context) {
    WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    Display display = windowManager.getDefaultDisplay();
    Point size = new Point();

    if (Build.VERSION.SDK_INT >= 17) {
      display.getRealSize(size);
    } else if (Build.VERSION.SDK_INT >= 14) {
      try {
        size.x = (Integer) Display.class.getMethod("getRawWidth").invoke(display);
        size.y = (Integer) Display.class.getMethod("getRawHeight").invoke(display);
      } catch (Exception ignored) {}
    }

    return size;
  }
}
