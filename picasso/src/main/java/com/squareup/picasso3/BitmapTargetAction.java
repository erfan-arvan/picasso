package com.squareup.picasso3;
final class BitmapTargetAction extends Action<BitmapTarget> {
  BitmapTargetAction(Picasso picasso, Target<BitmapTarget> wrapper, Request data) {
    super(picasso, wrapper, data);
  }
  @Override void complete(RequestHandler.Result result) {
    if (result == null) {
      throw new AssertionError(
          String.format("Attempted to complete action with no result!\n%s", this));
    }
    BitmapTarget target = getTarget();
    Bitmap bitmap = result.getBitmap();
    if (bitmap != null) {
      target.onBitmapLoaded(bitmap, result.getLoadedFrom());
      if (bitmap.isRecycled()) {
        throw new IllegalStateException("Target callback must not recycle bitmap!");
      }
    }
  }
  @Override void error(Exception e) {
    BitmapTarget target = getTarget();
    if (wrapper.errorResId != 0) {
      target.onBitmapFailed(e,
          ContextCompat.getDrawable(picasso.context, wrapper.errorResId));
    } else {
      target.onBitmapFailed(e, wrapper.errorDrawable);
    }
  }
}
