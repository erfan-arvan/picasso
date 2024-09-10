package com.squareup.picasso3;
class ContentStreamRequestHandler extends RequestHandler {
  final Context context;
  ContentStreamRequestHandler(Context context) {
    this.context = context;
  }
  @Override public boolean canHandleRequest( Request data) {
    Uri uri = data.uri;
    return uri != null && SCHEME_CONTENT.equals(uri.getScheme());
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
  Source getSource(Uri uri) throws FileNotFoundException {
    ContentResolver contentResolver = context.getContentResolver();
    InputStream inputStream = contentResolver.openInputStream(uri);
    if (inputStream == null) {
      throw new FileNotFoundException("can't open input stream, uri: " + uri);
    }
    return Okio.source(inputStream);
  }
  protected int getExifOrientation(Uri uri) throws IOException {
    ContentResolver contentResolver = context.getContentResolver();
    InputStream inputStream = null;
    try {
      inputStream = contentResolver.openInputStream(uri);
      if (inputStream == null) {
        throw new FileNotFoundException("can't open input stream, uri: " + uri);
      }
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
