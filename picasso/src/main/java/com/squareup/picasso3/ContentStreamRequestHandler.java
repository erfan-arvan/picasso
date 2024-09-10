package com.squareup.picasso3;
class ContentStreamRequestHandler extends RequestHandler {
  final Context context;
  ContentStreamRequestHandler(Context context) {
    this.context = context;
  }
  @Override public boolean canHandleRequest( Request data) {
    return SCHEME_CONTENT.equals(data.uri.getScheme());
  }
  @Override
  public void load( Picasso picasso,  Request request,  Callback callback) {
    boolean signaledCallback = false;
    try {
      Source source = getSource(request);
      if (source == null) {
        callback.onError(new IllegalStateException("null source from " + request.uri));
        return;
      }
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
  Source getSource(Request request) throws FileNotFoundException {
    ContentResolver contentResolver = context.getContentResolver();
    InputStream inputStream = contentResolver.openInputStream(request.uri);
    return inputStream == null ? null : Okio.source(inputStream);
  }
  protected int getExifOrientation(Request request) throws IOException {
    ContentResolver contentResolver = context.getContentResolver();
    InputStream inputStream = null;
    try {
      inputStream = contentResolver.openInputStream(request.uri);
      ExifInterface exifInterface = new ExifInterface(inputStream);
      return exifInterface.getAttributeInt(TAG_ORIENTATION, ORIENTATION_NORMAL);
    } finally {
      try {
        if (inputStream != null) {
          inputStream.close();
        }
      } catch (IOException ignored) {
      }
    }
  }
}
