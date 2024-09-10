package com.squareup.picasso3;
public enum MemoryPolicy {
  NO_CACHE(1 << 0),
  NO_STORE(1 << 1);
  static boolean shouldReadFromMemoryCache(int memoryPolicy) {
    return (memoryPolicy & MemoryPolicy.NO_CACHE.index) == 0;
  }
  static boolean shouldWriteToMemoryCache(int memoryPolicy) {
    return (memoryPolicy & MemoryPolicy.NO_STORE.index) == 0;
  }
  final int index;
  MemoryPolicy(int index) {
    this.index = index;
  }
}
