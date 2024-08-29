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

import android.graphics.Bitmap;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.squareup.picasso3.RequestHandler.Result;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import okio.Buffer;
import static com.squareup.picasso3.MemoryPolicy.shouldReadFromMemoryCache;
import static com.squareup.picasso3.Picasso.LoadedFrom.MEMORY;
import static com.squareup.picasso3.Picasso.Priority;
import static com.squareup.picasso3.Picasso.Priority.LOW;
import static com.squareup.picasso3.Utils.OWNER_HUNTER;
import static com.squareup.picasso3.Utils.VERB_DECODED;
import static com.squareup.picasso3.Utils.VERB_EXECUTING;
import static com.squareup.picasso3.Utils.VERB_JOINED;
import static com.squareup.picasso3.Utils.VERB_REMOVED;
import static com.squareup.picasso3.Utils.VERB_TRANSFORMED;
import static com.squareup.picasso3.Utils.getLogIdsForHunter;
import static com.squareup.picasso3.Utils.log;

@org.checkerframework.framework.qual.AnnotatedFor("org.checkerframework.checker.nullness.NullnessChecker")
class BitmapHunter implements Runnable {

    private static final @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull ThreadLocal<StringBuilder> NAME_BUILDER = new ThreadLocal<StringBuilder>() {

        protected StringBuilder initialValue() {
            return new StringBuilder(Utils.THREAD_PREFIX);
        }
    };

    private static final @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull AtomicInteger SEQUENCE_GENERATOR = new AtomicInteger();

    private static final @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull RequestHandler ERRORING_HANDLER = new RequestHandler() {

        public boolean canHandleRequest(Request data) {
            return true;
        }

        public void load(Picasso picasso, Request request, Callback callback) {
            callback.onError(new IllegalStateException("Unrecognized type of request: " + request));
        }
    };

    final  @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull int sequence;

    final @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.MonotonicNonNull Picasso picasso;

    final @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.MonotonicNonNull Dispatcher dispatcher;

    final @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull PlatformLruCache cache;

    final @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.MonotonicNonNull Stats stats;

    final @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull String key;

    @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.MonotonicNonNull Request data;

    final @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.MonotonicNonNull RequestHandler requestHandler;

    @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.Nullable Action action;

    @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.MonotonicNonNull List<Action> actions;

    @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.MonotonicNonNull Result result;

    @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.MonotonicNonNull Future<?> future;

    @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.MonotonicNonNull Exception exception;

     @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull int retryCount;

    @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.Nullable Priority priority;

    @org.checkerframework.dataflow.qual.Impure
    BitmapHunter(@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.Nullable Picasso picasso, @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.Nullable Dispatcher dispatcher, @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull PlatformLruCache cache, @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.Nullable Stats stats, @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull Action action, @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.Nullable RequestHandler requestHandler) {
        this.sequence = SEQUENCE_GENERATOR.incrementAndGet();
        this.picasso = picasso;
        this.dispatcher = dispatcher;
        this.cache = cache;
        this.stats = stats;
        this.action = action;
        this.key = action.getKey();
        this.data = action.getRequest();
        this.priority = action.getPriority();
        this.requestHandler = requestHandler;
        this.retryCount = requestHandler.getRetryCount();
    }

    @org.checkerframework.dataflow.qual.Impure
    public void run(@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull BitmapHunter this) {
        try {
            updateThreadName(data);
            if (picasso.loggingEnabled) {
                log(OWNER_HUNTER, VERB_EXECUTING, getLogIdsForHunter(this));
            }
            result = hunt();
            if (result.getBitmap() == null && result.getDrawable() == null) {
                dispatcher.dispatchFailed(this);
            } else {
                dispatcher.dispatchComplete(this);
            }
        } catch (NetworkRequestHandler.ResponseException e) {
            if (!NetworkPolicy.isOfflineOnly(e.networkPolicy) || e.code != 504) {
                exception = e;
            }
            dispatcher.dispatchFailed(this);
        } catch (IOException e) {
            exception = e;
            dispatcher.dispatchRetry(this);
        } catch (OutOfMemoryError e) {
            Buffer buffer = new Buffer();
            try {
                stats.createSnapshot().dump(buffer);
            } catch (IOException ioe) {
                throw new AssertionError(ioe);
            }
            exception = new RuntimeException(buffer.readUtf8(), e);
            dispatcher.dispatchFailed(this);
        } catch (Exception e) {
            exception = e;
            dispatcher.dispatchFailed(this);
        } finally {
            Thread.currentThread().setName(Utils.THREAD_IDLE_NAME);
        }
    }

    @org.checkerframework.dataflow.qual.Impure
    @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull Result hunt() throws IOException {
        if (shouldReadFromMemoryCache(data.memoryPolicy)) {
            Bitmap bitmap = cache.get(key);
            if (bitmap != null) {
                stats.dispatchCacheHit();
                if (picasso.loggingEnabled) {
                    log(OWNER_HUNTER, VERB_DECODED, data.logId(), "from cache");
                }
                return new Result(bitmap, MEMORY);
            }
        }
        if (retryCount == 0) {
            data = data.newBuilder().networkPolicy(NetworkPolicy.OFFLINE).build();
        }
        final AtomicReference<Result> resultReference = new AtomicReference<>();
        final AtomicReference<Throwable> exceptionReference = new AtomicReference<>();
        final CountDownLatch latch = new CountDownLatch(1);
        try {
            requestHandler.load(picasso, data, new RequestHandler.Callback() {

                public void onSuccess(Result result) {
                    resultReference.set(result);
                    latch.countDown();
                }

                public void onError(Throwable t) {
                    exceptionReference.set(t);
                    latch.countDown();
                }
            });
            latch.await();
        } catch (InterruptedException ie) {
            InterruptedIOException interruptedIoException = new InterruptedIOException();
            interruptedIoException.initCause(ie);
            throw interruptedIoException;
        }
        Throwable throwable = exceptionReference.get();
        if (throwable != null) {
            if (throwable instanceof IOException) {
                throw (IOException) throwable;
            }
            if (throwable instanceof Error) {
                throw (Error) throwable;
            }
            if (throwable instanceof RuntimeException) {
                throw (RuntimeException) throwable;
            }
            throw new RuntimeException(throwable);
        }
        Result result = resultReference.get();
        if (result == null) {
            throw new AssertionError("Request handler neither returned a result nor an exception.");
        }
        Bitmap bitmap = result.getBitmap();
        if (bitmap != null) {
            if (picasso.loggingEnabled) {
                log(OWNER_HUNTER, VERB_DECODED, data.logId());
            }
            stats.dispatchBitmapDecoded(bitmap);
            List<Transformation> transformations = new ArrayList<>(data.transformations.size() + 1);
            if (data.needsMatrixTransform() || result.getExifRotation() != 0) {
                transformations.add(new MatrixTransformation(data));
            }
            transformations.addAll(data.transformations);
            result = applyTransformations(picasso, data, transformations, result);
            bitmap = result.getBitmap();
            if (bitmap != null) {
                stats.dispatchBitmapTransformed(bitmap);
            }
        }
        return result;
    }

    @org.checkerframework.dataflow.qual.Impure
    void attach(@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull Action action) {
        boolean loggingEnabled = picasso.loggingEnabled;
        Request request = action.request;
        if (this.action == null) {
            this.action = action;
            if (loggingEnabled) {
                if (actions == null || actions.isEmpty()) {
                    log(OWNER_HUNTER, VERB_JOINED, request.logId(), "to empty hunter");
                } else {
                    log(OWNER_HUNTER, VERB_JOINED, request.logId(), getLogIdsForHunter(this, "to "));
                }
            }
            return;
        }
        if (actions == null) {
            actions = new ArrayList<>(3);
        }
        actions.add(action);
        if (loggingEnabled) {
            log(OWNER_HUNTER, VERB_JOINED, request.logId(), getLogIdsForHunter(this, "to "));
        }
        Priority actionPriority = action.getPriority();
        if (actionPriority.ordinal() > priority.ordinal()) {
            priority = actionPriority;
        }
    }

    @org.checkerframework.dataflow.qual.Impure
    void detach(@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull Action action) {
        boolean detached = false;
        if (this.action == action) {
            this.action = null;
            detached = true;
        } else if (actions != null) {
            detached = actions.remove(action);
        }
        // The action being detached had the highest priority. Update this
        // hunter's priority with the remaining actions.
        if (detached && action.getPriority() == priority) {
            priority = computeNewPriority();
        }
        if (picasso.loggingEnabled) {
            log(OWNER_HUNTER, VERB_REMOVED, action.request.logId(), getLogIdsForHunter(this, "from "));
        }
    }

    @org.checkerframework.dataflow.qual.Impure
    private @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.Nullable Priority computeNewPriority() {
        Priority newPriority = LOW;
        final boolean hasMultiple = actions != null && !actions.isEmpty();
        final boolean hasAny = action != null || hasMultiple;
        // Hunter has no requests, low priority.
        if (!hasAny) {
            return newPriority;
        }
        if (action != null) {
            newPriority = action.getPriority();
        }
        if (actions != null) {
            //noinspection ForLoopReplaceableByForEach
            for (int i = 0, n = actions.size(); i < n; i++) {
                Priority actionPriority = actions.get(i).getPriority();
                if (actionPriority.ordinal() > newPriority.ordinal()) {
                    newPriority = actionPriority;
                }
            }
        }
        return newPriority;
    }

    @org.checkerframework.dataflow.qual.Impure
     @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull boolean cancel() {
        return action == null && (actions == null || actions.isEmpty()) && future != null && future.cancel(false);
    }

    @org.checkerframework.dataflow.qual.Pure
     @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull boolean isCancelled() {
        return future != null && future.isCancelled();
    }

    @org.checkerframework.dataflow.qual.Impure
     @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull boolean shouldRetry( @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull boolean airplaneMode, @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.Nullable NetworkInfo info) {
        boolean hasRetries = retryCount > 0;
        if (!hasRetries) {
            return false;
        }
        retryCount--;
        return requestHandler.shouldRetry(airplaneMode, info);
    }

    @org.checkerframework.dataflow.qual.Pure
     @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull boolean supportsReplay() {
        return requestHandler.supportsReplay();
    }

    @org.checkerframework.dataflow.qual.Pure
    @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.Nullable Result getResult() {
        return result;
    }

    @org.checkerframework.dataflow.qual.Pure
    @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull String getKey() {
        return key;
    }

    @org.checkerframework.dataflow.qual.Pure
    @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.Nullable Request getData() {
        return data;
    }

    @org.checkerframework.dataflow.qual.Pure
    @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.Nullable Action getAction() {
        return action;
    }

    @org.checkerframework.dataflow.qual.Pure
    @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.Nullable Picasso getPicasso() {
        return picasso;
    }

    @org.checkerframework.dataflow.qual.Pure
    @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.Nullable List<Action> getActions() {
        return actions;
    }

    @org.checkerframework.dataflow.qual.Pure
    @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.Nullable Exception getException() {
        return exception;
    }

    @org.checkerframework.dataflow.qual.Pure
    @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.Nullable Priority getPriority() {
        return priority;
    }

    @org.checkerframework.dataflow.qual.Impure
    static void updateThreadName(@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.Nullable Request data) {
        String name = data.getName();
        StringBuilder builder = NAME_BUILDER.get();
        builder.ensureCapacity(Utils.THREAD_PREFIX.length() + name.length());
        builder.replace(Utils.THREAD_PREFIX.length(), builder.length(), name);
        Thread.currentThread().setName(builder.toString());
    }

    @org.checkerframework.dataflow.qual.Impure
    static @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull BitmapHunter forRequest(@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.Nullable Picasso picasso, @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.Nullable Dispatcher dispatcher, @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull PlatformLruCache cache, @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.Nullable Stats stats, @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull Action action) {
        Request request = action.getRequest();
        List<RequestHandler> requestHandlers = picasso.getRequestHandlers();
        // Index-based loop to avoid allocating an iterator.
        //noinspection ForLoopReplaceableByForEach
        for (int i = 0, count = requestHandlers.size(); i < count; i++) {
            RequestHandler requestHandler = requestHandlers.get(i);
            if (requestHandler.canHandleRequest(request)) {
                return new BitmapHunter(picasso, dispatcher, cache, stats, action, requestHandler);
            }
        }
        return new BitmapHunter(picasso, dispatcher, cache, stats, action, ERRORING_HANDLER);
    }

    @org.checkerframework.dataflow.qual.Impure
    static @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.Nullable Result applyTransformations(@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.Nullable Picasso picasso, @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull Request data, @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull List<Transformation> transformations, @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull Result result) {
        for (int i = 0, count = transformations.size(); i < count; i++) {
            final Transformation transformation = transformations.get(i);
            Result newResult;
            try {
                newResult = transformation.transform(result);
                if (picasso.loggingEnabled) {
                    log(OWNER_HUNTER, VERB_TRANSFORMED, data.logId(), "from transformations");
                }
            } catch (final RuntimeException e) {
                Picasso.HANDLER.post(new Runnable() {

                    public void run() {
                        throw new RuntimeException("Transformation " + transformation.key() + " crashed with exception.", e);
                    }
                });
                return null;
            }
            if (newResult == null) {
                final StringBuilder builder = //
                new StringBuilder().append("Transformation ").append(transformation.key()).append(" returned null after ").append(i).append(" previous transformation(s).\n\nTransformation list:\n");
                for (Transformation t : transformations) {
                    builder.append(t.key()).append('\n');
                }
                Picasso.HANDLER.post(new Runnable() {

                    public void run() {
                        throw new NullPointerException(builder.toString());
                    }
                });
                return null;
            }
            Bitmap bitmap = result.getBitmap();
            if (bitmap == null)
                return result;
            if (newResult == result && bitmap.isRecycled()) {
                Picasso.HANDLER.post(new Runnable() {

                    public void run() {
                        throw new IllegalStateException("Transformation " + transformation.key() + " returned input Bitmap but recycled it.");
                    }
                });
                return null;
            }
            // If the transformation returned a new bitmap ensure they recycled the original.
            if (newResult != result && newResult.getBitmap() != bitmap && !bitmap.isRecycled()) {
                Picasso.HANDLER.post(new Runnable() {

                    public void run() {
                        throw new IllegalStateException("Transformation " + transformation.key() + " mutated input Bitmap but failed to recycle the original.");
                    }
                });
                return null;
            }
            result = newResult;
        }
        return result;
    }
}
