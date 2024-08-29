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

import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.OnLifecycleEvent;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.DrawableRes;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.ImageView;
import android.widget.RemoteViews;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import static com.squareup.picasso3.Dispatcher.HUNTER_COMPLETE;
import static com.squareup.picasso3.Dispatcher.REQUEST_BATCH_RESUME;
import static com.squareup.picasso3.MemoryPolicy.shouldReadFromMemoryCache;
import static com.squareup.picasso3.Picasso.LoadedFrom.MEMORY;
import static com.squareup.picasso3.Utils.OWNER_MAIN;
import static com.squareup.picasso3.Utils.VERB_COMPLETED;
import static com.squareup.picasso3.Utils.VERB_ERRORED;
import static com.squareup.picasso3.Utils.VERB_RESUMED;
import static com.squareup.picasso3.Utils.calculateDiskCacheSize;
import static com.squareup.picasso3.Utils.checkMain;
import static com.squareup.picasso3.Utils.checkNotNull;
import static com.squareup.picasso3.Utils.createDefaultCacheDir;
import static com.squareup.picasso3.Utils.log;

/**
 * Image downloading, transformation, and caching manager.
 * <p>
 * Use {@see PicassoProvider#get()} for a global singleton instance
 * or construct your own instance with {@link Builder}.
 */
@org.checkerframework.framework.qual.AnnotatedFor("org.checkerframework.checker.nullness.NullnessChecker")
public class Picasso implements LifecycleObserver {

    /**
     * Callbacks for Picasso events.
     */
    public interface Listener {

        /**
         * Invoked when an image has failed to load. This is useful for reporting image failures to a
         * remote analytics service, for example.
         */
        @org.checkerframework.dataflow.qual.SideEffectFree
        void onImageLoadFailed(@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull Picasso picasso, @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.Nullable Uri uri, @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull Exception exception);
    }

    /**
     * A transformer that is called immediately before every request is submitted. This can be used to
     * modify any information about a request.
     * <p>
     * For example, if you use a CDN you can change the hostname for the image based on the current
     * location of the user in order to get faster download speeds.
     */
    public interface RequestTransformer {

        /**
         * Transform a request before it is submitted to be processed.
         *
         * @return The original request or a new request to replace it. Must not be null.
         */
        @org.checkerframework.dataflow.qual.Pure
        @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.Nullable Request transformRequest(@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull Request request);
    }

    /**
     * The priority of a request.
     *
     * @see RequestCreator#priority(Priority)
     */
    public enum Priority {

        LOW, NORMAL, HIGH
    }

    static final @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull String TAG = "Picasso";

    static final @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull Handler HANDLER = new Handler(Looper.getMainLooper()) {

        public void handleMessage(Message msg) {
            switch(msg.what) {
                case HUNTER_COMPLETE:
                    {
                        BitmapHunter hunter = (BitmapHunter) msg.obj;
                        hunter.picasso.complete(hunter);
                        break;
                    }
                case REQUEST_BATCH_RESUME:
                    List<Action> batch = (List<Action>) msg.obj;
                    //noinspection ForLoopReplaceableByForEach
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

    private final @org.checkerframework.checker.initialization.qual.FBCBottom @org.checkerframework.checker.nullness.qual.MonotonicNonNull Listener listener;

    private final @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull List<RequestTransformer> requestTransformers;

    private final @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull List<RequestHandler> requestHandlers;

    final @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.MonotonicNonNull Context context;

    final @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.MonotonicNonNull Dispatcher dispatcher;

    private final okhttp3.@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.MonotonicNonNull Cache closeableCache;

    final @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull PlatformLruCache cache;

    final @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.MonotonicNonNull Stats stats;

    final @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull Map<Object, Action> targetToAction;

    final @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull Map<ImageView, DeferredRequestCreator> targetToDeferredRequestCreator;

    final Bitmap.@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.MonotonicNonNull Config defaultBitmapConfig;

     @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull boolean indicatorsEnabled;

    volatile  @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull boolean loggingEnabled;

     @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull boolean shutdown;

    @org.checkerframework.dataflow.qual.Impure
    Picasso(@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.Nullable Context context, @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.Nullable Dispatcher dispatcher, Call.@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull Factory callFactory, okhttp3.@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.Nullable Cache closeableCache, @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull PlatformLruCache cache, @org.checkerframework.checker.initialization.qual.FBCBottom @org.checkerframework.checker.nullness.qual.Nullable Listener listener, @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull List<RequestTransformer> requestTransformers, @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull List<RequestHandler> extraRequestHandlers, @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.Nullable Stats stats, Bitmap.@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.Nullable Config defaultBitmapConfig,  @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull boolean indicatorsEnabled,  @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull boolean loggingEnabled) {
        this.context = context;
        this.dispatcher = dispatcher;
        this.closeableCache = closeableCache;
        this.cache = cache;
        this.listener = listener;
        this.requestTransformers = Collections.unmodifiableList(new ArrayList<>(requestTransformers));
        this.defaultBitmapConfig = defaultBitmapConfig;
        // Adjust this as internal handlers are added or removed.
        int builtInHandlers = 8;
        int extraCount = extraRequestHandlers.size();
        List<RequestHandler> allRequestHandlers = new ArrayList<>(builtInHandlers + extraCount);
        // ResourceRequestHandler needs to be the first in the list to avoid
        // forcing other RequestHandlers to perform null checks on request.uri
        // to cover the (request.resourceId != 0) case.
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

    @org.checkerframework.dataflow.qual.Impure
    void cancelAll() {
        checkMain();
        List<Action> actions = new ArrayList<>(targetToAction.values());
        //noinspection ForLoopReplaceableByForEach
        for (int i = 0, n = actions.size(); i < n; i++) {
            Action action = actions.get(i);
            cancelExistingRequest(action.getTarget());
        }
        List<DeferredRequestCreator> deferredRequestCreators = new ArrayList<>(targetToDeferredRequestCreator.values());
        //noinspection ForLoopReplaceableByForEach
        for (int i = 0, n = deferredRequestCreators.size(); i < n; i++) {
            DeferredRequestCreator deferredRequestCreator = deferredRequestCreators.get(i);
            deferredRequestCreator.cancel();
        }
    }

    /**
     * Cancel any existing requests for the specified target {@link ImageView}.
     */
    @org.checkerframework.dataflow.qual.Impure
    public void cancelRequest(@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.Nullable ImageView view) {
        // checkMain() is called from cancelExistingRequest()
        checkNotNull(view, "view == null");
        cancelExistingRequest(view);
    }

    /**
     * Cancel any existing requests for the specified {@link BitmapTarget} instance.
     */
    @org.checkerframework.dataflow.qual.Impure
    public void cancelRequest(@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.Nullable BitmapTarget target) {
        // checkMain() is called from cancelExistingRequest()
        checkNotNull(target, "target == null");
        cancelExistingRequest(target);
    }

    /**
     * Cancel any existing requests for the specified {@link RemoteViews} target with the given {@code
     * viewId}.
     */
    @org.checkerframework.dataflow.qual.Impure
    public void cancelRequest(@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.Nullable RemoteViews remoteViews,  @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull int viewId) {
        // checkMain() is called from cancelExistingRequest()
        checkNotNull(remoteViews, "remoteViews == null");
        cancelExistingRequest(new RemoteViewsAction.RemoteViewsTarget(remoteViews, viewId));
    }

    /**
     * Cancel any existing requests with given tag. You can set a tag
     * on new requests with {@link RequestCreator#tag(Object)}.
     *
     * @see RequestCreator#tag(Object)
     */
    @org.checkerframework.dataflow.qual.Impure
    public void cancelTag(@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.Nullable Object tag) {
        checkMain();
        checkNotNull(tag, "tag == null");
        List<Action> actions = new ArrayList<>(targetToAction.values());
        //noinspection ForLoopReplaceableByForEach
        for (int i = 0, n = actions.size(); i < n; i++) {
            Action action = actions.get(i);
            if (tag.equals(action.getTag())) {
                cancelExistingRequest(action.getTarget());
            }
        }
        List<DeferredRequestCreator> deferredRequestCreators = new ArrayList<>(targetToDeferredRequestCreator.values());
        //noinspection ForLoopReplaceableByForEach
        for (int i = 0, n = deferredRequestCreators.size(); i < n; i++) {
            DeferredRequestCreator deferredRequestCreator = deferredRequestCreators.get(i);
            if (tag.equals(deferredRequestCreator.getTag())) {
                deferredRequestCreator.cancel();
            }
        }
    }

    @org.checkerframework.dataflow.qual.Impure
    void pauseAll() {
        checkMain();
        List<Action> actions = new ArrayList<>(targetToAction.values());
        //noinspection ForLoopReplaceableByForEach
        for (int i = 0, n = actions.size(); i < n; i++) {
            Action action = actions.get(i);
            dispatcher.dispatchPauseTag(action.getTag());
        }
        List<DeferredRequestCreator> deferredRequestCreators = new ArrayList<>(targetToDeferredRequestCreator.values());
        //noinspection ForLoopReplaceableByForEach
        for (int i = 0, n = deferredRequestCreators.size(); i < n; i++) {
            DeferredRequestCreator deferredRequestCreator = deferredRequestCreators.get(i);
            Object tag = deferredRequestCreator.getTag();
            if (tag != null) {
                dispatcher.dispatchPauseTag(tag);
            }
        }
    }

    /**
     * Pause existing requests with the given tag. Use {@link #resumeTag(Object)}
     * to resume requests with the given tag.
     *
     * @see #resumeTag(Object)
     * @see RequestCreator#tag(Object)
     */
    @org.checkerframework.dataflow.qual.Impure
    public void pauseTag(Object tag) {
        checkNotNull(tag, "tag == null");
        dispatcher.dispatchPauseTag(tag);
    }

    @org.checkerframework.dataflow.qual.Impure
    void resumeAll() {
        checkMain();
        List<Action> actions = new ArrayList<>(targetToAction.values());
        //noinspection ForLoopReplaceableByForEach
        for (int i = 0, n = actions.size(); i < n; i++) {
            Action action = actions.get(i);
            dispatcher.dispatchResumeTag(action.getTag());
        }
        List<DeferredRequestCreator> deferredRequestCreators = new ArrayList<>(targetToDeferredRequestCreator.values());
        //noinspection ForLoopReplaceableByForEach
        for (int i = 0, n = deferredRequestCreators.size(); i < n; i++) {
            DeferredRequestCreator deferredRequestCreator = deferredRequestCreators.get(i);
            Object tag = deferredRequestCreator.getTag();
            if (tag != null) {
                dispatcher.dispatchResumeTag(tag);
            }
        }
    }

    /**
     * Resume paused requests with the given tag. Use {@link #pauseTag(Object)}
     * to pause requests with the given tag.
     *
     * @see #pauseTag(Object)
     * @see RequestCreator#tag(Object)
     */
    @org.checkerframework.dataflow.qual.Impure
    public void resumeTag(Object tag) {
        checkNotNull(tag, "tag == null");
        dispatcher.dispatchResumeTag(tag);
    }

    /**
     * Start an image request using the specified URI.
     * <p>
     * Passing {@code null} as a {@code uri} will not trigger any request but will set a placeholder,
     * if one is specified.
     *
     * @see #load(File)
     * @see #load(String)
     * @see #load(int)
     */
    @org.checkerframework.dataflow.qual.Pure
    public @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull RequestCreator load(@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull Uri uri) {
        return new RequestCreator(this, uri, 0);
    }

    /**
     * Start an image request using the specified path. This is a convenience method for calling
     * {@link #load(Uri)}.
     * <p>
     * This path may be a remote URL, file resource (prefixed with {@code file:}), content resource
     * (prefixed with {@code content:}), or android resource (prefixed with {@code android.resource:}.
     * <p>
     * Passing {@code null} as a {@code path} will not trigger any request but will set a
     * placeholder, if one is specified.
     *
     * @throws IllegalArgumentException if {@code path} is empty or blank string.
     * @see #load(Uri)
     * @see #load(File)
     * @see #load(int)
     */
    @org.checkerframework.dataflow.qual.Impure
    public @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull RequestCreator load(@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull String path) {
        if (path == null) {
            return new RequestCreator(this, null, 0);
        }
        if (path.trim().length() == 0) {
            throw new IllegalArgumentException("Path must not be empty.");
        }
        return load(Uri.parse(path));
    }

    /**
     * Start an image request using the specified image file. This is a convenience method for
     * calling {@link #load(Uri)}.
     * <p>
     * Passing {@code null} as a {@code file} will not trigger any request but will set a
     * placeholder, if one is specified.
     * <p>
     * Equivalent to calling {@link #load(Uri) load(Uri.fromFile(file))}.
     *
     * @see #load(Uri)
     * @see #load(String)
     * @see #load(int)
     */
    @org.checkerframework.dataflow.qual.Impure
    public @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull RequestCreator load(File file) {
        if (file == null) {
            return new RequestCreator(this, null, 0);
        }
        return load(Uri.fromFile(file));
    }

    /**
     * Start an image request using the specified drawable resource ID.
     *
     * @see #load(Uri)
     * @see #load(String)
     * @see #load(File)
     */
    @org.checkerframework.dataflow.qual.Pure
    public @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull RequestCreator load( @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull int resourceId) {
        if (resourceId == 0) {
            throw new IllegalArgumentException("Resource ID must not be zero.");
        }
        return new RequestCreator(this, null, resourceId);
    }

    /**
     * Clear all the bitmaps from the memory cache.
     */
    @org.checkerframework.dataflow.qual.Impure
    public void evictAll() {
        cache.clear();
    }

    /**
     * Invalidate all memory cached images for the specified {@code uri}.
     *
     * @see #invalidate(String)
     * @see #invalidate(File)
     */
    @org.checkerframework.dataflow.qual.Impure
    public void invalidate(@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull Uri uri) {
        if (uri != null) {
            cache.clearKeyUri(uri.toString());
        }
    }

    /**
     * Invalidate all memory cached images for the specified {@code path}. You can also pass a
     * {@linkplain RequestCreator#stableKey stable key}.
     *
     * @see #invalidate(Uri)
     * @see #invalidate(File)
     */
    @org.checkerframework.dataflow.qual.Impure
    public void invalidate(@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull String path) {
        if (path != null) {
            invalidate(Uri.parse(path));
        }
    }

    /**
     * Invalidate all memory cached images for the specified {@code file}.
     *
     * @see #invalidate(Uri)
     * @see #invalidate(String)
     */
    @org.checkerframework.dataflow.qual.Impure
    public void invalidate(@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull File file) {
        checkNotNull(file, "file == null");
        invalidate(Uri.fromFile(file));
    }

    /**
     * Toggle whether to display debug indicators on images.
     */
    @org.checkerframework.dataflow.qual.Impure
    public void setIndicatorsEnabled( @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull boolean enabled) {
        indicatorsEnabled = enabled;
    }

    /**
     * {@code true} if debug indicators should are displayed on images.
     */
    @org.checkerframework.dataflow.qual.Pure
    public  @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull boolean getIndicatorsEnabled() {
        return indicatorsEnabled;
    }

    /**
     * Toggle whether debug logging is enabled.
     * <p>
     * <b>WARNING:</b> Enabling this will result in excessive object allocation. This should be only
     * be used for debugging Picasso behavior. Do NOT pass {@code BuildConfig.DEBUG}.
     */
    // Public API.
    @org.checkerframework.dataflow.qual.Impure
    public void setLoggingEnabled(boolean enabled) {
        loggingEnabled = enabled;
    }

    /**
     * {@code true} if debug logging is enabled.
     */
    @org.checkerframework.dataflow.qual.Pure
    public  @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull boolean isLoggingEnabled() {
        return loggingEnabled;
    }

    /**
     * Creates a {@link StatsSnapshot} of the current stats for this instance.
     * <p>
     * <b>NOTE:</b> The snapshot may not always be completely up-to-date if requests are still in
     * progress.
     */
    @org.checkerframework.dataflow.qual.Impure
    public @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull StatsSnapshot getSnapshot() {
        return stats.createSnapshot();
    }

    /**
     * Stops this instance from accepting further requests.
     */
    @org.checkerframework.dataflow.qual.Impure
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

    @org.checkerframework.dataflow.qual.Pure
    @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull List<RequestHandler> getRequestHandlers() {
        return requestHandlers;
    }

    @org.checkerframework.dataflow.qual.Impure
    @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull Request transformRequest(@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull Request request) {
        for (int i = 0, size = requestTransformers.size(); i < size; i++) {
            RequestTransformer transformer = requestTransformers.get(i);
            Request transformed = transformer.transformRequest(request);
            if (transformed == null) {
                throw new IllegalStateException("Request transformer " + transformer.getClass().getCanonicalName() + " returned null for " + request);
            }
            request = transformed;
        }
        return request;
    }

    @org.checkerframework.dataflow.qual.Impure
    void defer(@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull ImageView view, @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull DeferredRequestCreator request) {
        // If there is already a deferred request, cancel it.
        if (targetToDeferredRequestCreator.containsKey(view)) {
            cancelExistingRequest(view);
        }
        targetToDeferredRequestCreator.put(view, request);
    }

    @org.checkerframework.dataflow.qual.Impure
    void enqueueAndSubmit(@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull Action action) {
        Object target = action.getTarget();
        if (targetToAction.get(target) != action) {
            // This will also check we are on the main thread.
            cancelExistingRequest(target);
            targetToAction.put(target, action);
        }
        submit(action);
    }

    @org.checkerframework.dataflow.qual.Impure
    void submit(@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull Action action) {
        dispatcher.dispatchSubmit(action);
    }

    @org.checkerframework.dataflow.qual.Impure
    @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.Nullable Bitmap quickMemoryCacheCheck(@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull String key) {
        Bitmap cached = cache.get(key);
        if (cached != null) {
            stats.dispatchCacheHit();
        } else {
            stats.dispatchCacheMiss();
        }
        return cached;
    }

    @org.checkerframework.dataflow.qual.Impure
    void complete(@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull BitmapHunter hunter) {
        Action single = hunter.getAction();
        List<Action> joined = hunter.getActions();
        boolean hasMultiple = joined != null && !joined.isEmpty();
        boolean shouldDeliver = single != null || hasMultiple;
        if (!shouldDeliver) {
            return;
        }
        Uri uri = checkNotNull(hunter.getData().uri, "uri == null");
        Exception exception = hunter.getException();
        RequestHandler.Result result = hunter.getResult();
        if (single != null) {
            deliverAction(result, single, exception);
        }
        if (joined != null) {
            //noinspection ForLoopReplaceableByForEach
            for (int i = 0, n = joined.size(); i < n; i++) {
                Action join = joined.get(i);
                deliverAction(result, join, exception);
            }
        }
        if (listener != null && exception != null) {
            listener.onImageLoadFailed(this, uri, exception);
        }
    }

    @org.checkerframework.dataflow.qual.Impure
    void resumeAction(@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull Action action) {
        Bitmap bitmap = null;
        if (shouldReadFromMemoryCache(action.request.memoryPolicy)) {
            bitmap = quickMemoryCacheCheck(action.getKey());
        }
        if (bitmap != null) {
            // Resumed action is cached, complete immediately.
            deliverAction(new RequestHandler.Result(bitmap, MEMORY), action, null);
            if (loggingEnabled) {
                log(OWNER_MAIN, VERB_COMPLETED, action.request.logId(), "from " + MEMORY);
            }
        } else {
            // Re-submit the action to the executor.
            enqueueAndSubmit(action);
            if (loggingEnabled) {
                log(OWNER_MAIN, VERB_RESUMED, action.request.logId());
            }
        }
    }

    @org.checkerframework.dataflow.qual.Impure
    private void deliverAction(RequestHandler.@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.Nullable Result result, @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull Action action, @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.Nullable Exception e) {
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
            Exception exception = checkNotNull(e, "e == null");
            action.error(exception);
            if (loggingEnabled) {
                log(OWNER_MAIN, VERB_ERRORED, action.request.logId(), exception.getMessage());
            }
        }
    }

    @org.checkerframework.dataflow.qual.Impure
    void cancelExistingRequest(@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.Nullable Object target) {
        checkMain();
        Action action = targetToAction.remove(target);
        if (action != null) {
            action.cancel();
            dispatcher.dispatchCancel(action);
        }
        if (target instanceof ImageView) {
            ImageView targetImageView = (ImageView) target;
            DeferredRequestCreator deferredRequestCreator = targetToDeferredRequestCreator.remove(targetImageView);
            if (deferredRequestCreator != null) {
                deferredRequestCreator.cancel();
            }
        }
    }

    /**
     * Fluent API for creating {@link Picasso} instances.
     */
    // Public API.
    public static class Builder {

        private final @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull Context context;

        private Call.@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.Nullable Factory callFactory;

        private @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.Nullable ExecutorService service;

        private @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.MonotonicNonNull PlatformLruCache cache;

        private @org.checkerframework.checker.initialization.qual.FBCBottom @org.checkerframework.checker.nullness.qual.Nullable Listener listener;

        private final @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull List<RequestTransformer> requestTransformers = new ArrayList<>();

        private final @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull List<RequestHandler> requestHandlers = new ArrayList<>();

        private Bitmap.@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.MonotonicNonNull Config defaultBitmapConfig;

        private  @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull boolean indicatorsEnabled;

        private  @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull boolean loggingEnabled;

        /**
         * Start building a new {@link Picasso} instance.
         */
        @org.checkerframework.dataflow.qual.Impure
        public Builder(@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.Nullable Context context) {
            checkNotNull(context, "context == null");
            this.context = context.getApplicationContext();
        }

        /**
         * Specify the default {@link Bitmap.Config} used when decoding images. This can be overridden
         * on a per-request basis using {@link RequestCreator#config(Bitmap.Config) config(..)}.
         */
        @org.checkerframework.dataflow.qual.Impure
        public @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull Builder defaultBitmapConfig(Bitmap.Config bitmapConfig) {
            checkNotNull(bitmapConfig, "bitmapConfig == null");
            this.defaultBitmapConfig = bitmapConfig;
            return this;
        }

        /**
         * Specify the HTTP client to be used for network requests.
         * <p>
         * Note: Calling {@link #callFactory} overwrites this value.
         */
        @org.checkerframework.dataflow.qual.Impure
        public @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull Builder client(@org.checkerframework.checker.initialization.qual.FBCBottom @org.checkerframework.checker.nullness.qual.Nullable OkHttpClient client) {
            checkNotNull(client, "client == null");
            callFactory = client;
            return this;
        }

        /**
         * Specify the call factory to be used for network requests.
         * <p>
         * Note: Calling {@link #client} overwrites this value.
         */
        @org.checkerframework.dataflow.qual.Impure
        public @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull Builder callFactory(Call.@org.checkerframework.checker.initialization.qual.FBCBottom @org.checkerframework.checker.nullness.qual.Nullable Factory factory) {
            checkNotNull(factory, "factory == null");
            callFactory = factory;
            return this;
        }

        /**
         * Specify the executor service for loading images in the background.
         * <p>
         * Note: Calling {@link Picasso#shutdown() shutdown()} will not shutdown supplied executors.
         */
        @org.checkerframework.dataflow.qual.Impure
        public @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull Builder executor(@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.Nullable ExecutorService executorService) {
            checkNotNull(executorService, "executorService == null");
            if (this.service != null) {
                throw new IllegalStateException("Executor service already set.");
            }
            this.service = executorService;
            return this;
        }

        /**
         * Specify the memory cache size in bytes to use for the most recent images.
         * A size of 0 disables in-memory caching.
         */
        @org.checkerframework.dataflow.qual.Impure
        public @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull Builder withCacheSize( @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull int maxByteCount) {
            if (maxByteCount < 0) {
                throw new IllegalArgumentException("maxByteCount < 0: " + maxByteCount);
            }
            cache = new PlatformLruCache(maxByteCount);
            return this;
        }

        /**
         * Specify a listener for interesting events.
         */
        @org.checkerframework.dataflow.qual.Impure
        public @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull Builder listener(@org.checkerframework.checker.initialization.qual.FBCBottom @org.checkerframework.checker.nullness.qual.Nullable Listener listener) {
            checkNotNull(listener, "listener == null");
            if (this.listener != null) {
                throw new IllegalStateException("Listener already set.");
            }
            this.listener = listener;
            return this;
        }

        /**
         * Add a transformer that observes and potentially modify all incoming requests.
         */
        @org.checkerframework.dataflow.qual.Impure
        public @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull Builder addRequestTransformer(@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.Nullable RequestTransformer transformer) {
            checkNotNull(transformer, "transformer == null");
            if (requestTransformers.contains(transformer)) {
                throw new IllegalStateException("Transformer already set.");
            }
            requestTransformers.add(transformer);
            return this;
        }

        /**
         * Register a {@link RequestHandler}.
         */
        @org.checkerframework.dataflow.qual.Impure
        public @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull Builder addRequestHandler(@org.checkerframework.checker.initialization.qual.FBCBottom @org.checkerframework.checker.nullness.qual.Nullable RequestHandler requestHandler) {
            checkNotNull(requestHandler, "requestHandler == null");
            if (requestHandlers.contains(requestHandler)) {
                throw new IllegalStateException("RequestHandler already registered.");
            }
            requestHandlers.add(requestHandler);
            return this;
        }

        /**
         * Toggle whether to display debug indicators on images.
         */
        @org.checkerframework.dataflow.qual.Impure
        public @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull Builder indicatorsEnabled( @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull boolean enabled) {
            this.indicatorsEnabled = enabled;
            return this;
        }

        /**
         * Toggle whether debug logging is enabled.
         * <p>
         * <b>WARNING:</b> Enabling this will result in excessive object allocation. This should be only
         * be used for debugging purposes. Do NOT pass {@code BuildConfig.DEBUG}.
         */
        @org.checkerframework.dataflow.qual.Impure
        public @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull Builder loggingEnabled(boolean enabled) {
            this.loggingEnabled = enabled;
            return this;
        }

        /**
         * Create the {@link Picasso} instance.
         */
        @org.checkerframework.dataflow.qual.Impure
        public @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull Picasso build() {
            Context context = this.context;
            okhttp3.Cache unsharedCache = null;
            if (callFactory == null) {
                File cacheDir = createDefaultCacheDir(context);
                long maxSize = calculateDiskCacheSize(cacheDir);
                unsharedCache = new okhttp3.Cache(cacheDir, maxSize);
                callFactory = new OkHttpClient.Builder().cache(unsharedCache).build();
            }
            if (cache == null) {
                cache = new PlatformLruCache(Utils.calculateMemoryCacheSize(context));
            }
            if (service == null) {
                service = new PicassoExecutorService();
            }
            Stats stats = new Stats(cache);
            Dispatcher dispatcher = new Dispatcher(context, service, HANDLER, cache, stats);
            return new Picasso(context, dispatcher, callFactory, unsharedCache, cache, listener, requestTransformers, requestHandlers, stats, defaultBitmapConfig, indicatorsEnabled, loggingEnabled);
        }
    }

    /**
     * Describes where the image was loaded from.
     */
    public enum LoadedFrom {

        MEMORY(Color.GREEN), DISK(Color.BLUE), NETWORK(Color.RED);

        final  @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull int debugColor;

        @org.checkerframework.dataflow.qual.Impure
        LoadedFrom( @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull int debugColor) {
            this.debugColor = debugColor;
        }
    }
}
