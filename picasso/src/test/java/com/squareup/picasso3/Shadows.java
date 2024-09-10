package com.squareup.picasso3;
final class Shadows {
  @Implements(MediaStore.Video.Thumbnails.class)
  public static class ShadowVideoThumbnails {
    @Implementation
    public static Bitmap getThumbnail(ContentResolver cr, long origId, int kind,
        BitmapFactory.Options options) {
      return makeBitmap();
    }
    private ShadowVideoThumbnails() {
    }
  }
  @Implements(MediaStore.Images.Thumbnails.class)
  public static class ShadowImageThumbnails {
    @Implementation
    public static Bitmap getThumbnail(ContentResolver cr, long origId, int kind,
        BitmapFactory.Options options) {
      return makeBitmap(20, 20);
    }
    private ShadowImageThumbnails() {
    }
  }
  private Shadows() {
  }
}
