package com.squareup.picasso3;
final class Utils {
  static final String THREAD_PREFIX = "Picasso-";
  static final String THREAD_IDLE_NAME = THREAD_PREFIX + "Idle";
  private static final String PICASSO_CACHE = "picasso-cache";
  private static final int MIN_DISK_CACHE_SIZE = 5 * 1024 * 1024; 
  private static final int MAX_DISK_CACHE_SIZE = 50 * 1024 * 1024; 
  static final int THREAD_LEAK_CLEANING_MS = 1000;
  static final StringBuilder MAIN_THREAD_KEY_BUILDER = new StringBuilder();
  static final String OWNER_MAIN = "Main";
  static final String OWNER_DISPATCHER = "Dispatcher";
  static final String OWNER_HUNTER = "Hunter";
  static final String VERB_CREATED = "created";
  static final String VERB_CHANGED = "changed";
  static final String VERB_IGNORED = "ignored";
  static final String VERB_ENQUEUED = "enqueued";
  static final String VERB_CANCELED = "canceled";
  static final String VERB_RETRYING = "retrying";
  static final String VERB_EXECUTING = "executing";
  static final String VERB_DECODED = "decoded";
  static final String VERB_TRANSFORMED = "transformed";
  static final String VERB_JOINED = "joined";
  static final String VERB_REMOVED = "removed";
  static final String VERB_DELIVERED = "delivered";
  static final String VERB_REPLAYING = "replaying";
  static final String VERB_COMPLETED = "completed";
  static final String VERB_ERRORED = "errored";
  static final String VERB_PAUSED = "paused";
  static final String VERB_RESUMED = "resumed";
  private static final ByteString WEBP_FILE_HEADER_RIFF = ByteString.encodeUtf8("RIFF");
  private static final ByteString WEBP_FILE_HEADER_WEBP = ByteString.encodeUtf8("WEBP");
  private Utils() {
  }
  public static  <T> T checkNotNull( T value,  String message) {
    if (value == null) {
      throw new NullPointerException(message);
    }
    return value;
  }
  static void checkNotMain() {
    if (isMain()) {
      throw new IllegalStateException("Method call should not happen from the main thread.");
    }
  }
  static void checkMain() {
    if (!isMain()) {
      throw new IllegalStateException("Method call should happen from the main thread.");
    }
  }
  static boolean isMain() {
    return Looper.getMainLooper().getThread() == Thread.currentThread();
  }
  static String getLogIdsForHunter(BitmapHunter hunter) {
    return getLogIdsForHunter(hunter, "");
  }
  static String getLogIdsForHunter(BitmapHunter hunter, String prefix) {
    StringBuilder builder = new StringBuilder(prefix);
    Action action = hunter.getAction();
    if (action != null) {
      builder.append(action.request.logId());
    }
    List<Action> actions = hunter.getActions();
    if (actions != null) {
      for (int i = 0, count = actions.size(); i < count; i++) {
        if (i > 0 || action != null) builder.append(", ");
        builder.append(actions.get(i).request.logId());
      }
    }
    return builder.toString();
  }
  static void log(String owner, String verb, String logId) {
    log(owner, verb, logId, "");
  }
  static void log(String owner, String verb, String logId,  String extras) {
    if (extras == null) {
      extras = "";
    }
    Log.d(TAG, format("%1$-11s %2$-12s %3$s %4$s", owner, verb, logId, extras));
  }
  static File createDefaultCacheDir(Context context) {
    File cache = new File(context.getApplicationContext().getCacheDir(), PICASSO_CACHE);
    if (!cache.exists()) {
      cache.mkdirs();
    }
    return cache;
  }
  static long calculateDiskCacheSize(File dir) {
    long size = MIN_DISK_CACHE_SIZE;
    try {
      StatFs statFs = new StatFs(dir.getAbsolutePath());
      long blockCount =
          SDK_INT < JELLY_BEAN_MR2 ? (long) statFs.getBlockCount() : statFs.getBlockCountLong();
      long blockSize =
          SDK_INT < JELLY_BEAN_MR2 ? (long) statFs.getBlockSize() : statFs.getBlockSizeLong();
      long available = blockCount * blockSize;
      size = available / 50;
    } catch (IllegalArgumentException ignored) {
    }
    return Math.max(Math.min(size, MAX_DISK_CACHE_SIZE), MIN_DISK_CACHE_SIZE);
  }
  static int calculateMemoryCacheSize(Context context) {
    ActivityManager am = ContextCompat.getSystemService(context, ActivityManager.class);
    boolean largeHeap = (context.getApplicationInfo().flags & FLAG_LARGE_HEAP) != 0;
    int memoryClass = largeHeap ? am.getLargeMemoryClass() : am.getMemoryClass();
    return (int) (1024L * 1024L * memoryClass / 7);
  }
  static boolean isAirplaneModeOn(Context context) {
    ContentResolver contentResolver = context.getContentResolver();
    try {
      if (SDK_INT < JELLY_BEAN_MR1) {
        return Settings.System.getInt(contentResolver, Settings.System.AIRPLANE_MODE_ON, 0) != 0;
      }
      return Settings.Global.getInt(contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
    } catch (NullPointerException e) {
      return false;
    } catch (SecurityException e) {
      return false;
    }
  }
  static boolean hasPermission(Context context, String permission) {
    return context.checkCallingOrSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
  }
  static boolean isWebPFile(BufferedSource source) throws IOException {
    return source.rangeEquals(0, WEBP_FILE_HEADER_RIFF)
        && source.rangeEquals(8, WEBP_FILE_HEADER_WEBP);
  }
  static int getResourceId(Resources resources, Request data) throws FileNotFoundException {
    if (data.resourceId != 0 || data.uri == null) {
      return data.resourceId;
    }
    String pkg = data.uri.getAuthority();
    if (pkg == null) throw new FileNotFoundException("No package provided: " + data.uri);
    int id;
    List<String> segments = data.uri.getPathSegments();
    if (segments == null || segments.isEmpty()) {
      throw new FileNotFoundException("No path segments: " + data.uri);
    } else if (segments.size() == 1) {
      try {
        id = Integer.parseInt(segments.get(0));
      } catch (NumberFormatException e) {
        throw new FileNotFoundException("Last path segment is not a resource ID: " + data.uri);
      }
    } else if (segments.size() == 2) {
      String type = segments.get(0);
      String name = segments.get(1);
      id = resources.getIdentifier(name, type, pkg);
    } else {
      throw new FileNotFoundException("More than two path segments: " + data.uri);
    }
    return id;
  }
  static Resources getResources(Context context, Request data) throws FileNotFoundException {
    if (data.resourceId != 0 || data.uri == null) {
      return context.getResources();
    }
    String pkg = data.uri.getAuthority();
    if (pkg == null) throw new FileNotFoundException("No package provided: " + data.uri);
    try {
      PackageManager pm = context.getPackageManager();
      return pm.getResourcesForApplication(pkg);
    } catch (PackageManager.NameNotFoundException e) {
      throw new FileNotFoundException("Unable to obtain resources for package: " + data.uri);
    }
  }
  static void flushStackLocalLeaks(Looper looper) {
    Handler handler = new Handler(looper) {
      @Override public void handleMessage(Message msg) {
        sendMessageDelayed(obtainMessage(), THREAD_LEAK_CLEANING_MS);
      }
    };
    handler.sendMessageDelayed(handler.obtainMessage(), THREAD_LEAK_CLEANING_MS);
  }
  static class PicassoThreadFactory implements ThreadFactory {
    @Override public Thread newThread( Runnable r) {
      return new PicassoThread(r);
    }
  }
  private static class PicassoThread extends Thread {
    PicassoThread(Runnable r) {
      super(r);
    }
    @Override public void run() {
      Process.setThreadPriority(THREAD_PRIORITY_BACKGROUND);
      super.run();
    }
  }
}
