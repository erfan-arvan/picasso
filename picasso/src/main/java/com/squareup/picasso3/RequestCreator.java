package com.squareup.picasso3;
public class RequestCreator {
  private static final AtomicInteger nextId = new AtomicInteger();
  private final Picasso picasso;
  private final Request.Builder data;
  private boolean noFade;
  private boolean deferred;
  private boolean setPlaceholder = true;
  private int placeholderResId;
  private int errorResId;
   private Drawable placeholderDrawable;
   private Drawable errorDrawable;
  RequestCreator(Picasso picasso,  Uri uri, int resourceId) {
    if (picasso.shutdown) {
      throw new IllegalStateException(
          "Picasso instance already shut down. Cannot submit new requests.");
    }
    this.picasso = picasso;
    this.data = new Request.Builder(uri, resourceId, picasso.defaultBitmapConfig);
  }
  @VisibleForTesting RequestCreator() {
    this.picasso = null;
    this.data = new Request.Builder(null, 0, null);
  }
  public RequestCreator noPlaceholder() {
    if (placeholderResId != 0) {
      throw new IllegalStateException("Placeholder resource already set.");
    }
    if (placeholderDrawable != null) {
      throw new IllegalStateException("Placeholder image already set.");
    }
    setPlaceholder = false;
    return this;
  }
  public RequestCreator placeholder(@DrawableRes int placeholderResId) {
    if (!setPlaceholder) {
      throw new IllegalStateException("Already explicitly declared as no placeholder.");
    }
    if (placeholderResId == 0) {
      throw new IllegalArgumentException("Placeholder image resource invalid.");
    }
    if (placeholderDrawable != null) {
      throw new IllegalStateException("Placeholder image already set.");
    }
    this.placeholderResId = placeholderResId;
    return this;
  }
  public RequestCreator placeholder( Drawable placeholderDrawable) {
    if (!setPlaceholder) {
      throw new IllegalStateException("Already explicitly declared as no placeholder.");
    }
    if (placeholderResId != 0) {
      throw new IllegalStateException("Placeholder image already set.");
    }
    this.placeholderDrawable = placeholderDrawable;
    return this;
  }
  public RequestCreator error(@DrawableRes int errorResId) {
    if (errorResId == 0) {
      throw new IllegalArgumentException("Error image resource invalid.");
    }
    if (errorDrawable != null) {
      throw new IllegalStateException("Error image already set.");
    }
    this.errorResId = errorResId;
    return this;
  }
  public RequestCreator error( Drawable errorDrawable) {
    if (errorDrawable == null) {
      throw new IllegalArgumentException("Error image may not be null.");
    }
    if (errorResId != 0) {
      throw new IllegalStateException("Error image already set.");
    }
    this.errorDrawable = errorDrawable;
    return this;
  }
  public RequestCreator tag( Object tag) {
    data.tag(tag);
    return this;
  }
  public RequestCreator fit() {
    deferred = true;
    return this;
  }
  RequestCreator unfit() {
    deferred = false;
    return this;
  }
  RequestCreator clearTag() {
    data.clearTag();
    return this;
  }
   Object getTag() {
    return data.getTag();
  }
  public RequestCreator resizeDimen(int targetWidthResId, int targetHeightResId) {
    Resources resources = picasso.context.getResources();
    int targetWidth = resources.getDimensionPixelSize(targetWidthResId);
    int targetHeight = resources.getDimensionPixelSize(targetHeightResId);
    return resize(targetWidth, targetHeight);
  }
  public RequestCreator resize(int targetWidth, int targetHeight) {
    data.resize(targetWidth, targetHeight);
    return this;
  }
  public RequestCreator centerCrop() {
    data.centerCrop(Gravity.CENTER);
    return this;
  }
  public RequestCreator centerCrop(int alignGravity) {
    data.centerCrop(alignGravity);
    return this;
  }
  public RequestCreator centerInside() {
    data.centerInside();
    return this;
  }
  public RequestCreator onlyScaleDown() {
    data.onlyScaleDown();
    return this;
  }
  public RequestCreator rotate(float degrees) {
    data.rotate(degrees);
    return this;
  }
  public RequestCreator rotate(float degrees, float pivotX, float pivotY) {
    data.rotate(degrees, pivotX, pivotY);
    return this;
  }
  public RequestCreator config( Bitmap.Config config) {
    data.config(config);
    return this;
  }
  public RequestCreator stableKey( String stableKey) {
    data.stableKey(stableKey);
    return this;
  }
  public RequestCreator priority( Priority priority) {
    data.priority(priority);
    return this;
  }
  public RequestCreator transform( Transformation transformation) {
    data.transform(transformation);
    return this;
  }
  public RequestCreator transform( List<? extends Transformation> transformations) {
    data.transform(transformations);
    return this;
  }
  public RequestCreator memoryPolicy( MemoryPolicy policy,
       MemoryPolicy... additional) {
    data.memoryPolicy(policy, additional);
    return this;
  }
  public RequestCreator networkPolicy( NetworkPolicy policy,
       NetworkPolicy... additional) {
    data.networkPolicy(policy, additional);
    return this;
  }
  public RequestCreator purgeable() {
    data.purgeable();
    return this;
  }
  public RequestCreator noFade() {
    noFade = true;
    return this;
  }
  public Bitmap get() throws IOException {
    long started = System.nanoTime();
    checkNotMain();
    if (deferred) {
      throw new IllegalStateException("Fit cannot be used with get.");
    }
    if (!data.hasImage()) {
      return null;
    }
    Request request = createRequest(started);
    Action action = new GetAction(picasso, request);
    RequestHandler.Result result =
        forRequest(picasso, picasso.dispatcher, picasso.cache, picasso.stats, action).hunt();
    Bitmap bitmap = result.getBitmap();
    if (bitmap != null && shouldWriteToMemoryCache(request.memoryPolicy)) {
      picasso.cache.set(request.key, bitmap);
    }
    return bitmap;
  }
  public void fetch() {
    fetch(null);
  }
  public void fetch( Callback callback) {
    long started = System.nanoTime();
    if (deferred) {
      throw new IllegalStateException("Fit cannot be used with fetch.");
    }
    if (data.hasImage()) {
      if (!data.hasPriority()) {
        data.priority(Priority.LOW);
      }
      Request request = createRequest(started);
      if (shouldReadFromMemoryCache(request.memoryPolicy)) {
        Bitmap bitmap = picasso.quickMemoryCacheCheck(request.key);
        if (bitmap != null) {
          if (picasso.loggingEnabled) {
            log(OWNER_MAIN, VERB_COMPLETED, request.plainId(), "from " + MEMORY);
          }
          if (callback != null) {
            callback.onSuccess();
          }
          return;
        }
      }
      Action action = new FetchAction(picasso, request, callback);
      picasso.submit(action);
    }
  }
  public void into( BitmapTarget target) {
    long started = System.nanoTime();
    checkMain();
    if (target == null) {
      throw new IllegalArgumentException("Target must not be null.");
    }
    if (deferred) {
      throw new IllegalStateException("Fit cannot be used with a Target.");
    }
    if (!data.hasImage()) {
      picasso.cancelRequest(target);
      target.onPrepareLoad(setPlaceholder ? getPlaceholderDrawable() : null);
      return;
    }
    Request request = createRequest(started);
    if (shouldReadFromMemoryCache(request.memoryPolicy)) {
      Bitmap bitmap = picasso.quickMemoryCacheCheck(request.key);
      if (bitmap != null) {
        picasso.cancelRequest(target);
        target.onBitmapLoaded(bitmap, MEMORY);
        return;
      }
    }
    target.onPrepareLoad(setPlaceholder ? getPlaceholderDrawable() : null);
    Target<BitmapTarget> wrapper = new Target<>(target, errorResId, errorDrawable, noFade);
    Action action = new BitmapTargetAction(picasso, wrapper, request);
    picasso.enqueueAndSubmit(action);
  }
  public void into( RemoteViews remoteViews, @IdRes int viewId, int notificationId,
       Notification notification) {
    into(remoteViews, viewId, notificationId, notification, null);
  }
  public void into( RemoteViews remoteViews, @IdRes int viewId, int notificationId,
       Notification notification,  String notificationTag) {
    into(remoteViews, viewId, notificationId, notification, notificationTag, null);
  }
  public void into( RemoteViews remoteViews, @IdRes int viewId, int notificationId,
       Notification notification,  String notificationTag,
       Callback callback) {
    long started = System.nanoTime();
    if (remoteViews == null) {
      throw new IllegalArgumentException("RemoteViews must not be null.");
    }
    if (notification == null) {
      throw new IllegalArgumentException("Notification must not be null.");
    }
    if (deferred) {
      throw new IllegalStateException("Fit cannot be used with RemoteViews.");
    }
    if (placeholderDrawable != null || errorDrawable != null) {
      throw new IllegalArgumentException(
          "Cannot use placeholder or error drawables with remote views.");
    }
    Request request = createRequest(started);
    Target<RemoteViewsTarget> remoteTarget =
        new Target<>(new RemoteViewsTarget(remoteViews, viewId), errorResId);
    RemoteViewsAction action =
        new NotificationAction(picasso, request, remoteTarget, notificationId, notification,
            notificationTag, callback);
    performRemoteViewInto(request, action);
  }
  public void into( RemoteViews remoteViews, @IdRes int viewId, int appWidgetId) {
    into(remoteViews, viewId, new int[] { appWidgetId }, null);
  }
  public void into( RemoteViews remoteViews, @IdRes int viewId,
       int[] appWidgetIds) {
    into(remoteViews, viewId, appWidgetIds, null);
  }
  public void into( RemoteViews remoteViews, @IdRes int viewId, int appWidgetId,
       Callback callback) {
    into(remoteViews, viewId, new int[] { appWidgetId }, callback);
  }
  public void into( RemoteViews remoteViews, @IdRes int viewId,  int[] appWidgetIds,
       Callback callback) {
    long started = System.nanoTime();
    if (remoteViews == null) {
      throw new IllegalArgumentException("remoteViews must not be null.");
    }
    if (appWidgetIds == null) {
      throw new IllegalArgumentException("appWidgetIds must not be null.");
    }
    if (deferred) {
      throw new IllegalStateException("Fit cannot be used with remote views.");
    }
    if (placeholderDrawable != null || errorDrawable != null) {
      throw new IllegalArgumentException(
          "Cannot use placeholder or error drawables with remote views.");
    }
    Request request = createRequest(started);
    Target<RemoteViewsTarget> remoteTarget =
        new Target<>(new RemoteViewsTarget(remoteViews, viewId), errorResId);
    RemoteViewsAction action =
        new AppWidgetAction(picasso, request, remoteTarget, appWidgetIds, callback);
    performRemoteViewInto(request, action);
  }
  public void into( ImageView target) {
    into(target, null);
  }
  public void into( ImageView target,  Callback callback) {
    long started = System.nanoTime();
    checkMain();
    if (target == null) {
      throw new IllegalArgumentException("Target must not be null.");
    }
    if (!data.hasImage()) {
      picasso.cancelRequest(target);
      if (setPlaceholder) {
        setPlaceholder(target, getPlaceholderDrawable());
      }
      return;
    }
    if (deferred) {
      if (data.hasSize()) {
        throw new IllegalStateException("Fit cannot be used with resize.");
      }
      int width = target.getWidth();
      int height = target.getHeight();
      if (width == 0 || height == 0) {
        if (setPlaceholder) {
          setPlaceholder(target, getPlaceholderDrawable());
        }
        picasso.defer(target, new DeferredRequestCreator(this, target, callback));
        return;
      }
      data.resize(width, height);
    }
    Request request = createRequest(started);
    if (shouldReadFromMemoryCache(request.memoryPolicy)) {
      Bitmap bitmap = picasso.quickMemoryCacheCheck(request.key);
      if (bitmap != null) {
        picasso.cancelRequest(target);
        RequestHandler.Result result = new RequestHandler.Result(bitmap, MEMORY);
        setResult(target, picasso.context, result, noFade, picasso.indicatorsEnabled);
        if (picasso.loggingEnabled) {
          log(OWNER_MAIN, VERB_COMPLETED, request.plainId(), "from " + MEMORY);
        }
        if (callback != null) {
          callback.onSuccess();
        }
        return;
      }
    }
    if (setPlaceholder) {
      setPlaceholder(target, getPlaceholderDrawable());
    }
    Target<ImageView> wrapper = new Target<>(target, errorResId, errorDrawable, noFade);
    Action action = new ImageViewAction(picasso, wrapper, request, callback);
    picasso.enqueueAndSubmit(action);
  }
  private  Drawable getPlaceholderDrawable() {
    return placeholderResId == 0
        ? placeholderDrawable
        : ContextCompat.getDrawable(picasso.context, placeholderResId);
  }
  private Request createRequest(long started) {
    int id = nextId.getAndIncrement();
    Request request = data.build();
    request.id = id;
    request.started = started;
    boolean loggingEnabled = picasso.loggingEnabled;
    if (loggingEnabled) {
      log(OWNER_MAIN, VERB_CREATED, request.plainId(), request.toString());
    }
    Request transformed = picasso.transformRequest(request);
    if (transformed != request) {
      transformed.id = id;
      transformed.started = started;
      if (loggingEnabled) {
        log(OWNER_MAIN, VERB_CHANGED, transformed.logId(), "into " + transformed);
      }
    }
    return transformed;
  }
  private void performRemoteViewInto(Request request, RemoteViewsAction action) {
    if (shouldReadFromMemoryCache(request.memoryPolicy)) {
      Bitmap bitmap = picasso.quickMemoryCacheCheck(action.getKey());
      if (bitmap != null) {
        action.complete(new RequestHandler.Result(bitmap, MEMORY));
        return;
      }
    }
    if (placeholderResId != 0) {
      action.setImageResource(placeholderResId);
    }
    picasso.enqueueAndSubmit(action);
  }
}
