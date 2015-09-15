/**
 * Copyright (c) 2015 Evernote Corporation. All rights reserved.
 */
package com.evernote.keyboardgeometrybuilder;

import android.util.Log;

import java.util.concurrent.ArrayBlockingQueue;

import eu.chainfire.libsuperuser.Shell;

/**
 * Created by paour on 07/08/15.
 */
public class ShellCommandRunner extends Thread {
  public static final String TAG = "ShellCommandRunner";
  private final Shell.Interactive shell;
  ArrayBlockingQueue<String> queue = new ArrayBlockingQueue<>(1);

  public ShellCommandRunner(Shell.Interactive shell) {
    super(TAG);
    this.shell = shell;
    start();
  }

  @Override
  public void run() {
    while(true) {
      try {
        String tmpCommand = queue.take();

        if (tmpCommand.length() == 0) break;

        shell.waitForIdle();
        Log.d(TAG, "*** Executing " + tmpCommand);
        shell.addCommand(tmpCommand);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  public void addCommand(String shellCommand) {
    queue.add(shellCommand);
  }

  public void shutdown() {
    queue.add("");
  }
}
