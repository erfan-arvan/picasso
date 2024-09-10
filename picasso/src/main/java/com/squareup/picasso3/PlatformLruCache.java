package com.squareup.picasso3;
final class PlatformLruCache {
  final LruCache<String, BitmapAndSize> cache;
  PlatformLruCache(int maxByteCount) {
    cache = new LruCache<String, BitmapAndSize>(maxByteCount != 0 ? maxByteCount : 1) {
      @Override protected int sizeOf(String key, BitmapAndSize value) {
        return value.byteCount;
      }
    };
  }
   public Bitmap get( String key) {
    BitmapAndSize bitmapAndSize = cache.get(key);
    return bitmapAndSize != null ? bitmapAndSize.bitmap : null;
  }
  void set( String key,  Bitmap bitmap) {
    if (key == null || bitmap == null) {
      throw new NullPointerException("key == null || bitmap == null");
    }
    int byteCount = BitmapCompat.getAllocationByteCount(bitmap);
    if (byteCount > maxSize()) {
      cache.remove(key);
      return;
    }
    cache.put(key, new BitmapAndSize(bitmap, byteCount));
  }
  int size() {
    return cache.size();
  }
  int maxSize() {
    return cache.maxSize();
  }
  void clear() {
    cache.evictAll();
  }
  void clearKeyUri(String uri) {
    for (String key : cache.snapshot().keySet()) {
      if (key.startsWith(uri)
          && key.length() > uri.length()
          && key.charAt(uri.length()) == KEY_SEPARATOR) {
        cache.remove(key);
      }
    }
  }
  int hitCount() {
    return cache.hitCount();
  }
  int missCount() {
    return cache.missCount();
  }
  int putCount() {
    return cache.putCount();
  }
  int evictionCount() {
    return cache.evictionCount();
  }
  static final class BitmapAndSize {
    final Bitmap bitmap;
    final int byteCount;
    BitmapAndSize(Bitmap bitmap, int byteCount) {
      this.bitmap = bitmap;
      this.byteCount = byteCount;
    }
  }
}
