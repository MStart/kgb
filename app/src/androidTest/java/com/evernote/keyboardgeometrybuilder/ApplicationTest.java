package com.evernote.keyboardgeometrybuilder;

import android.test.ActivityInstrumentationTestCase2;

import com.evernote.espressokeyboard.KeyLocations;
import com.evernote.espressokeyboard.KeyboardTypeAction;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class ApplicationTest extends ActivityInstrumentationTestCase2<TestKeyboard> {
  public ApplicationTest() {
    super(TestKeyboard.class);
  }

  public void testTypeHello() throws Exception {
    onView(withId(R.id.input))
        .perform(new KeyboardTypeAction("Hello"))
        .check(matches(withText("Hello")));
  }

  public void testTypeTwoLines() throws Exception {
    onView(withId(R.id.input))
        .perform(new KeyboardTypeAction("Hello\nHi."))
        .check(matches(withText("Hello\nHi.")));
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();

    KeyLocations.create(getActivity());
  }
}