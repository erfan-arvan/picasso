package com.squareup.picasso3;
public enum NetworkPolicy {
  NO_CACHE(1 << 0),
  NO_STORE(1 << 1),
  OFFLINE(1 << 2);
  public static boolean shouldReadFromDiskCache(int networkPolicy) {
    return (networkPolicy & NetworkPolicy.NO_CACHE.index) == 0;
  }
  public static boolean shouldWriteToDiskCache(int networkPolicy) {
    return (networkPolicy & NetworkPolicy.NO_STORE.index) == 0;
  }
  public static boolean isOfflineOnly(int networkPolicy) {
    return (networkPolicy & NetworkPolicy.OFFLINE.index) != 0;
  }
  final int index;
  NetworkPolicy(int index) {
    this.index = index;
  }
}
