/**
 * Copyright (c) 2015 Evernote Corporation. All rights reserved.
 */
package com.evernote.espressokeyboard;

import android.os.Build;
import android.support.test.espresso.IdlingResource;
import android.support.test.espresso.UiController;
import android.support.test.espresso.ViewAction;
import android.support.test.espresso.action.GeneralClickAction;
import android.support.test.espresso.action.GeneralLocation;
import android.support.test.espresso.action.Press;
import android.support.test.espresso.action.Tap;
import android.support.test.espresso.core.deps.guava.base.Preconditions;
import android.support.test.espresso.matcher.ViewMatchers;
import android.util.Log;
import android.view.View;
import android.widget.SearchView;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

import eu.chainfire.libsuperuser.Shell;

/**
 * Created by paour on 26/08/15.
 */
public class KeyboardTypeAction implements ViewAction, IdlingResource {
  private static final String TAG = KeyboardTypeAction.class.getSimpleName();
  private final String stringToBeTyped;
  private final boolean tapToFocus;
  private Shell.Interactive interactive;
  private ResourceCallback resourceCallback;

  public KeyboardTypeAction(String stringToBeTyped) {
    this(stringToBeTyped, true);
  }

  public KeyboardTypeAction(String stringToBeTyped, boolean tapToFocus) {
    Preconditions.checkNotNull(stringToBeTyped);
    this.stringToBeTyped = stringToBeTyped;
    this.tapToFocus = tapToFocus;
  }

  public Matcher<View> getConstraints() {
    Matcher matchers = Matchers.allOf(ViewMatchers.isDisplayed());
    if(!this.tapToFocus) {
      matchers = Matchers.allOf(matchers, ViewMatchers.hasFocus());
    }

    //noinspection unchecked
    return Build.VERSION.SDK_INT < 11 ?
        Matchers.allOf(matchers, ViewMatchers.supportsInputMethods()) :
        Matchers.allOf(matchers, Matchers.anyOf(ViewMatchers.supportsInputMethods(), ViewMatchers.isAssignableFrom(SearchView.class)));
  }

  public void perform(UiController uiController, View view) {
    if(this.stringToBeTyped.length() == 0) {
      Log.w(TAG, "Supplied string is empty resulting in no-op (nothing is typed).");
    } else {
      if(this.tapToFocus) {
        (new GeneralClickAction(Tap.SINGLE, GeneralLocation.CENTER, Press.FINGER)).perform(uiController, view);
        uiController.loopMainThreadUntilIdle();
      }

      Shell.Builder builder = new Shell.Builder().useSU();

      for (char c : stringToBeTyped.toCharArray()) {
        KeyInfo keyInfo = KeyLocations.instance().findKey(c);
        builder.addCommand("input tap " + keyInfo.absoluteX + " " + keyInfo.absoluteY);
      }

      interactive = builder.open(null);

      // todo: use IdlingResource
      uiController.loopMainThreadForAtLeast(stringToBeTyped.length() * 200);
    }
  }

  public String getDescription() {
    return String.format("really type text(%s)", this.stringToBeTyped);
  }

  @Override
  public String getName() {
    return "Root command idle";
  }

  @Override
  public boolean isIdleNow() {
    return interactive == null || interactive.isIdle();
  }

  @Override
  public void registerIdleTransitionCallback(ResourceCallback resourceCallback) {
    this.resourceCallback = resourceCallback;
  }
}
