package com.squareup.picasso3;
public interface BitmapTarget {
  void onBitmapLoaded( Bitmap bitmap,  LoadedFrom from);
  void onBitmapFailed( Exception e,  Drawable errorDrawable);
  void onPrepareLoad( Drawable placeHolderDrawable);
}
