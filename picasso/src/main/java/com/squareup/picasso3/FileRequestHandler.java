package com.squareup.picasso3;
class FileRequestHandler extends ContentStreamRequestHandler {
  FileRequestHandler(Context context) {
    super(context);
  }
  @Override public boolean canHandleRequest( Request data) {
    Uri uri = data.uri;
    return uri != null && SCHEME_FILE.equals(uri.getScheme());
  }
  @Override
  public void load( Picasso picasso,  Request request,  Callback callback) {
    boolean signaledCallback = false;
    try {
      Uri requestUri = checkNotNull(request.uri, "request.uri == null");
      Source source = getSource(requestUri);
      Bitmap bitmap = decodeStream(source, request);
      int exifRotation = getExifOrientation(requestUri);
      signaledCallback = true;
      callback.onSuccess(new Result(bitmap, DISK, exifRotation));
    } catch (Exception e) {
      if (!signaledCallback) {
        callback.onError(e);
      }
    }
  }
  @Override protected int getExifOrientation(Uri uri) throws IOException {
    String path = uri.getPath();
    if (path == null) {
      throw new FileNotFoundException("path == null, uri: " + uri);
    }
    ExifInterface exifInterface = new ExifInterface(path);
    return exifInterface.getAttributeInt(TAG_ORIENTATION, ORIENTATION_NORMAL);
  }
}
