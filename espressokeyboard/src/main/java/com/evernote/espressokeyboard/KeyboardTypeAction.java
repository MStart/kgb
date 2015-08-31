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
import android.view.KeyEvent;
import android.view.View;
import android.widget.SearchView;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import eu.chainfire.libsuperuser.Shell;

/**
 * Created by paour on 26/08/15.
 */
public class KeyboardTypeAction implements ViewAction, IdlingResource {
  private static final String TAG = KeyboardTypeAction.class.getSimpleName();
  private final boolean tapToFocus;
  private Shell.Interactive interactive;
  List<KeyInfo> keysToBeHit;
  String description;

  public KeyboardTypeAction(String stringToBeTyped) {
    this(stringToBeTyped, true);
  }

  public KeyboardTypeAction(KeyInfo... keysToBeHit) {
    this(true, keysToBeHit);
  }

  public KeyboardTypeAction(String stringToBeTyped, boolean tapToFocus) {
    Preconditions.checkNotNull(stringToBeTyped);
    this.description = stringToBeTyped;
    this.tapToFocus = tapToFocus;

    keysToBeHit = new ArrayList<>();
    for (char c : stringToBeTyped.toCharArray()) {
      if (c == '\n') {
        keysToBeHit.add(KeyLocations.instance().findKey(KeyEvent.KEYCODE_ENTER));
      } else {
        if (Character.isUpperCase(c)) {
          keysToBeHit.add(KeyLocations.instance().findKey(KeyEvent.KEYCODE_SHIFT_LEFT));
          c = Character.toLowerCase(c);
        }

        keysToBeHit.add(KeyLocations.instance().findKey(c));
      }
    }
  }

  public KeyboardTypeAction(boolean tapToFocus, KeyInfo... keysToBeHit) {
    this.tapToFocus = tapToFocus;
    this.description = String.format("%d keys", keysToBeHit.length);
    this.keysToBeHit = Arrays.asList(keysToBeHit);
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
    if (keysToBeHit.size() == 0) {
      Log.w(TAG, "Supplied string is empty resulting in no-op (nothing is typed).");
    } else {
      if(this.tapToFocus) {
        (new GeneralClickAction(Tap.SINGLE, GeneralLocation.CENTER, Press.FINGER)).perform(uiController, view);
        uiController.loopMainThreadUntilIdle();
      }

      Shell.Builder builder = new Shell.Builder().useSU();

      for (KeyInfo keyInfo : keysToBeHit) {
        builder.addCommand("input tap " + keyInfo.absoluteX + " " + keyInfo.absoluteY);
      }

      interactive = builder.open(null);

      // todo: use IdlingResource
      uiController.loopMainThreadForAtLeast(keysToBeHit.size() * 500);
    }
  }

  public String getDescription() {
    return String.format("really type text(%s)", this.description);
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
    //this.resourceCallback = resourceCallback;
  }
}
