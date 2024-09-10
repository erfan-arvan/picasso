package com.squareup.picasso3;
class ResourceRequestHandler extends RequestHandler {
  private final Context context;
  ResourceRequestHandler(Context context) {
    this.context = context;
  }
  @Override public boolean canHandleRequest( Request data) {
    if (data.resourceId != 0 && !isXmlResource(context.getResources(), data.resourceId)) {
      return true;
    }
    return data.uri != null && SCHEME_ANDROID_RESOURCE.equals(data.uri.getScheme());
  }
  @Override
  public void load( Picasso picasso,  Request request,  Callback callback) {
    boolean signaledCallback = false;
    try {
      Bitmap bitmap = decodeResource(context, request);
      signaledCallback = true;
      callback.onSuccess(new Result(bitmap, DISK));
    } catch (Exception e) {
      if (!signaledCallback) {
        callback.onError(e);
      }
    }
  }
}
