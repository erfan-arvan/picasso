/*
 * Copyright (C) 2013 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.picasso3;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import static android.content.Intent.ACTION_AIRPLANE_MODE_CHANGED;
import static android.net.ConnectivityManager.CONNECTIVITY_ACTION;
import static android.os.Process.THREAD_PRIORITY_BACKGROUND;
import static com.squareup.picasso3.BitmapHunter.forRequest;
import static com.squareup.picasso3.MemoryPolicy.shouldWriteToMemoryCache;
import static com.squareup.picasso3.Picasso.TAG;
import static com.squareup.picasso3.Utils.OWNER_DISPATCHER;
import static com.squareup.picasso3.Utils.VERB_CANCELED;
import static com.squareup.picasso3.Utils.VERB_DELIVERED;
import static com.squareup.picasso3.Utils.VERB_ENQUEUED;
import static com.squareup.picasso3.Utils.VERB_IGNORED;
import static com.squareup.picasso3.Utils.VERB_PAUSED;
import static com.squareup.picasso3.Utils.VERB_REPLAYING;
import static com.squareup.picasso3.Utils.VERB_RETRYING;
import static com.squareup.picasso3.Utils.getLogIdsForHunter;
import static com.squareup.picasso3.Utils.hasPermission;
import static com.squareup.picasso3.Utils.log;

@org.checkerframework.framework.qual.AnnotatedFor("org.checkerframework.checker.nullness.NullnessChecker")
class Dispatcher {

    private static final  @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull int RETRY_DELAY = 500;

    private static final  @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull int AIRPLANE_MODE_ON = 1;

    private static final  @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull int AIRPLANE_MODE_OFF = 0;

    static final  @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull int REQUEST_SUBMIT = 1;

    static final  @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull int REQUEST_CANCEL = 2;

    static final  @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull int HUNTER_COMPLETE = 4;

    static final  @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull int HUNTER_RETRY = 5;

    static final  @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull int HUNTER_DECODE_FAILED = 6;

    static final  @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull int NETWORK_STATE_CHANGE = 9;

    static final  @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull int AIRPLANE_MODE_CHANGE = 10;

    static final  @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull int TAG_PAUSE = 11;

    static final  @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull int TAG_RESUME = 12;

    static final  @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull int REQUEST_BATCH_RESUME = 13;

    private static final @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull String DISPATCHER_THREAD_NAME = "Dispatcher";

    final @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull DispatcherThread dispatcherThread;

    final @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull Context context;

    final @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.MonotonicNonNull ExecutorService service;

    final @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull Map<String, BitmapHunter> hunterMap;

    final @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull Map<Object, Action> failedActions;

    final @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull Map<Object, Action> pausedActions;

    final @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull Set<Object> pausedTags;

    final @org.checkerframework.checker.initialization.qual.UnderInitialization(com.squareup.picasso3.Dispatcher.DispatcherHandler.class) @org.checkerframework.checker.nullness.qual.NonNull Handler handler;

    final @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull Handler mainThreadHandler;

    final @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull PlatformLruCache cache;

    final @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.MonotonicNonNull Stats stats;

    final @org.checkerframework.checker.initialization.qual.UnderInitialization(com.squareup.picasso3.Dispatcher.NetworkBroadcastReceiver.class) @org.checkerframework.checker.nullness.qual.NonNull NetworkBroadcastReceiver receiver;

    final  @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull boolean scansNetworkChanges;

     @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull boolean airplaneMode;

    @org.checkerframework.dataflow.qual.Impure
    Dispatcher(@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull Context context, @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.Nullable ExecutorService service, @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull Handler mainThreadHandler, @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull PlatformLruCache cache, @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.Nullable Stats stats) {
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

    @org.checkerframework.dataflow.qual.Impure
    void shutdown() {
        // Shutdown the thread pool only if it is the one created by Picasso.
        if (service instanceof PicassoExecutorService) {
            service.shutdown();
        }
        dispatcherThread.quit();
        // Unregister network broadcast receiver on the main thread.
        Picasso.HANDLER.post(new Runnable() {

            public void run() {
                receiver.unregister();
            }
        });
    }

    @org.checkerframework.dataflow.qual.Impure
    void dispatchSubmit(@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull Action action) {
        handler.sendMessage(handler.obtainMessage(REQUEST_SUBMIT, action));
    }

    @org.checkerframework.dataflow.qual.Impure
    void dispatchCancel(@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull Action action) {
        handler.sendMessage(handler.obtainMessage(REQUEST_CANCEL, action));
    }

    @org.checkerframework.dataflow.qual.Impure
    void dispatchPauseTag(@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.Nullable Object tag) {
        handler.sendMessage(handler.obtainMessage(TAG_PAUSE, tag));
    }

    @org.checkerframework.dataflow.qual.Impure
    void dispatchResumeTag(@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.Nullable Object tag) {
        handler.sendMessage(handler.obtainMessage(TAG_RESUME, tag));
    }

    @org.checkerframework.dataflow.qual.Impure
    void dispatchComplete(@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull BitmapHunter hunter) {
        handler.sendMessage(handler.obtainMessage(HUNTER_COMPLETE, hunter));
    }

    @org.checkerframework.dataflow.qual.Impure
    void dispatchRetry(@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull BitmapHunter hunter) {
        handler.sendMessageDelayed(handler.obtainMessage(HUNTER_RETRY, hunter), RETRY_DELAY);
    }

    @org.checkerframework.dataflow.qual.Impure
    void dispatchFailed(@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull BitmapHunter hunter) {
        handler.sendMessage(handler.obtainMessage(HUNTER_DECODE_FAILED, hunter));
    }

    @org.checkerframework.dataflow.qual.Impure
    void dispatchNetworkStateChange(@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull NetworkInfo info) {
        handler.sendMessage(handler.obtainMessage(NETWORK_STATE_CHANGE, info));
    }

    @org.checkerframework.dataflow.qual.Impure
    void dispatchAirplaneModeChange( @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull boolean airplaneMode) {
        handler.sendMessage(handler.obtainMessage(AIRPLANE_MODE_CHANGE, airplaneMode ? AIRPLANE_MODE_ON : AIRPLANE_MODE_OFF, 0));
    }

    @org.checkerframework.dataflow.qual.Impure
    void performSubmit(@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull Action action) {
        performSubmit(action, true);
    }

    @org.checkerframework.dataflow.qual.Impure
    void performSubmit(@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull Action action,  @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull boolean dismissFailed) {
        if (pausedTags.contains(action.getTag())) {
            pausedActions.put(action.getTarget(), action);
            if (action.getPicasso().loggingEnabled) {
                log(OWNER_DISPATCHER, VERB_PAUSED, action.request.logId(), "because tag '" + action.getTag() + "' is paused");
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

    @org.checkerframework.dataflow.qual.Impure
    void performCancel(@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull Action action) {
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
                log(OWNER_DISPATCHER, VERB_CANCELED, action.getRequest().logId(), "because paused request got canceled");
            }
        }
        Action remove = failedActions.remove(action.getTarget());
        if (remove != null && remove.getPicasso().loggingEnabled) {
            log(OWNER_DISPATCHER, VERB_CANCELED, remove.getRequest().logId(), "from replaying");
        }
    }

    @org.checkerframework.dataflow.qual.Impure
    void performPauseTag(@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull Object tag) {
        // Trying to pause a tag that is already paused.
        if (!pausedTags.add(tag)) {
            return;
        }
        // Go through all active hunters and detach/pause the requests
        // that have the paused tag.
        for (Iterator<BitmapHunter> it = hunterMap.values().iterator(); it.hasNext(); ) {
            BitmapHunter hunter = it.next();
            boolean loggingEnabled = hunter.getPicasso().loggingEnabled;
            Action single = hunter.getAction();
            List<Action> joined = hunter.getActions();
            boolean hasMultiple = joined != null && !joined.isEmpty();
            // Hunter has no requests, bail early.
            if (single == null && !hasMultiple) {
                continue;
            }
            if (single != null && single.getTag().equals(tag)) {
                hunter.detach(single);
                pausedActions.put(single.getTarget(), single);
                if (loggingEnabled) {
                    log(OWNER_DISPATCHER, VERB_PAUSED, single.request.logId(), "because tag '" + tag + "' was paused");
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
                        log(OWNER_DISPATCHER, VERB_PAUSED, action.request.logId(), "because tag '" + tag + "' was paused");
                    }
                }
            }
            // Check if the hunter can be cancelled in case all its requests
            // had the tag being paused here.
            if (hunter.cancel()) {
                it.remove();
                if (loggingEnabled) {
                    log(OWNER_DISPATCHER, VERB_CANCELED, getLogIdsForHunter(hunter), "all actions paused");
                }
            }
        }
    }

    @org.checkerframework.dataflow.qual.Impure
    void performResumeTag(@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull Object tag) {
        // Trying to resume a tag that is not paused.
        if (!pausedTags.remove(tag)) {
            return;
        }
        List<Action> batch = null;
        for (Iterator<Action> i = pausedActions.values().iterator(); i.hasNext(); ) {
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

    @org.checkerframework.dataflow.qual.Impure
    void performRetry(@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull BitmapHunter hunter) {
        if (hunter.isCancelled())
            return;
        if (service.isShutdown()) {
            performError(hunter);
            return;
        }
        NetworkInfo networkInfo = null;
        if (scansNetworkChanges) {
            ConnectivityManager connectivityManager = ContextCompat.getSystemService(context, ConnectivityManager.class);
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
            // Mark for replay only if we observe network info changes and support replay.
            if (scansNetworkChanges && hunter.supportsReplay()) {
                markForReplay(hunter);
            }
        }
    }

    @org.checkerframework.dataflow.qual.Impure
    void performComplete(@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull BitmapHunter hunter) {
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

    @org.checkerframework.dataflow.qual.Impure
    void performError(@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull BitmapHunter hunter) {
        hunterMap.remove(hunter.getKey());
        deliver(hunter);
    }

    @org.checkerframework.dataflow.qual.Impure
    void performAirplaneModeChange( @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull boolean airplaneMode) {
        this.airplaneMode = airplaneMode;
    }

    @org.checkerframework.dataflow.qual.Impure
    void performNetworkStateChange(@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.Nullable NetworkInfo info) {
        // Intentionally check only if isConnected() here before we flush out failed actions.
        if (info != null && info.isConnected()) {
            flushFailedActions();
        }
    }

    @org.checkerframework.dataflow.qual.Impure
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

    @org.checkerframework.dataflow.qual.Impure
    private void markForReplay(@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull BitmapHunter hunter) {
        Action action = hunter.getAction();
        if (action != null) {
            markForReplay(action);
        }
        List<Action> joined = hunter.getActions();
        if (joined != null) {
            //noinspection ForLoopReplaceableByForEach
            for (int i = 0, n = joined.size(); i < n; i++) {
                Action join = joined.get(i);
                markForReplay(join);
            }
        }
    }

    @org.checkerframework.dataflow.qual.Impure
    private void markForReplay(@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull Action action) {
        Object target = action.getTarget();
        action.willReplay = true;
        failedActions.put(target, action);
    }

    @org.checkerframework.dataflow.qual.Impure
    private void deliver(@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull BitmapHunter hunter) {
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

    @org.checkerframework.dataflow.qual.Impure
    private void logDelivery(@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull BitmapHunter bitmapHunter) {
        Picasso picasso = bitmapHunter.getPicasso();
        if (picasso.loggingEnabled) {
            log(OWNER_DISPATCHER, VERB_DELIVERED, Utils.getLogIdsForHunter(bitmapHunter));
        }
    }

    private static class DispatcherHandler extends Handler {

        private final @org.checkerframework.checker.initialization.qual.UnderInitialization(java.lang.Object.class) @org.checkerframework.checker.nullness.qual.NonNull Dispatcher dispatcher;

        @org.checkerframework.dataflow.qual.Impure
        DispatcherHandler(@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull Looper looper, @org.checkerframework.checker.initialization.qual.UnderInitialization(java.lang.Object.class) @org.checkerframework.checker.nullness.qual.NonNull Dispatcher dispatcher) {
            super(looper);
            this.dispatcher = dispatcher;
        }

        @org.checkerframework.dataflow.qual.Impure
        public void handleMessage(@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull DispatcherHandler this, final @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull Message msg) {
            switch(msg.what) {
                case REQUEST_SUBMIT:
                    {
                        Action action = (Action) msg.obj;
                        dispatcher.performSubmit(action);
                        break;
                    }
                case REQUEST_CANCEL:
                    {
                        Action action = (Action) msg.obj;
                        dispatcher.performCancel(action);
                        break;
                    }
                case TAG_PAUSE:
                    {
                        Object tag = msg.obj;
                        dispatcher.performPauseTag(tag);
                        break;
                    }
                case TAG_RESUME:
                    {
                        Object tag = msg.obj;
                        dispatcher.performResumeTag(tag);
                        break;
                    }
                case HUNTER_COMPLETE:
                    {
                        BitmapHunter hunter = (BitmapHunter) msg.obj;
                        dispatcher.performComplete(hunter);
                        break;
                    }
                case HUNTER_RETRY:
                    {
                        BitmapHunter hunter = (BitmapHunter) msg.obj;
                        dispatcher.performRetry(hunter);
                        break;
                    }
                case HUNTER_DECODE_FAILED:
                    {
                        BitmapHunter hunter = (BitmapHunter) msg.obj;
                        dispatcher.performError(hunter);
                        break;
                    }
                case NETWORK_STATE_CHANGE:
                    {
                        NetworkInfo info = (NetworkInfo) msg.obj;
                        dispatcher.performNetworkStateChange(info);
                        break;
                    }
                case AIRPLANE_MODE_CHANGE:
                    {
                        dispatcher.performAirplaneModeChange(msg.arg1 == AIRPLANE_MODE_ON);
                        break;
                    }
                default:
                    Picasso.HANDLER.post(new Runnable() {

                        public void run() {
                            throw new AssertionError("Unknown handler message received: " + msg.what);
                        }
                    });
            }
        }
    }

    static class DispatcherThread extends HandlerThread {

        @org.checkerframework.dataflow.qual.Impure
        DispatcherThread() {
            super(Utils.THREAD_PREFIX + DISPATCHER_THREAD_NAME, THREAD_PRIORITY_BACKGROUND);
        }
    }

    static class NetworkBroadcastReceiver extends BroadcastReceiver {

        static final @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull String EXTRA_AIRPLANE_STATE = "state";

        private final @org.checkerframework.checker.initialization.qual.UnknownInitialization(java.lang.Object.class) @org.checkerframework.checker.nullness.qual.NonNull Dispatcher dispatcher;

        @org.checkerframework.dataflow.qual.Impure
        NetworkBroadcastReceiver(@org.checkerframework.checker.initialization.qual.UnknownInitialization(java.lang.Object.class) @org.checkerframework.checker.nullness.qual.NonNull Dispatcher dispatcher) {
            this.dispatcher = dispatcher;
        }

        @org.checkerframework.dataflow.qual.Impure
        void register() {
            IntentFilter filter = new IntentFilter();
            filter.addAction(ACTION_AIRPLANE_MODE_CHANGED);
            if (dispatcher.scansNetworkChanges) {
                filter.addAction(CONNECTIVITY_ACTION);
            }
            dispatcher.context.registerReceiver(this, filter);
        }

        @org.checkerframework.dataflow.qual.Impure
        void unregister() {
            dispatcher.context.unregisterReceiver(this);
        }

        @org.checkerframework.dataflow.qual.Impure
        public void onReceive(@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull NetworkBroadcastReceiver this, @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.Nullable Context context, @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.Nullable Intent intent) {
            // On some versions of Android this may be called with a null Intent,
            // also without extras (getExtras() == null), in such case we use defaults.
            if (intent == null) {
                return;
            }
            final String action = intent.getAction();
            if (ACTION_AIRPLANE_MODE_CHANGED.equals(action)) {
                if (!intent.hasExtra(EXTRA_AIRPLANE_STATE)) {
                    // No airplane state, ignore it. Should we query Utils.isAirplaneModeOn?
                    return;
                }
                dispatcher.dispatchAirplaneModeChange(intent.getBooleanExtra(EXTRA_AIRPLANE_STATE, false));
            } else if (CONNECTIVITY_ACTION.equals(action)) {
                ConnectivityManager connectivityManager = ContextCompat.getSystemService(context, ConnectivityManager.class);
                NetworkInfo networkInfo = null;
                try {
                    networkInfo = connectivityManager.getActiveNetworkInfo();
                } catch (RuntimeException re) {
                    Log.w(TAG, "System UI crashed, ignoring attempt to change network state.");
                    return;
                }
                if (networkInfo == null) {
                    Log.w(TAG, "No default network is currently active, ignoring attempt to change network state.");
                    return;
                }
                dispatcher.dispatchNetworkStateChange(networkInfo);
            }
        }
    }
}
