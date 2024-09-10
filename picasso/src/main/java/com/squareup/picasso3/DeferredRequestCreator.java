package com.squareup.picasso3;
class DeferredRequestCreator implements OnPreDrawListener, OnAttachStateChangeListener {
  private final RequestCreator creator;
  @VisibleForTesting final ImageView target;
  @VisibleForTesting  Callback callback;
  DeferredRequestCreator(RequestCreator creator, ImageView target,  Callback callback) {
    this.creator = creator;
    this.target = target;
    this.callback = callback;
    target.addOnAttachStateChangeListener(this);
    if (target.getWindowToken() != null) {
      onViewAttachedToWindow(target);
    }
  }
  @Override public void onViewAttachedToWindow(View view) {
    view.getViewTreeObserver().addOnPreDrawListener(this);
  }
  @Override public void onViewDetachedFromWindow(View view) {
    view.getViewTreeObserver().removeOnPreDrawListener(this);
  }
  @Override public boolean onPreDraw() {
    ImageView target = this.target;
    ViewTreeObserver vto = target.getViewTreeObserver();
    if (!vto.isAlive()) {
      return true;
    }
    int width = target.getWidth();
    int height = target.getHeight();
    if (width <= 0 || height <= 0) {
      return true;
    }
    target.removeOnAttachStateChangeListener(this);
    vto.removeOnPreDrawListener(this);
    this.creator.unfit().resize(width, height).into(target, callback);
    return true;
  }
  void cancel() {
    creator.clearTag();
    callback = null;
    target.removeOnAttachStateChangeListener(this);
    ViewTreeObserver vto = target.getViewTreeObserver();
    if (vto.isAlive()) {
      vto.removeOnPreDrawListener(this);
    }
  }
   Object getTag() {
    return creator.getTag();
  }
}
