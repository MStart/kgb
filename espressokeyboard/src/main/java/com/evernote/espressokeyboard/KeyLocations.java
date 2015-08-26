/**
 * Copyright (c) 2015 Evernote Corporation. All rights reserved.
 */
package com.evernote.espressokeyboard;

import com.google.gdata.client.spreadsheet.FeedURLFactory;
import com.google.gdata.client.spreadsheet.SpreadsheetService;
import com.google.gdata.data.spreadsheet.ListEntry;
import com.google.gdata.data.spreadsheet.ListFeed;
import com.google.gdata.data.spreadsheet.WorksheetEntry;
import com.google.gdata.data.spreadsheet.WorksheetFeed;
import com.google.gdata.util.ServiceException;

import java.io.IOException;
import java.net.URL;
import java.util.List;

/**
 * Created by paour on 26/08/15.
 */
public class KeyLocations {
  static KeyLocations instance = null;

  private KeyLocations() {
    try {
      SpreadsheetService service =
          new SpreadsheetService("MySpreadsheetIntegration-v1");

      URL worksheetFeedUrl = FeedURLFactory.getDefault().getWorksheetFeedUrl("15sKldjTZ0YbtfihlSKzEgikCJL6hMNtKuWIIpudvPBQ", "public", "full");

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
          System.out.print(tag + "=" + row.getCustomElements().getValue(tag) + "\t");
        }
        System.out.println();
      }
    } catch (IOException e) {
      e.printStackTrace();
    } catch (ServiceException e) {
      e.printStackTrace();
    }
  }

  public static synchronized KeyLocations instance() {
    if (instance == null) {
      instance = new KeyLocations();
    }

    return instance;
  }

  public KeyInfo findKey(char key) {
    return new KeyInfo(60, 923, "" + key);
  }
}
