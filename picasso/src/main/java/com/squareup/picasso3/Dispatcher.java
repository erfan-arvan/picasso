package com.squareup.picasso3;
class Dispatcher {
  private static final int RETRY_DELAY = 500;
  private static final int AIRPLANE_MODE_ON = 1;
  private static final int AIRPLANE_MODE_OFF = 0;
  static final int REQUEST_SUBMIT = 1;
  static final int REQUEST_CANCEL = 2;
  static final int HUNTER_COMPLETE = 4;
  static final int HUNTER_RETRY = 5;
  static final int HUNTER_DECODE_FAILED = 6;
  static final int NETWORK_STATE_CHANGE = 9;
  static final int AIRPLANE_MODE_CHANGE = 10;
  static final int TAG_PAUSE = 11;
  static final int TAG_RESUME = 12;
  static final int REQUEST_BATCH_RESUME = 13;
  private static final String DISPATCHER_THREAD_NAME = "Dispatcher";
  final DispatcherThread dispatcherThread;
  final Context context;
  final ExecutorService service;
  final Map<String, BitmapHunter> hunterMap;
  final Map<Object, Action> failedActions;
  final Map<Object, Action> pausedActions;
  final Set<Object> pausedTags;
  final Handler handler;
  final Handler mainThreadHandler;
  final PlatformLruCache cache;
  final Stats stats;
  final NetworkBroadcastReceiver receiver;
  final boolean scansNetworkChanges;
  boolean airplaneMode;
  Dispatcher(Context context, ExecutorService service, Handler mainThreadHandler,
      PlatformLruCache cache, Stats stats) {
    this.dispatcherThread = new DispatcherThread();
    this.dispatcherThread.start();
    Utils.flushStackLocalLeaks(dispatcherThread.getLooper());
    this.context = context;
    this.service = service;
    this.hunterMap = new LinkedHashMap<>();
    this.failedActions = new LinkedHashMap<>();
    this.pausedActions = new LinkedHashMap<>();
    this.pausedTags = new LinkedHashSet<>();
    this.handler = new DispatcherHandler(dispatcherThread.getLooper(), this);
    this.mainThreadHandler = mainThreadHandler;
    this.cache = cache;
    this.stats = stats;
    this.airplaneMode = Utils.isAirplaneModeOn(this.context);
    this.scansNetworkChanges = hasPermission(context, Manifest.permission.ACCESS_NETWORK_STATE);
    this.receiver = new NetworkBroadcastReceiver(this);
    receiver.register();
  }
  void shutdown() {
    if (service instanceof PicassoExecutorService) {
      service.shutdown();
    }
    dispatcherThread.quit();
    Picasso.HANDLER.post(new Runnable() {
      @Override public void run() {
        receiver.unregister();
      }
    });
  }
  void dispatchSubmit(Action action) {
    handler.sendMessage(handler.obtainMessage(REQUEST_SUBMIT, action));
  }
  void dispatchCancel(Action action) {
    handler.sendMessage(handler.obtainMessage(REQUEST_CANCEL, action));
  }
  void dispatchPauseTag(Object tag) {
    handler.sendMessage(handler.obtainMessage(TAG_PAUSE, tag));
  }
  void dispatchResumeTag(Object tag) {
    handler.sendMessage(handler.obtainMessage(TAG_RESUME, tag));
  }
  void dispatchComplete(BitmapHunter hunter) {
    handler.sendMessage(handler.obtainMessage(HUNTER_COMPLETE, hunter));
  }
  void dispatchRetry(BitmapHunter hunter) {
    handler.sendMessageDelayed(handler.obtainMessage(HUNTER_RETRY, hunter), RETRY_DELAY);
  }
  void dispatchFailed(BitmapHunter hunter) {
    handler.sendMessage(handler.obtainMessage(HUNTER_DECODE_FAILED, hunter));
  }
  void dispatchNetworkStateChange(NetworkInfo info) {
    handler.sendMessage(handler.obtainMessage(NETWORK_STATE_CHANGE, info));
  }
  void dispatchAirplaneModeChange(boolean airplaneMode) {
    handler.sendMessage(handler.obtainMessage(AIRPLANE_MODE_CHANGE,
        airplaneMode ? AIRPLANE_MODE_ON : AIRPLANE_MODE_OFF, 0));
  }
  void performSubmit(Action action) {
    performSubmit(action, true);
  }
  void performSubmit(Action action, boolean dismissFailed) {
    if (pausedTags.contains(action.getTag())) {
      pausedActions.put(action.getTarget(), action);
      if (action.getPicasso().loggingEnabled) {
        log(OWNER_DISPATCHER, VERB_PAUSED, action.request.logId(),
            "because tag '" + action.getTag() + "' is paused");
      }
      return;
    }
    BitmapHunter hunter = hunterMap.get(action.getKey());
    if (hunter != null) {
      hunter.attach(action);
      return;
    }
    if (service.isShutdown()) {
      if (action.getPicasso().loggingEnabled) {
        log(OWNER_DISPATCHER, VERB_IGNORED, action.request.logId(), "because shut down");
      }
      return;
    }
    hunter = forRequest(action.getPicasso(), this, cache, stats, action);
    hunter.future = service.submit(hunter);
    hunterMap.put(action.getKey(), hunter);
    if (dismissFailed) {
      failedActions.remove(action.getTarget());
    }
    if (action.getPicasso().loggingEnabled) {
      log(OWNER_DISPATCHER, VERB_ENQUEUED, action.request.logId());
    }
  }
  void performCancel(Action action) {
    String key = action.getKey();
    BitmapHunter hunter = hunterMap.get(key);
    if (hunter != null) {
      hunter.detach(action);
      if (hunter.cancel()) {
        hunterMap.remove(key);
        if (action.getPicasso().loggingEnabled) {
          log(OWNER_DISPATCHER, VERB_CANCELED, action.getRequest().logId());
        }
      }
    }
    if (pausedTags.contains(action.getTag())) {
      pausedActions.remove(action.getTarget());
      if (action.getPicasso().loggingEnabled) {
        log(OWNER_DISPATCHER, VERB_CANCELED, action.getRequest().logId(),
            "because paused request got canceled");
      }
    }
    Action remove = failedActions.remove(action.getTarget());
    if (remove != null && remove.getPicasso().loggingEnabled) {
      log(OWNER_DISPATCHER, VERB_CANCELED, remove.getRequest().logId(), "from replaying");
    }
  }
  void performPauseTag(Object tag) {
    if (!pausedTags.add(tag)) {
      return;
    }
    for (Iterator<BitmapHunter> it = hunterMap.values().iterator(); it.hasNext();) {
      BitmapHunter hunter = it.next();
      boolean loggingEnabled = hunter.getPicasso().loggingEnabled;
      Action single = hunter.getAction();
      List<Action> joined = hunter.getActions();
      boolean hasMultiple = joined != null && !joined.isEmpty();
      if (single == null && !hasMultiple) {
        continue;
      }
      if (single != null && single.getTag().equals(tag)) {
        hunter.detach(single);
        pausedActions.put(single.getTarget(), single);
        if (loggingEnabled) {
          log(OWNER_DISPATCHER, VERB_PAUSED, single.request.logId(),
              "because tag '" + tag + "' was paused");
        }
      }
      if (hasMultiple) {
        for (int i = joined.size() - 1; i >= 0; i--) {
          Action action = joined.get(i);
          if (!action.getTag().equals(tag)) {
            continue;
          }
          hunter.detach(action);
          pausedActions.put(action.getTarget(), action);
          if (loggingEnabled) {
            log(OWNER_DISPATCHER, VERB_PAUSED, action.request.logId(),
                "because tag '" + tag + "' was paused");
          }
        }
      }
      if (hunter.cancel()) {
        it.remove();
        if (loggingEnabled) {
          log(OWNER_DISPATCHER, VERB_CANCELED, getLogIdsForHunter(hunter), "all actions paused");
        }
      }
    }
  }
  void performResumeTag(Object tag) {
    if (!pausedTags.remove(tag)) {
      return;
    }
    List<Action> batch = null;
    for (Iterator<Action> i = pausedActions.values().iterator(); i.hasNext();) {
      Action action = i.next();
      if (action.getTag().equals(tag)) {
        if (batch == null) {
          batch = new ArrayList<>();
        }
        batch.add(action);
        i.remove();
      }
    }
    if (batch != null) {
      mainThreadHandler.sendMessage(mainThreadHandler.obtainMessage(REQUEST_BATCH_RESUME, batch));
    }
  }
  @SuppressLint("MissingPermission")
  void performRetry(BitmapHunter hunter) {
    if (hunter.isCancelled()) return;
    if (service.isShutdown()) {
      performError(hunter);
      return;
    }
    NetworkInfo networkInfo = null;
    if (scansNetworkChanges) {
      ConnectivityManager connectivityManager =
          ContextCompat.getSystemService(context, ConnectivityManager.class);
      networkInfo = connectivityManager.getActiveNetworkInfo();
    }
    if (hunter.shouldRetry(airplaneMode, networkInfo)) {
      if (hunter.getPicasso().loggingEnabled) {
        log(OWNER_DISPATCHER, VERB_RETRYING, getLogIdsForHunter(hunter));
      }
      if (hunter.getException() instanceof NetworkRequestHandler.ContentLengthException) {
        hunter.data = hunter.data.newBuilder().networkPolicy(NetworkPolicy.NO_CACHE).build();
      }
      hunter.future = service.submit(hunter);
    } else {
      performError(hunter);
      if (scansNetworkChanges && hunter.supportsReplay()) {
        markForReplay(hunter);
      }
    }
  }
  void performComplete(BitmapHunter hunter) {
    if (shouldWriteToMemoryCache(hunter.data.memoryPolicy)) {
      RequestHandler.Result result = hunter.getResult();
      Bitmap bitmap = result.getBitmap();
      if (bitmap != null) {
        cache.set(hunter.getKey(), bitmap);
      }
    }
    hunterMap.remove(hunter.getKey());
    deliver(hunter);
  }
  void performError(BitmapHunter hunter) {
    hunterMap.remove(hunter.getKey());
    deliver(hunter);
  }
  void performAirplaneModeChange(boolean airplaneMode) {
    this.airplaneMode = airplaneMode;
  }
  void performNetworkStateChange(NetworkInfo info) {
    if (info != null && info.isConnected()) {
      flushFailedActions();
    }
  }
  private void flushFailedActions() {
    if (!failedActions.isEmpty()) {
      Iterator<Action> iterator = failedActions.values().iterator();
      while (iterator.hasNext()) {
        Action action = iterator.next();
        iterator.remove();
        if (action.getPicasso().loggingEnabled) {
          log(OWNER_DISPATCHER, VERB_REPLAYING, action.getRequest().logId());
        }
        performSubmit(action, false);
      }
    }
  }
  private void markForReplay(BitmapHunter hunter) {
    Action action = hunter.getAction();
    if (action != null) {
      markForReplay(action);
    }
    List<Action> joined = hunter.getActions();
    if (joined != null) {
      for (int i = 0, n = joined.size(); i < n; i++) {
        Action join = joined.get(i);
        markForReplay(join);
      }
    }
  }
  private void markForReplay(Action action) {
    Object target = action.getTarget();
    action.willReplay = true;
    failedActions.put(target, action);
  }
  private void deliver(BitmapHunter hunter) {
    if (hunter.isCancelled()) {
      return;
    }
    RequestHandler.Result result = hunter.getResult();
    if (result != null) {
      Bitmap bitmap = result.getBitmap();
      if (bitmap != null) {
        bitmap.prepareToDraw();
      }
    }
    mainThreadHandler.sendMessage(mainThreadHandler.obtainMessage(HUNTER_COMPLETE, hunter));
    logDelivery(hunter);
  }
  private void logDelivery(BitmapHunter bitmapHunter) {
    Picasso picasso = bitmapHunter.getPicasso();
    if (picasso.loggingEnabled) {
      log(OWNER_DISPATCHER, VERB_DELIVERED, Utils.getLogIdsForHunter(bitmapHunter));
    }
  }
  private static class DispatcherHandler extends Handler {
    private final Dispatcher dispatcher;
    DispatcherHandler(Looper looper, Dispatcher dispatcher) {
      super(looper);
      this.dispatcher = dispatcher;
    }
    @Override public void handleMessage(final Message msg) {
      switch (msg.what) {
        case REQUEST_SUBMIT: {
          Action action = (Action) msg.obj;
          dispatcher.performSubmit(action);
          break;
        }
        case REQUEST_CANCEL: {
          Action action = (Action) msg.obj;
          dispatcher.performCancel(action);
          break;
        }
        case TAG_PAUSE: {
          Object tag = msg.obj;
          dispatcher.performPauseTag(tag);
          break;
        }
        case TAG_RESUME: {
          Object tag = msg.obj;
          dispatcher.performResumeTag(tag);
          break;
        }
        case HUNTER_COMPLETE: {
          BitmapHunter hunter = (BitmapHunter) msg.obj;
          dispatcher.performComplete(hunter);
          break;
        }
        case HUNTER_RETRY: {
          BitmapHunter hunter = (BitmapHunter) msg.obj;
          dispatcher.performRetry(hunter);
          break;
        }
        case HUNTER_DECODE_FAILED: {
          BitmapHunter hunter = (BitmapHunter) msg.obj;
          dispatcher.performError(hunter);
          break;
        }
        case NETWORK_STATE_CHANGE: {
          NetworkInfo info = (NetworkInfo) msg.obj;
          dispatcher.performNetworkStateChange(info);
          break;
        }
        case AIRPLANE_MODE_CHANGE: {
          dispatcher.performAirplaneModeChange(msg.arg1 == AIRPLANE_MODE_ON);
          break;
        }
        default:
          Picasso.HANDLER.post(new Runnable() {
            @Override public void run() {
              throw new AssertionError("Unknown handler message received: " + msg.what);
            }
          });
      }
    }
  }
  static class DispatcherThread extends HandlerThread {
    DispatcherThread() {
      super(Utils.THREAD_PREFIX + DISPATCHER_THREAD_NAME, THREAD_PRIORITY_BACKGROUND);
    }
  }
  static class NetworkBroadcastReceiver extends BroadcastReceiver {
    static final String EXTRA_AIRPLANE_STATE = "state";
    private final Dispatcher dispatcher;
    NetworkBroadcastReceiver(Dispatcher dispatcher) {
      this.dispatcher = dispatcher;
    }
    void register() {
      IntentFilter filter = new IntentFilter();
      filter.addAction(ACTION_AIRPLANE_MODE_CHANGED);
      if (dispatcher.scansNetworkChanges) {
        filter.addAction(CONNECTIVITY_ACTION);
      }
      dispatcher.context.registerReceiver(this, filter);
    }
    void unregister() {
      dispatcher.context.unregisterReceiver(this);
    }
    @SuppressLint("MissingPermission")
    @Override public void onReceive(Context context, Intent intent) {
      if (intent == null) {
        return;
      }
      final String action = intent.getAction();
      if (ACTION_AIRPLANE_MODE_CHANGED.equals(action)) {
        if (!intent.hasExtra(EXTRA_AIRPLANE_STATE)) {
          return; 
        }
        dispatcher.dispatchAirplaneModeChange(intent.getBooleanExtra(EXTRA_AIRPLANE_STATE, false));
      } else if (CONNECTIVITY_ACTION.equals(action)) {
        ConnectivityManager connectivityManager =
            ContextCompat.getSystemService(context, ConnectivityManager.class);
        NetworkInfo networkInfo = null;
        try {
          networkInfo = connectivityManager.getActiveNetworkInfo();
        } catch (RuntimeException re) {
          Log.w(TAG, "System UI crashed, ignoring attempt to change network state.");
          return;
        }
        if (networkInfo == null) {
          Log.w(TAG, "No default network is currently active, ignoring attempt to change "
              + "network state.");
          return;
        }
        dispatcher.dispatchNetworkStateChange(networkInfo);
      }
    }
  }
}
