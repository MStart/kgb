/**
 * Copyright (c) 2015 Evernote Corporation. All rights reserved.
 */
package com.evernote.keyboardgeometrybuilder;

import android.util.Log;

import com.google.gdata.client.spreadsheet.*;
import com.google.gdata.data.spreadsheet.*;
import com.google.gdata.util.*;

import java.io.IOException;
import java.net.*;
import java.util.*;

/**
 * Created by paour on 07/08/15.
 */
public class GoogleSpreadsheet {
  public static final String TAG = "GoogleSpreadsheet";
  public static void uploadSession(HashMap<KeyInfo, KeyInfo> foundKeys) {
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

      ListEntry row = new ListEntry();
      row.getCustomElements().setValueLocal("run", "Joe");
      row.getCustomElements().setValueLocal("key", "Smith");
      row.getCustomElements().setValueLocal("absoluteX", "26");
      row.getCustomElements().setValueLocal("absoluteY", "176");

      // Send the new row to the API for insertion.
      row = service.insert(listFeedUrl, row);
    } catch (IOException e) {
      e.printStackTrace();
    } catch (ServiceException e) {
      e.printStackTrace();
    }
  }
}
