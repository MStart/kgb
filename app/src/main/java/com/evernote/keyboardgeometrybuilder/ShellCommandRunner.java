/**
 * Copyright (c) 2015 Evernote Corporation. All rights reserved.
 */
package com.evernote.keyboardgeometrybuilder;

import android.util.Log;

import eu.chainfire.libsuperuser.Shell;

/**
 * Created by paour on 07/08/15.
 */
public class ShellCommandRunner extends Thread {
  public static final String TAG = "ShellCommandRunner";
  private final Shell.Interactive shell;
  private String shellCommand;
  private boolean running = true;

  public ShellCommandRunner(Shell.Interactive shell) {
    super(TAG);
    this.shell = shell;
    start();
  }

  @Override
  public void run() {
    while (running) {
      if (shellCommand == null) {
        synchronized (shell) {
          try {
            shell.wait();
          } catch (InterruptedException e) {
            Log.e(TAG, "", e);
          }
        }
      } else {
        String tmpCommand;

        synchronized (shell) {
          tmpCommand = shellCommand;
          shellCommand = null;
        }

        shell.waitForIdle();
        Log.d(TAG, "*** Executing " + tmpCommand);
        shell.addCommand(tmpCommand);
      }
    }
  }

  public void addCommand(String shellCommand) {
    synchronized (shell) {
      if (this.shellCommand != null) {
        throw new IllegalStateException("Trying to run two commands");
      }

      this.shellCommand = shellCommand;

      shell.notifyAll();
    }
  }

  public void shutdown() {
    running = false;
    synchronized (shell) {
      shell.notifyAll();
    }
  }
}
