/**
 * Copyright (c) 2015 Evernote Corporation. All rights reserved.
 */
package com.evernote.espressokeyboard;

import android.content.Context;

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
import java.util.List;

/**
 * Created by paour on 26/08/15.
 */
public class KeyLocations {
  static KeyLocations instance = null;

  HashMap<KeyInfo,KeyInfo> keys = new HashMap<>();

  private KeyLocations(Context context) {
    try {
/*
      SpreadsheetService service =
          new SpreadsheetService("MySpreadsheetIntegration-v1");

      URL worksheetFeedUrl = FeedURLFactory.getDefault().getWorksheetFeedUrl("15sKldjTZ0YbtfihlSKzEgikCJL6hMNtKuWIIpudvPBQ", "public", "full");

      WorksheetFeed worksheetFeed = service.getFeed(worksheetFeedUrl, WorksheetFeed.class);
      List<WorksheetEntry> worksheets = worksheetFeed.getEntries();
      WorksheetEntry runsWorksheet = null;
      WorksheetEntry keysWorksheet = null;

      for (WorksheetEntry worksheet : worksheets) {
        String title = worksheet.getTitle().getPlainText();
        if ("runs".equals(title)) {
          runsWorksheet = worksheet;
        } else if ("keys".equals(title)) {
          keysWorksheet = worksheet;
        }
      }

      // Fetch the list feed of the worksheet.
      URL listFeedUrl = runsWorksheet.getListFeedUrl();
      ListFeed listFeed = service.getFeed(new Query(listFeedUrl).add, ListFeed.class);

      // Iterate through each row, printing its cell values.
      for (ListEntry row : listFeed.getEntries()) {
        // Print the first column's cell value
        System.out.print(row.getTitle().getPlainText() + "\t");
        // Iterate over the remaining columns, and print each cell value
        for (String tag : row.getCustomElements().getTags()) {
          System.out.print(tag + "=" + row.getCustomElements().getValue(tag) + "\t");
        }
        System.out.println();
      }
*/
      OkHttpClient client = new OkHttpClient();

      //client.networkInterceptors().add(new StethoInterceptor());

      JSONObject jsonConfig = ConfigHelper.getConfig(context);

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

      JSONArray jsonKeys = responseJson.getJSONArray("keys");

      for (int i = 0; i < jsonKeys.length(); i++) {
        KeyInfo key = new KeyInfo(jsonKeys.getJSONObject(i));
        keys.put(key, key);
      }

      System.out.println(keys);
    } catch (IOException e) {
      e.printStackTrace();
    } catch (JSONException e) {
      e.printStackTrace();
    }
  }

  public static synchronized KeyLocations create(Context context) {
    if (instance == null) {
      instance = new KeyLocations(context);
    }

    return instance;
  }

  public static synchronized KeyLocations instance() {
    return instance;
  }

  public KeyInfo findKey(char key) {
    return keys.get(new KeyInfo("" + key));
  }
}
