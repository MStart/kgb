/**
 * Copyright (c) 2015 Evernote Corporation. All rights reserved.
 */
package com.evernote.keyboardgeometrybuilder;

import android.content.Context;

import com.evernote.espressokeyboard.ConfigHelper;
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
import java.text.DateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

/**
 * Created by paour on 07/08/15.
 */
public class KeyboardGeometryUploader {
  public static final String TAG = "KeyboardGeometryUploader";

  public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

  public static void uploadSession(Context context, Collection<KeyInfo> foundKeys) throws IOException {
    OkHttpClient client = new OkHttpClient();

    JSONObject jsonBody = new JSONObject();
    try {
      JSONObject jsonRun = ConfigHelper.getConfig(context);

      UUID runId = UUID.randomUUID();
      DateFormat dateFormat = DateFormat.getDateTimeInstance();
      dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
      jsonRun.put("run", runId)
          .put("timestamp", dateFormat.format(new Date()));

      jsonBody.put("run", jsonRun);
      JSONArray jsonKeys = new JSONArray();
      jsonBody.put("keys", jsonKeys);

      for (KeyInfo foundKey : foundKeys) {
        jsonKeys.put(foundKey.toJson().put("run", runId));
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
  }
}
