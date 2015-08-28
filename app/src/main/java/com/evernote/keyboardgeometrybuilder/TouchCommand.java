/**
 * Copyright (c) 2015 Evernote Corporation. All rights reserved.
 */
package com.evernote.keyboardgeometrybuilder;

/**
 * Created by paour on 05/08/15.
 */
public interface TouchCommand {
  String getShellCommand();
  void setTextReceived(String textReceived);
  void setKeyReceived(String textReceived, int keyReceived);
  void onNothingReceived();
}
