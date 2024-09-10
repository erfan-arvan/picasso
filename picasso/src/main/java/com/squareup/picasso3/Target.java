package com.squareup.picasso3;
public class Target<T> {
  final T target;
   final Drawable errorDrawable;
  final int errorResId;
  final boolean noFade;
  Target( T target) {
    this.target = target;
    this.errorResId = 0;
    this.errorDrawable = null;
    this.noFade = false;
  }
  Target( T target, @DrawableRes int errorResId) {
    this.target = target;
    this.errorResId = errorResId;
    this.errorDrawable = null;
    this.noFade = false;
  }
  Target( T target, Drawable errorDrawable) {
    this.target = target;
    this.errorResId = 0;
    this.errorDrawable = errorDrawable;
    this.noFade = false;
  }
  Target( T target, @DrawableRes int errorResId,  Drawable errorDrawable,
      boolean noFade) {
    this.target = target;
    this.errorResId = errorResId;
    this.errorDrawable = errorDrawable;
    this.noFade = noFade;
  }
}
