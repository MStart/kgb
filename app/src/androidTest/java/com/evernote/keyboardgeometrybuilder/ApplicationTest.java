package com.evernote.keyboardgeometrybuilder;

import android.app.Application;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.action.ViewActions;
import android.test.ActivityInstrumentationTestCase2;
import android.test.ApplicationTestCase;
import android.util.Log;

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
  private TestKeyboard mActivity;

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

    injectInstrumentation(InstrumentationRegistry.getInstrumentation());
    mActivity = getActivity();

    /*Stetho.initialize(
        Stetho.newInitializerBuilder(mActivity)
            .enableDumpapp(
                Stetho.defaultDumperPluginsProvider(mActivity))
            .enableWebKitInspector(
                Stetho.defaultInspectorModulesProvider(mActivity))
            .build());*/

    KeyLocations.create(mActivity);
  }
}