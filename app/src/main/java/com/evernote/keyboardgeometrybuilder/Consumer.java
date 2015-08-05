/**
 * Copyright (c) 2015 Evernote Corporation. All rights reserved.
 */
package com.evernote.keyboardgeometrybuilder;

/**
 * @author xlepaul
 * @since 2015-04-15
 */
public interface Consumer<T> {
  void accept(T t);
}