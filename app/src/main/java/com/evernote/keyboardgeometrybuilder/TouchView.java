/**
 * Copyright (c) 2015 Evernote Corporation. All rights reserved.
 */
package com.evernote.keyboardgeometrybuilder;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;

import com.evernote.espressokeyboard.KeyInfo;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * Created by paour on 05/08/15.
 */
public class TouchView extends View {
  public static final int TOUCH_RADIUS = 10;
  public static final int TOUCH_DECAY_MS = 2000;
  private Paint paint;
  private Paint textPaint;
  private KeyboardGeometry kgb;

  static class Touch {
    int x;
    int y;
    long t;

    public Touch(int x, int y) {
      this.x = x;
      this.y = y;
      this.t = System.currentTimeMillis();
    }
  }

  LinkedList<Touch> touches = new LinkedList<>();

  public TouchView(Context context) {
    super(context);
  }

  public TouchView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public TouchView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  public TouchView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
  }

  public void setKeyboardGeometry(KeyboardGeometry kgb) {
    this.kgb = kgb;
  }

  @Override
  protected void onDraw(Canvas canvas) {
    if (paint == null) {
      paint = new Paint();
      textPaint = new Paint();
      textPaint.setTextSize(11);
      textPaint.setTextAlign(Paint.Align.CENTER);
    }

    paint.setColor(Color.RED);
    paint.setAlpha(80);
    for (int row = 0; row < kgb.numRows; row++) {
      canvas.drawLine(0, kgb.spaceBarBottom - row * (kgb.keyHeight + kgb.rowPadding),
          getWidth(), kgb.spaceBarBottom - row * (kgb.keyHeight + kgb.rowPadding), paint);
      canvas.drawLine(0, kgb.spaceBarBottom - row * (kgb.keyHeight + kgb.rowPadding) - kgb.keyHeight,
          getWidth(), kgb.spaceBarBottom - row * (kgb.keyHeight + kgb.rowPadding) - kgb.keyHeight, paint);
    }

    for (KeyInfo keyInfo : kgb.foundKeys.values()) {
      canvas.drawCircle(keyInfo.absoluteX, keyInfo.absoluteY, TOUCH_RADIUS, paint);
      canvas.drawText(keyInfo.character, keyInfo.absoluteX, keyInfo.absoluteY, textPaint);
    }

    paint.setColor(Color.BLUE);
    long curTime = System.currentTimeMillis();
    for (Iterator<Touch> iterator = touches.iterator(); iterator.hasNext(); ) {
      Touch touch = iterator.next();
      long age = curTime - touch.t;
      if (age > TOUCH_DECAY_MS) {
        iterator.remove();
      } else {
        paint.setAlpha((int) (255 * (1 - ((float) age / TOUCH_DECAY_MS))));
        canvas.drawCircle(touch.x, touch.y, TOUCH_RADIUS, paint);
      }
    }
    //canvas.drawRect(5, 5, getWidth() - 5, getHeight() - 5, paint);
  }

  public void drawTouch(int x, int y) {
    touches.add(new Touch(x, y));
    invalidate();
  }
}
