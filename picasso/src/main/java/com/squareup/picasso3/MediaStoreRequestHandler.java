package com.squareup.picasso3;
class MediaStoreRequestHandler extends ContentStreamRequestHandler {
  private static final String[] CONTENT_ORIENTATION = new String[] {
      Images.ImageColumns.ORIENTATION
  };
  MediaStoreRequestHandler(Context context) {
    super(context);
  }
  @Override public boolean canHandleRequest( Request data) {
    final Uri uri = data.uri;
    return uri != null
        && SCHEME_CONTENT.equals(uri.getScheme())
        && MediaStore.AUTHORITY.equals(uri.getAuthority());
  }
  @Override
  public void load( Picasso picasso,  Request request,  Callback callback) {
    boolean signaledCallback = false;
    try {
      ContentResolver contentResolver = context.getContentResolver();
      Uri requestUri = checkNotNull(request.uri, "request.uri == null");
      int exifOrientation = getExifOrientation(requestUri);
      String mimeType = contentResolver.getType(requestUri);
      boolean isVideo = mimeType != null && mimeType.startsWith("video/");
      if (request.hasSize()) {
        PicassoKind picassoKind = getPicassoKind(request.targetWidth, request.targetHeight);
        if (!isVideo && picassoKind == FULL) {
          Source source = getSource(requestUri);
          Bitmap bitmap = decodeStream(source, request);
          signaledCallback = true;
          callback.onSuccess(new Result(bitmap, DISK, exifOrientation));
          return;
        }
        long id = parseId(requestUri);
        BitmapFactory.Options options =
            checkNotNull(createBitmapOptions(request), "options == null");
        options.inJustDecodeBounds = true;
        calculateInSampleSize(request.targetWidth, request.targetHeight, picassoKind.width,
            picassoKind.height, options, request);
        Bitmap bitmap;
        if (isVideo) {
          int kind = (picassoKind == FULL) ? Video.Thumbnails.MINI_KIND : picassoKind.androidKind;
          bitmap = Video.Thumbnails.getThumbnail(contentResolver, id, kind, options);
        } else {
          bitmap =
              Images.Thumbnails.getThumbnail(contentResolver, id, picassoKind.androidKind, options);
        }
        if (bitmap != null) {
          signaledCallback = true;
          callback.onSuccess(new Result(bitmap, DISK, exifOrientation));
          return;
        }
      }
      Source source = getSource(requestUri);
      Bitmap bitmap = decodeStream(source, request);
      signaledCallback = true;
      callback.onSuccess(new Result(bitmap, DISK, exifOrientation));
    } catch (Exception e) {
      if (!signaledCallback) {
        callback.onError(e);
      }
    }
  }
  static PicassoKind getPicassoKind(int targetWidth, int targetHeight) {
    if (targetWidth <= MICRO.width && targetHeight <= MICRO.height) {
      return MICRO;
    } else if (targetWidth <= MINI.width && targetHeight <= MINI.height) {
      return MINI;
    }
    return FULL;
  }
  @Override
  protected int getExifOrientation(Uri uri) {
    Cursor cursor = null;
    try {
      ContentResolver contentResolver = context.getContentResolver();
      cursor = contentResolver.query(uri, CONTENT_ORIENTATION, null, null, null);
      if (cursor == null || !cursor.moveToFirst()) {
        return 0;
      }
      return cursor.getInt(0);
    } catch (RuntimeException ignored) {
      return 0;
    } finally {
      if (cursor != null) {
        cursor.close();
      }
    }
  }
  enum PicassoKind {
    MICRO(MICRO_KIND, 96, 96),
    MINI(MINI_KIND, 512, 384),
    FULL(FULL_SCREEN_KIND, -1, -1);
    final int androidKind;
    final int width;
    final int height;
    PicassoKind(int androidKind, int width, int height) {
      this.androidKind = androidKind;
      this.width = width;
      this.height = height;
    }
  }
}
