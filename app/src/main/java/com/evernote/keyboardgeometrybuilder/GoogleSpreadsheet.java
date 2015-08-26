/**
 * Copyright (c) 2015 Evernote Corporation. All rights reserved.
 */
package com.evernote.keyboardgeometrybuilder;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Point;
import android.os.Build;
import android.provider.Settings;
import android.util.DisplayMetrics;

import com.evernote.espressokeyboard.KeyInfo;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Collection;
import java.util.Locale;
import java.util.UUID;

/**
 * Created by paour on 07/08/15.
 */
public class GoogleSpreadsheet {
  public static final String TAG = "GoogleSpreadsheet";

  public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

  public static void uploadSession(Context context, Collection<KeyInfo> foundKeys) {
    try {
      OkHttpClient client = new OkHttpClient();

      JSONObject jsonBody = new JSONObject();
      try {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        Point screenSize = Util.getRealScreenSize(context);
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

        JSONObject jsonGlobal = new JSONObject()
            .put("run", UUID.randomUUID())
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
            .put("screen_h", screenSize.y);

        jsonBody.put("global", jsonGlobal);
        JSONArray jsonKeys = new JSONArray();
        jsonBody.put("keys", jsonKeys);

        for (KeyInfo foundKey : foundKeys) {
          jsonKeys.put(foundKey.toJson());
        }
      } catch (JSONException e1) {
        e1.printStackTrace();
      }

      System.out.println(jsonBody.toString());

      RequestBody body = RequestBody.create(JSON, jsonBody.toString());
      Request request = new Request.Builder()
          .url("https://script.google.com/macros/s/AKfycbyE7NEzpm6sGFhNd9j22QkI5RS6rpGeVDv6J5EHEUCl3Gy6AFU/exec")
          .post(body)
          .build();
      Response response = client.newCall(request).execute();
      System.out.println(response.body().string());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
