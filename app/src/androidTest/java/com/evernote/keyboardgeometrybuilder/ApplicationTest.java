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

  public void testTypeA() throws Exception {
    onView(withId(R.id.input))
        .perform(new KeyboardTypeAction("a"))
        .check(matches(withText("a")));
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();

    KeyLocations.create(getActivity());
  }
}