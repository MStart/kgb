/**
 * Copyright (c) 2015 Evernote Corporation. All rights reserved.
 */
package com.evernote.keyboardgeometrybuilder;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import com.google.gdata.client.spreadsheet.*;
import com.google.gdata.data.spreadsheet.*;
import com.google.gdata.util.*;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.*;
import java.util.*;

/**
 * Created by paour on 07/08/15.
 */
public class GoogleSpreadsheet {
  public static final String TAG = "GoogleSpreadsheet";

  public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

  public static void uploadSession(Context context, Collection<KeyInfo> foundKeys) {
    try {
      SpreadsheetService service =
          new SpreadsheetService("MySpreadsheetIntegration-v1");

      URL worksheetFeedUrl = FeedURLFactory.getDefault().getWorksheetFeedUrl("15sKldjTZ0YbtfihlSKzEgikCJL6hMNtKuWIIpudvPBQ", "public", "full");
      Log.d(TAG, worksheetFeedUrl.toString());
      WorksheetFeed worksheetFeed = service.getFeed(worksheetFeedUrl, WorksheetFeed.class);
      List<WorksheetEntry> worksheets = worksheetFeed.getEntries();
      WorksheetEntry worksheet = worksheets.get(0);

      // Fetch the list feed of the worksheet.
      URL listFeedUrl = worksheet.getListFeedUrl();
      ListFeed listFeed = service.getFeed(listFeedUrl, ListFeed.class);

      // Iterate through each row, printing its cell values.
      for (ListEntry row : listFeed.getEntries()) {
        // Print the first column's cell value
        System.out.print(row.getTitle().getPlainText() + "\t");
        // Iterate over the remaining columns, and print each cell value
        for (String tag : row.getCustomElements().getTags()) {
          System.out.print(row.getCustomElements().getValue(tag) + "\t");
        }
        System.out.println();
      }

      /*ListEntry row = new ListEntry();
      row.getCustomElements().setValueLocal("run", "Joe");
      row.getCustomElements().setValueLocal("key", "Smith");
      row.getCustomElements().setValueLocal("absoluteX", "26");
      row.getCustomElements().setValueLocal("absoluteY", "176");

      // Send the new row to the API for insertion.
      row = service.insert(listFeedUrl, row);*/

      OkHttpClient client = new OkHttpClient();

      JSONObject jsonBody = new JSONObject();
      try {
        JSONObject jsonGlobal = new JSONObject()
            .put("run", UUID.randomUUID())
            .put("device", Build.MODEL)
            .put("manufacturer", Build.BRAND)
            .put("android_version", Build.VERSION.RELEASE)
            .put("orientation", context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT ?
                "Portrait" : "Landscape")
            .put("language", Locale.getDefault().getLanguage())
            .put("country", Locale.getDefault().getCountry())
            .put("keyboard", Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.DEFAULT_INPUT_METHOD));

        Point screenSize = Util.getRealScreenSize(context);
        jsonGlobal.put("screen_x", screenSize.x)
            .put("screen_y", screenSize.y);
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
    } catch (IOException | ServiceException e) {
      e.printStackTrace();
    }
  }
}
