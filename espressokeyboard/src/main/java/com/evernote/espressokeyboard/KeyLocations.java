/**
 * Copyright (c) 2015 Evernote Corporation. All rights reserved.
 */
package com.evernote.espressokeyboard;

import android.content.Context;
import android.graphics.Point;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import com.squareup.okhttp.HttpUrl;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import eu.chainfire.libsuperuser.Shell;

import static com.evernote.espressokeyboard.ConfigHelper.DENSITY;
import static com.evernote.espressokeyboard.ConfigHelper.DEVICE;
import static com.evernote.espressokeyboard.ConfigHelper.FONT_SCALE;
import static com.evernote.espressokeyboard.ConfigHelper.KEYBOARD;
import static com.evernote.espressokeyboard.ConfigHelper.LANGUAGE;
import static com.evernote.espressokeyboard.ConfigHelper.NAVBAR_H;
import static com.evernote.espressokeyboard.ConfigHelper.NAVBAR_W;
import static com.evernote.espressokeyboard.ConfigHelper.ORIENTATION;
import static com.evernote.espressokeyboard.ConfigHelper.ORIENTATION_PORTRAIT;
import static com.evernote.espressokeyboard.ConfigHelper.SCREEN_H;
import static com.evernote.espressokeyboard.ConfigHelper.SCREEN_W;

/**
 * Created by paour on 26/08/15.
 */
public class KeyLocations {
  public static final String TAG = "KeyLocations";
  static KeyLocations instance = null;

  HashMap<String, HashMap<Key, KeyInfo>> keyboardKeys = new HashMap<>();
  HashMap<Key, KeyInfo> keys;

  private KeyLocations(Context context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
      Log.i(TAG, "Older platform, requires root");
      try {
        Class.forName("eu.chainfire.libsuperuser.Shell");
      } catch (ClassNotFoundException e) {
        throw new IllegalStateException("libsuperuser dependency not met. Add \"testCompile " +
            "'eu.chainfire:libsuperuser:1.0.0.+'\" to your build.gradle");
      }

      if (!Shell.SU.available()) {
        throw new IllegalStateException("This device is not rooted or you did not grand root access, " +
            "any tests depending on direct keyboard stimulation will not run");
      }
    } else {
      Log.i(TAG, "Will use uiAutomation for event injection");
    }

    try {
      OkHttpClient client = new OkHttpClient();

      //client.networkInterceptors().add(new StethoInterceptor());

      JSONObject jsonConfig = ConfigHelper.getConfig(context);

      List<String> keyboards = KeyboardSwitcher.getKeyboards(context);

      jsonConfig.put(ConfigHelper.KEYBOARD, new JSONArray(keyboards));

      //noinspection deprecation
      Request request = new Request.Builder()
          .url(HttpUrl.parse("https://script.google.com/macros/s/AKfycbyE7NEzpm6sGFhNd9j22QkI5RS6rpGeVDv6J5EHEUCl3Gy6AFU/exec")
              .newBuilder()
                  // HttpUrl.Builder doesn't properly encode curly braces (at least from java.net.URI's point of view)
                  //.setQueryParameter("config", jsonConfig.toString())
              .setEncodedQueryParameter("config", URLEncoder.encode(jsonConfig.toString()))
              .build())
          .get()
          .build();
      Response response = client.newCall(request).execute();

      JSONObject responseJson = new JSONObject(response.body().string());

      Iterator<String> it = responseJson.keys();
      while (it.hasNext()) {
        String keyboardName = it.next();
        JSONObject keyboardValue = responseJson.getJSONObject(keyboardName);
        JSONArray jsonKeys = keyboardValue.getJSONArray("keys");
        JSONObject jsonMatchedConfig = keyboardValue.getJSONObject("run");

        Log.d(TAG, "Matched config: " + jsonMatchedConfig);

        if (!Objects.equals(jsonConfig.optString(LANGUAGE), jsonMatchedConfig.optString(LANGUAGE))) {
          throw new IllegalStateException("The current language hasn't been uploaded to the database: current " + jsonConfig + " - matched " + jsonMatchedConfig);
        }

        if (!Objects.equals(jsonConfig.optString(ORIENTATION), jsonMatchedConfig.optString(ORIENTATION))) {
          throw new IllegalStateException("The current orientation hasn't been uploaded to the database: current " + jsonConfig + " - matched " + jsonMatchedConfig);
        }

        // need to project key locations?
        Point currentNavBarSize = new Point(jsonConfig.getInt(NAVBAR_W), jsonConfig.getInt(NAVBAR_H));
        Point matchedNavBarSize = new Point(jsonMatchedConfig.getInt(NAVBAR_W), jsonMatchedConfig.getInt(NAVBAR_H));
        Point translate;
        float scaleX = 1, scaleY = 1;
        if (Objects.equals(jsonConfig.optString(DEVICE), jsonMatchedConfig.optString(DEVICE))
            && Objects.equals(jsonConfig.optString(DENSITY), jsonMatchedConfig.optString(DENSITY))
            && Objects.equals(currentNavBarSize, matchedNavBarSize)
            && Objects.equals(jsonConfig.optString(FONT_SCALE), jsonMatchedConfig.optString(FONT_SCALE))) {
          translate = new Point(0, 0);
        } else {
          Point currentSize = new Point(jsonConfig.getInt(SCREEN_W), jsonConfig.getInt(SCREEN_H));
          Point matchedSize = new Point(jsonMatchedConfig.getInt(SCREEN_W), jsonMatchedConfig.getInt(SCREEN_H));

          scaleX = currentSize.x / matchedSize.x;
          scaleY = currentSize.y / matchedSize.y;
          if (ORIENTATION_PORTRAIT.equals(jsonConfig.optString(ORIENTATION))) {
            translate = new Point(0, matchedNavBarSize.y - currentNavBarSize.y);
          } else {
            translate = new Point(matchedNavBarSize.x - currentNavBarSize.x, 0);
          }

          Log.d(TAG, "Reprojecting: translation " + translate + " - scale " + scaleX + "," + scaleY);
        }

        keys = new HashMap<>();
        for (int i = 0; i < jsonKeys.length(); i++) {
          KeyInfo keyInfo = new KeyInfo(jsonKeys.getJSONObject(i), translate, scaleX, scaleY);
          keys.put(keyInfo.getKey(), keyInfo);
        }

        Log.d(TAG, "KeyLocations: " + keys);

        keyboardKeys.put(keyboardName, keys);
      }
    } catch (IOException e) {
      e.printStackTrace();
      throw new IllegalStateException("Network issue", e);
    } catch (JSONException e) {
      throw new IllegalStateException("Content issue", e);
    }
  }

  /**
   * Initialize the requirements for direct keyboard stimulation: downloads a key location profile
   * for the current configuration and tests root availability. Make sure to call this method
   * in the test class's {@code setUp()} method, since it performs network IO.
   *
   * @param context a Context instance
   * @return a singleton for this class
   */
  public static synchronized KeyLocations create(Context context) {
    if (instance == null) {
      instance = new KeyLocations(context);
    }

    return instance;
  }

  public static synchronized KeyLocations instance() {
    return instance;
  }

  /**
   * use findStandard for keys, findSpecial for specials, or findCompletion
   **/
  @Deprecated
  public KeyInfo findKey(char key) {
    return findStandard(key);
  }

  @Deprecated
  public KeyInfo findKey(int keyCode) {
    return findSpecial(keyCode);
  }

  public KeyInfo findStandard(char key) {
    return findKey(Key.getCharacter("" + key));
  }

  public KeyInfo findSpecial(int keyCode) {
    return findKey(Key.getSpecial(keyCode));
  }

  public KeyInfo findCompletion() {
    return findKey(Key.getCompletion());
  }

  /**
   * If more than one keyboard is in use, clients must call this before
   * using {@code findKey}
   * @param currentKeyboard the ID of the current keyboard
   */
  public void setCurrentKeyboard(String currentKeyboard) {
    keys = keyboardKeys.get(currentKeyboard);

    if (keys == null) {
      throw new IllegalStateException("Unknown keyboard " + currentKeyboard);
    }
  }

  public void setCurrentKeyboard(Context context) {
    setCurrentKeyboard(Settings.Secure.getString(context.getContentResolver(),
        Settings.Secure.DEFAULT_INPUT_METHOD));
  }

  private KeyInfo findKey(Key key) {
    KeyInfo result = keys.get(key);

    if (result == null) {
      throw new IllegalStateException("Could not find " + key.description());
    }

    return result;
  }
}
