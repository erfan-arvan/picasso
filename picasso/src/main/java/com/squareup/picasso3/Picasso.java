package com.squareup.picasso3;
public class Picasso implements LifecycleObserver {
  public interface Listener {
    void onImageLoadFailed( Picasso picasso,  Uri uri,
         Exception exception);
  }
  public interface RequestTransformer {
     Request transformRequest( Request request);
  }
  public enum Priority {
    LOW,
    NORMAL,
    HIGH
  }
  static final String TAG = "Picasso";
  static final Handler HANDLER = new Handler(Looper.getMainLooper()) {
    @Override public void handleMessage(Message msg) {
      switch (msg.what) {
        case HUNTER_COMPLETE: {
          BitmapHunter hunter = (BitmapHunter) msg.obj;
          hunter.picasso.complete(hunter);
          break;
        }
        case REQUEST_BATCH_RESUME:
           List<Action> batch = (List<Action>) msg.obj;
          for (int i = 0, n = batch.size(); i < n; i++) {
            Action action = batch.get(i);
            action.picasso.resumeAction(action);
          }
          break;
        default:
          throw new AssertionError("Unknown handler message received: " + msg.what);
      }
    }
  };
  private final Listener listener;
  private final List<RequestTransformer> requestTransformers;
  private final List<RequestHandler> requestHandlers;
  final Context context;
  final Dispatcher dispatcher;
  private final  okhttp3.Cache closeableCache;
  final PlatformLruCache cache;
  final Stats stats;
  final Map<Object, Action> targetToAction;
  final Map<ImageView, DeferredRequestCreator> targetToDeferredRequestCreator;
  final Bitmap.Config defaultBitmapConfig;
  boolean indicatorsEnabled;
  volatile boolean loggingEnabled;
  boolean shutdown;
  Picasso(Context context, Dispatcher dispatcher, Call.Factory callFactory,
       okhttp3.Cache closeableCache, PlatformLruCache cache, Listener listener,
      List<RequestTransformer> requestTransformers, List<RequestHandler> extraRequestHandlers,
      Stats stats, Bitmap.Config defaultBitmapConfig, boolean indicatorsEnabled,
      boolean loggingEnabled) {
    this.context = context;
    this.dispatcher = dispatcher;
    this.closeableCache = closeableCache;
    this.cache = cache;
    this.listener = listener;
    this.requestTransformers = Collections.unmodifiableList(new ArrayList<>(requestTransformers));
    this.defaultBitmapConfig = defaultBitmapConfig;
    int builtInHandlers = 8; 
    int extraCount = extraRequestHandlers.size();
    List<RequestHandler> allRequestHandlers = new ArrayList<>(builtInHandlers + extraCount);
    allRequestHandlers.add(ResourceDrawableRequestHandler.create(context));
    allRequestHandlers.add(new ResourceRequestHandler(context));
    allRequestHandlers.addAll(extraRequestHandlers);
    allRequestHandlers.add(new ContactsPhotoRequestHandler(context));
    allRequestHandlers.add(new MediaStoreRequestHandler(context));
    allRequestHandlers.add(new ContentStreamRequestHandler(context));
    allRequestHandlers.add(new AssetRequestHandler(context));
    allRequestHandlers.add(new FileRequestHandler(context));
    allRequestHandlers.add(new NetworkRequestHandler(callFactory, stats));
    requestHandlers = Collections.unmodifiableList(allRequestHandlers);
    this.stats = stats;
    this.targetToAction = new LinkedHashMap<>();
    this.targetToDeferredRequestCreator = new LinkedHashMap<>();
    this.indicatorsEnabled = indicatorsEnabled;
    this.loggingEnabled = loggingEnabled;
  }
  @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
  void cancelAll() {
    checkMain();
    List<Action> actions = new ArrayList<>(targetToAction.values());
    for (int i = 0, n = actions.size(); i < n; i++) {
      Action action = actions.get(i);
      cancelExistingRequest(action.getTarget());
    }
    List<DeferredRequestCreator> deferredRequestCreators =
        new ArrayList<>(targetToDeferredRequestCreator.values());
    for (int i = 0, n = deferredRequestCreators.size(); i < n; i++) {
      DeferredRequestCreator deferredRequestCreator = deferredRequestCreators.get(i);
      deferredRequestCreator.cancel();
    }
  }
  public void cancelRequest( ImageView view) {
    checkNotNull(view, "view == null");
    cancelExistingRequest(view);
  }
  public void cancelRequest( BitmapTarget target) {
    checkNotNull(target, "target == null");
    cancelExistingRequest(target);
  }
  public void cancelRequest( RemoteViews remoteViews, @IdRes int viewId) {
    checkNotNull(remoteViews, "remoteViews == null");
    cancelExistingRequest(new RemoteViewsAction.RemoteViewsTarget(remoteViews, viewId));
  }
  public void cancelTag( Object tag) {
    checkMain();
    checkNotNull(tag, "tag == null");
    List<Action> actions = new ArrayList<>(targetToAction.values());
    for (int i = 0, n = actions.size(); i < n; i++) {
      Action action = actions.get(i);
      if (tag.equals(action.getTag())) {
        cancelExistingRequest(action.getTarget());
      }
    }
    List<DeferredRequestCreator> deferredRequestCreators =
        new ArrayList<>(targetToDeferredRequestCreator.values());
    for (int i = 0, n = deferredRequestCreators.size(); i < n; i++) {
      DeferredRequestCreator deferredRequestCreator = deferredRequestCreators.get(i);
      if (tag.equals(deferredRequestCreator.getTag())) {
        deferredRequestCreator.cancel();
      }
    }
  }
  @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
  void pauseAll() {
    checkMain();
    List<Action> actions = new ArrayList<>(targetToAction.values());
    for (int i = 0, n = actions.size(); i < n; i++) {
      Action action = actions.get(i);
      dispatcher.dispatchPauseTag(action.getTag());
    }
    List<DeferredRequestCreator> deferredRequestCreators =
        new ArrayList<>(targetToDeferredRequestCreator.values());
    for (int i = 0, n = deferredRequestCreators.size(); i < n; i++) {
      DeferredRequestCreator deferredRequestCreator = deferredRequestCreators.get(i);
      dispatcher.dispatchPauseTag(deferredRequestCreator.getTag());
    }
  }
  public void pauseTag( Object tag) {
    checkNotNull(tag, "tag == null");
    dispatcher.dispatchPauseTag(tag);
  }
  @OnLifecycleEvent(Lifecycle.Event.ON_START)
  void resumeAll() {
    checkMain();
    List<Action> actions = new ArrayList<>(targetToAction.values());
    for (int i = 0, n = actions.size(); i < n; i++) {
      Action action = actions.get(i);
      dispatcher.dispatchResumeTag(action.getTag());
    }
    List<DeferredRequestCreator> deferredRequestCreators =
        new ArrayList<>(targetToDeferredRequestCreator.values());
    for (int i = 0, n = deferredRequestCreators.size(); i < n; i++) {
      DeferredRequestCreator deferredRequestCreator = deferredRequestCreators.get(i);
      dispatcher.dispatchResumeTag(deferredRequestCreator.getTag());
    }
  }
  public void resumeTag( Object tag) {
    checkNotNull(tag, "tag == null");
    dispatcher.dispatchResumeTag(tag);
  }
  public RequestCreator load( Uri uri) {
    return new RequestCreator(this, uri, 0);
  }
  public RequestCreator load( String path) {
    if (path == null) {
      return new RequestCreator(this, null, 0);
    }
    if (path.trim().length() == 0) {
      throw new IllegalArgumentException("Path must not be empty.");
    }
    return load(Uri.parse(path));
  }
  public RequestCreator load( File file) {
    if (file == null) {
      return new RequestCreator(this, null, 0);
    }
    return load(Uri.fromFile(file));
  }
  public RequestCreator load(@DrawableRes int resourceId) {
    if (resourceId == 0) {
      throw new IllegalArgumentException("Resource ID must not be zero.");
    }
    return new RequestCreator(this, null, resourceId);
  }
  public void evictAll() {
    cache.clear();
  }
  public void invalidate( Uri uri) {
    if (uri != null) {
      cache.clearKeyUri(uri.toString());
    }
  }
  public void invalidate( String path) {
    if (path != null) {
      invalidate(Uri.parse(path));
    }
  }
  public void invalidate( File file) {
    checkNotNull(file, "file == null");
    invalidate(Uri.fromFile(file));
  }
   public void setIndicatorsEnabled(boolean enabled) {
    indicatorsEnabled = enabled;
  }
   public boolean getIndicatorsEnabled() {
    return indicatorsEnabled;
  }
  public void setLoggingEnabled(boolean enabled) {
    loggingEnabled = enabled;
  }
  public boolean isLoggingEnabled() {
    return loggingEnabled;
  }
   public StatsSnapshot getSnapshot() {
    return stats.createSnapshot();
  }
  public void shutdown() {
    if (shutdown) {
      return;
    }
    cache.clear();
    stats.shutdown();
    dispatcher.shutdown();
    if (closeableCache != null) {
      try {
        closeableCache.close();
      } catch (IOException ignored) {
      }
    }
    for (DeferredRequestCreator deferredRequestCreator : targetToDeferredRequestCreator.values()) {
      deferredRequestCreator.cancel();
    }
    targetToDeferredRequestCreator.clear();
    shutdown = true;
  }
  List<RequestHandler> getRequestHandlers() {
    return requestHandlers;
  }
  Request transformRequest(Request request) {
    for (int i = 0, size = requestTransformers.size(); i < size; i++) {
      RequestTransformer transformer = requestTransformers.get(i);
      Request transformed = transformer.transformRequest(request);
      if (transformed == null) {
        throw new IllegalStateException("Request transformer "
            + transformer.getClass().getCanonicalName()
            + " returned null for "
            + request);
      }
      request = transformed;
    }
    return request;
  }
  void defer(ImageView view, DeferredRequestCreator request) {
    if (targetToDeferredRequestCreator.containsKey(view)) {
      cancelExistingRequest(view);
    }
    targetToDeferredRequestCreator.put(view, request);
  }
  void enqueueAndSubmit(Action action) {
    Object target = action.getTarget();
    if (targetToAction.get(target) != action) {
      cancelExistingRequest(target);
      targetToAction.put(target, action);
    }
    submit(action);
  }
  void submit(Action action) {
    dispatcher.dispatchSubmit(action);
  }
  Bitmap quickMemoryCacheCheck(String key) {
    Bitmap cached = cache.get(key);
    if (cached != null) {
      stats.dispatchCacheHit();
    } else {
      stats.dispatchCacheMiss();
    }
    return cached;
  }
  void complete(BitmapHunter hunter) {
    Action single = hunter.getAction();
    List<Action> joined = hunter.getActions();
    boolean hasMultiple = joined != null && !joined.isEmpty();
    boolean shouldDeliver = single != null || hasMultiple;
    if (!shouldDeliver) {
      return;
    }
    Uri uri = hunter.getData().uri;
    Exception exception = hunter.getException();
    RequestHandler.Result result = hunter.getResult();
    if (single != null) {
      deliverAction(result, single, exception);
    }
    if (hasMultiple) {
      for (int i = 0, n = joined.size(); i < n; i++) {
        Action join = joined.get(i);
        deliverAction(result, join, exception);
      }
    }
    if (listener != null && exception != null) {
      listener.onImageLoadFailed(this, uri, exception);
    }
  }
  void resumeAction(Action action) {
    Bitmap bitmap = null;
    if (shouldReadFromMemoryCache(action.request.memoryPolicy)) {
      bitmap = quickMemoryCacheCheck(action.getKey());
    }
    if (bitmap != null) {
      deliverAction(new RequestHandler.Result(bitmap, MEMORY), action, null);
      if (loggingEnabled) {
        log(OWNER_MAIN, VERB_COMPLETED, action.request.logId(), "from " + MEMORY);
      }
    } else {
      enqueueAndSubmit(action);
      if (loggingEnabled) {
        log(OWNER_MAIN, VERB_RESUMED, action.request.logId());
      }
    }
  }
  private void deliverAction(RequestHandler.Result result, Action action, Exception e) {
    if (action.isCancelled()) {
      return;
    }
    if (!action.willReplay()) {
      targetToAction.remove(action.getTarget());
    }
    if (result != null) {
      action.complete(result);
      if (loggingEnabled) {
        log(OWNER_MAIN, VERB_COMPLETED, action.request.logId(), "from " + result.getLoadedFrom());
      }
    } else {
      action.error(e);
      if (loggingEnabled) {
        log(OWNER_MAIN, VERB_ERRORED, action.request.logId(), e.getMessage());
      }
    }
  }
  void cancelExistingRequest(Object target) {
    checkMain();
    Action action = targetToAction.remove(target);
    if (action != null) {
      action.cancel();
      dispatcher.dispatchCancel(action);
    }
    if (target instanceof ImageView) {
      ImageView targetImageView = (ImageView) target;
      DeferredRequestCreator deferredRequestCreator =
          targetToDeferredRequestCreator.remove(targetImageView);
      if (deferredRequestCreator != null) {
        deferredRequestCreator.cancel();
      }
    }
  }
  public static class Builder {
    private final Context context;
    private Call.Factory callFactory;
    private ExecutorService service;
    private PlatformLruCache cache;
    private Listener listener;
    private final List<RequestTransformer> requestTransformers = new ArrayList<>();
    private final List<RequestHandler> requestHandlers = new ArrayList<>();
    private Bitmap.Config defaultBitmapConfig;
    private boolean indicatorsEnabled;
    private boolean loggingEnabled;
    public Builder( Context context) {
      checkNotNull(context, "context == null");
      this.context = context.getApplicationContext();
    }
    public Builder defaultBitmapConfig( Bitmap.Config bitmapConfig) {
      checkNotNull(bitmapConfig, "bitmapConfig == null");
      this.defaultBitmapConfig = bitmapConfig;
      return this;
    }
    public Builder client( OkHttpClient client) {
      checkNotNull(client, "client == null");
      callFactory = client;
      return this;
    }
    public Builder callFactory( Call.Factory factory) {
      checkNotNull(factory, "factory == null");
      callFactory = factory;
      return this;
    }
    public Builder executor( ExecutorService executorService) {
      checkNotNull(executorService, "executorService == null");
      if (this.service != null) {
        throw new IllegalStateException("Executor service already set.");
      }
      this.service = executorService;
      return this;
    }
    public Builder withCacheSize(int maxByteCount) {
      if (maxByteCount < 0) {
        throw new IllegalArgumentException("maxByteCount < 0: " + maxByteCount);
      }
      cache = new PlatformLruCache(maxByteCount);
      return this;
    }
    public Builder listener( Listener listener) {
      checkNotNull(listener, "listener == null");
      if (this.listener != null) {
        throw new IllegalStateException("Listener already set.");
      }
      this.listener = listener;
      return this;
    }
    public Builder addRequestTransformer( RequestTransformer transformer) {
      checkNotNull(transformer, "transformer == null");
      if (requestTransformers.contains(transformer)) {
        throw new IllegalStateException("Transformer already set.");
      }
      requestTransformers.add(transformer);
      return this;
    }
    public Builder addRequestHandler( RequestHandler requestHandler) {
      checkNotNull(requestHandler, "requestHandler == null");
      if (requestHandlers.contains(requestHandler)) {
        throw new IllegalStateException("RequestHandler already registered.");
      }
      requestHandlers.add(requestHandler);
      return this;
    }
    public Builder indicatorsEnabled(boolean enabled) {
      this.indicatorsEnabled = enabled;
      return this;
    }
    public Builder loggingEnabled(boolean enabled) {
      this.loggingEnabled = enabled;
      return this;
    }
    public Picasso build() {
      Context context = this.context;
      okhttp3.Cache unsharedCache = null;
      if (callFactory == null) {
        File cacheDir = createDefaultCacheDir(context);
        long maxSize = calculateDiskCacheSize(cacheDir);
        unsharedCache = new okhttp3.Cache(cacheDir, maxSize);
        callFactory = new OkHttpClient.Builder()
            .cache(unsharedCache)
            .build();
      }
      if (cache == null) {
        cache = new PlatformLruCache(Utils.calculateMemoryCacheSize(context));
      }
      if (service == null) {
        service = new PicassoExecutorService();
      }
      Stats stats = new Stats(cache);
      Dispatcher dispatcher = new Dispatcher(context, service, HANDLER, cache, stats);
      return new Picasso(context, dispatcher, callFactory, unsharedCache, cache, listener,
          requestTransformers, requestHandlers, stats, defaultBitmapConfig, indicatorsEnabled,
          loggingEnabled);
    }
  }
  public enum LoadedFrom {
    MEMORY(Color.GREEN),
    DISK(Color.BLUE),
    NETWORK(Color.RED);
    final int debugColor;
    LoadedFrom(int debugColor) {
      this.debugColor = debugColor;
    }
  }
}
