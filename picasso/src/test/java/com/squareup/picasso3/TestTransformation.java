package com.squareup.picasso3;
class TestTransformation implements Transformation {
  private final String key;
  private final Bitmap result;
  TestTransformation(String key) {
    this(key, Bitmap.createBitmap(10, 10, null));
  }
  TestTransformation(String key, Bitmap result) {
    this.key = key;
    this.result = result;
  }
  @Override public RequestHandler.Result transform(RequestHandler.Result source) {
    Bitmap bitmap = source.getBitmap();
    if (bitmap == null) {
      return source;
    }
    bitmap.recycle();
    return new RequestHandler.Result(result, source.getLoadedFrom(), source.getExifRotation());
  }
  @Override public String key() {
    return key;
  }
}
