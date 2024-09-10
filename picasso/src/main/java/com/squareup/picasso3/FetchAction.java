package com.squareup.picasso3;
class FetchAction extends Action<Object> {
  private final Object fetchTarget;
   private Callback callback;
  FetchAction(Picasso picasso, Request data,  Callback callback) {
    super(picasso, null, data);
    this.fetchTarget = new Object();
    this.callback = callback;
  }
  @Override void complete(RequestHandler.Result result) {
    if (callback != null) {
      callback.onSuccess();
    }
  }
  @Override void error(Exception e) {
    if (callback != null) {
      callback.onError(e);
    }
  }
  @Override void cancel() {
    super.cancel();
    callback = null;
  }
  @Override Object getTarget() {
    return fetchTarget;
  }
}
