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
  public static final String DEVICE = "device";
  public static final String KEYBOARD = "keyboard";
  public static final String DENSITY = "density";
  public static final String SCREEN_W = "screen_w";
  public static final String SCREEN_H = "screen_h";
  public static final String FONT_SCALE = "font_scale";
  public static final String LANGUAGE = "language";
  public static final String COUNTRY = "country";
  public static final String ORIENTATION = "orientation";
  public static final String ORIENTATION_PORTRAIT = "Portrait";
  public static final String ORIENTATION_LANDSCAPE = "Landscape";
  public static final String NAVBAR_W = "navbar_w";
  public static final String NAVBAR_H = "navbar_h";

  public static JSONObject getConfig(Context context) throws JSONException {
    DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
    Point screenSize = NavBarUtil.getRealScreenSize(context);
    Point navigationBarSize = NavBarUtil.getNavigationBarSize(context);
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
        .put(DEVICE, Build.MODEL)
        .put("manufacturer", Build.BRAND)
        .put("android_version", Build.VERSION.RELEASE)
        .put(ORIENTATION, context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT ?
            ORIENTATION_PORTRAIT : ORIENTATION_LANDSCAPE)
        .put(LANGUAGE, Locale.getDefault().getLanguage())
        .put(COUNTRY, Locale.getDefault().getCountry())
        .put(KEYBOARD, keyboard)
        .put("keyboard_version_name", keyboardVersionName)
        .put("keyboard_version_code", keyboardVersionCode)
        .put(DENSITY, displayMetrics.density)
        .put("density_dpi", displayMetrics.densityDpi)
        .put("density_scaled", displayMetrics.scaledDensity)
        .put(FONT_SCALE, context.getResources().getConfiguration().fontScale)
        .put(SCREEN_W, screenSize.x)
        .put(SCREEN_H, screenSize.y)
        .put("kgb_version", BuildConfig.VERSION_CODE)
        .put(NAVBAR_W, navigationBarSize.x)
        .put(NAVBAR_H, navigationBarSize.y)
        ;
  }
}
