package com.squareup.picasso3;
class ImageViewAction extends Action<ImageView> {
   Callback callback;
  ImageViewAction(Picasso picasso, Target<ImageView> target, Request data,
       Callback callback) {
    super(picasso, target, data);
    this.callback = callback;
  }
  @Override public void complete(RequestHandler.Result result) {
    if (result == null) {
      throw new AssertionError(
          String.format("Attempted to complete action with no result!\n%s", this));
    }
    Target<ImageView> wrapper = checkNotNull(this.wrapper, "wrapper == null");
    ImageView target = wrapper.target;
    Context context = picasso.context;
    boolean indicatorsEnabled = picasso.indicatorsEnabled;
    PicassoDrawable.setResult(target, context, result, wrapper.noFade, indicatorsEnabled);
    if (callback != null) {
      callback.onSuccess();
    }
  }
  @Override public void error(Exception e) {
    Target<ImageView> wrapper = checkNotNull(this.wrapper, "wrapper == null");
    ImageView target = wrapper.target;
    Drawable placeholder = target.getDrawable();
    if (placeholder instanceof Animatable) {
      ((Animatable) placeholder).stop();
    }
    if (wrapper.errorResId != 0) {
      target.setImageResource(wrapper.errorResId);
    } else if (wrapper.errorDrawable != null) {
      target.setImageDrawable(wrapper.errorDrawable);
    }
    if (callback != null) {
      callback.onError(e);
    }
  }
  @Override void cancel() {
    super.cancel();
    if (callback != null) {
      callback = null;
    }
  }
}
