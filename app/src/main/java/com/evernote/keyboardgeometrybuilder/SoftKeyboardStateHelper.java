package com.evernote.keyboardgeometrybuilder;

import android.app.Activity;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import java.util.LinkedList;
import java.util.List;

/**
 * Helper to keep track of whether the soft keyboard is on screen or not.  This is good to use
 * standalone for Activities, but for Fragments, just use onKeyboardStateChanged
 *
 * Created by janders on 4/1/15.
 *
 * Copyright (c) 2015 Evernote Corporation. All rights reserved.
 *
 * http://stackoverflow.com/questions/2150078/how-to-check-visibility-of-software-keyboard-in-android#
 *
 */
public class SoftKeyboardStateHelper implements ViewTreeObserver.OnGlobalLayoutListener {

  public int getmLastSoftKeyboardHeightInPx() {
    return mLastSoftKeyboardHeightInPx;
  }

  /**
   * Listener interface to know when onscreen keyboard is shown and hidden
   */
  public interface SoftKeyboardStateListener {
    /**
     * Called with true when keyboard is shown, false when hidden
     * @param open true when keyboard is shown, false when hidden
     */
    void onSoftKeyboardStateChanged(boolean open);
  }

  // From @nathanielwolf answer...  Lollipop includes button bar in the root. Add height of button bar (48dp) to maxDiff
  // Not sure if needed, but keeping here for quick reference
  //private final int EstimatedKeyboardDP = DefaultKeyboardDP + (SystemUtils.isLollipopOrGreater() ? 48 : 0);

  private final List<SoftKeyboardStateListener> mListeners = new LinkedList<>();
  private final View mScreenRootView;
  private int mLastSoftKeyboardHeightInPx;
  private Boolean mIsSoftKeyboardOpened;
  private int mKeyboardHeight;
  final Rect mRect = new Rect();

  /**
   *
   * @param activity the Activity
   */
  public SoftKeyboardStateHelper(Activity activity) {
    this(activity, false);
  }

  /**
   *
   * @param activity the Activity
   * @param isSoftKeyboardOpened starting state
   */
  public SoftKeyboardStateHelper(Activity activity, boolean isSoftKeyboardOpened) {
    mScreenRootView = ((ViewGroup) activity.findViewById(android.R.id.content)).getChildAt(0);
    mIsSoftKeyboardOpened = isSoftKeyboardOpened;
    mScreenRootView.getViewTreeObserver().addOnGlobalLayoutListener(this);
    mKeyboardHeight = mScreenRootView.getContext().getResources().getDimensionPixelSize(R.dimen.keyboard_min_height);
  }

  @Override
  public void onGlobalLayout() {
    //r will be populated with the coordinates of your view that area still visible.
    mScreenRootView.getWindowVisibleDisplayFrame(mRect);

    final int heightDiff = mScreenRootView.getRootView().getHeight() - mRect.height();
    if (!mIsSoftKeyboardOpened && heightDiff > mKeyboardHeight) { // if more than mKeyboardHeight, its probably a keyboard...
      mIsSoftKeyboardOpened = true;
      mLastSoftKeyboardHeightInPx = heightDiff;
      notifyOnSoftKeyboardStateChanged(true);
    } else if (mIsSoftKeyboardOpened && heightDiff < mKeyboardHeight) {
      mIsSoftKeyboardOpened = false;
      notifyOnSoftKeyboardStateChanged(false);
    }
  }

  public void setIsSoftKeyboardOpened(boolean isSoftKeyboardOpened) {
    mIsSoftKeyboardOpened = isSoftKeyboardOpened;
  }

  public boolean isSoftKeyboardOpened() {
    return mIsSoftKeyboardOpened;
  }

//  /**
//   * Not sure this can really be trusted 100% of the time
//   * Default value is zero (0)
//   * @return last saved keyboard height in px
//   */
//  public int getLastSoftKeyboardHeightInPx() {
//    return mLastSoftKeyboardHeightInPx;
//  }

  public void addSoftKeyboardStateListener(SoftKeyboardStateListener listener) {
    mListeners.add(listener);
  }

  public void removeSoftKeyboardStateListener(SoftKeyboardStateListener listener) {
    mListeners.remove(listener);
  }

  public void dispose() {
    try {
      //noinspection deprecation
      mScreenRootView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
    } catch (Exception ignored) {
      //
    }
  }

  private void notifyOnSoftKeyboardStateChanged(boolean open) {
    for (SoftKeyboardStateListener listener : mListeners) {
      if (listener != null) {
        listener.onSoftKeyboardStateChanged(open);
      }
    }
  }
}