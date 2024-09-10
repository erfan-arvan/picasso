package com.squareup.picasso3;
class FileRequestHandler extends ContentStreamRequestHandler {
  FileRequestHandler(Context context) {
    super(context);
  }
  @Override public boolean canHandleRequest( Request data) {
    return SCHEME_FILE.equals(data.uri.getScheme());
  }
  @Override
  public void load( Picasso picasso,  Request request,  Callback callback) {
    boolean signaledCallback = false;
    try {
      Source source = getSource(request);
      Bitmap bitmap = decodeStream(source, request);
      int exifRotation = getExifOrientation(request);
      signaledCallback = true;
      callback.onSuccess(new Result(bitmap, DISK, exifRotation));
    } catch (Exception e) {
      if (!signaledCallback) {
        callback.onError(e);
      }
    }
  }
  @Override protected int getExifOrientation(Request request) throws IOException {
    ExifInterface exifInterface = new ExifInterface(request.uri.getPath());
    return exifInterface.getAttributeInt(TAG_ORIENTATION, ORIENTATION_NORMAL);
  }
}
