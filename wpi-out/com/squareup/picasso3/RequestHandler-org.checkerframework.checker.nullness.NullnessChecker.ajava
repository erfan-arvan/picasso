/*
 * Copyright (C) 2014 Square, Inc.
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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.graphics.drawable.Drawable;
import android.net.NetworkInfo;
import android.os.Build;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.TypedValue;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import okio.BufferedSource;
import okio.Okio;
import okio.Source;
import static com.squareup.picasso3.Utils.checkNotNull;

/**
 * {@code RequestHandler} allows you to extend Picasso to load images in ways that are not
 * supported by default in the library.
 * <p>
 * <h2>Usage</h2>
 * {@code RequestHandler} must be subclassed to be used. You will have to override two methods
 * ({@link #canHandleRequest(Request)} and {@link #load(Picasso, Request, Callback)}) with
 * your custom logic to load images.
 * <p>
 * You should then register your {@link RequestHandler} using
 * {@link Picasso.Builder#addRequestHandler(RequestHandler)}
 * <p>
 * <b>Note:</b> This is a beta feature. The API is subject to change in a backwards incompatible
 * way at any time.
 *
 * @see Picasso.Builder#addRequestHandler(RequestHandler)
 */
@org.checkerframework.framework.qual.AnnotatedFor("org.checkerframework.checker.nullness.NullnessChecker")
public abstract class RequestHandler {

    /**
     * {@link Result} represents the result of a {@link #load(Picasso, Request, Callback)} call
     * in a {@link RequestHandler}.
     *
     * @see RequestHandler
     * @see #load(Picasso, Request, Callback)
     */
    public static final class Result {

        private final Picasso.@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull LoadedFrom loadedFrom;

        private final @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.MonotonicNonNull Bitmap bitmap;

        private final @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.MonotonicNonNull Drawable drawable;

        private final  @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull int exifRotation;

        @org.checkerframework.dataflow.qual.SideEffectFree
        public Result(@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.Nullable Bitmap bitmap, Picasso.@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull LoadedFrom loadedFrom) {
            this(checkNotNull(bitmap, "bitmap == null"), null, loadedFrom, 0);
        }

        @org.checkerframework.dataflow.qual.SideEffectFree
        public Result(@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.Nullable Bitmap bitmap, Picasso.@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull LoadedFrom loadedFrom,  @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull int exifRotation) {
            this(checkNotNull(bitmap, "bitmap == null"), null, loadedFrom, exifRotation);
        }

        @org.checkerframework.dataflow.qual.SideEffectFree
        public Result(@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull Drawable drawable, Picasso.@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull LoadedFrom loadedFrom) {
            this(null, checkNotNull(drawable, "drawable == null"), loadedFrom, 0);
        }

        @org.checkerframework.dataflow.qual.SideEffectFree
        private Result(@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.Nullable Bitmap bitmap, @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.Nullable Drawable drawable, Picasso.@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull LoadedFrom loadedFrom,  @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull int exifRotation) {
            this.bitmap = bitmap;
            this.drawable = drawable;
            this.loadedFrom = checkNotNull(loadedFrom, "loadedFrom == null");
            this.exifRotation = exifRotation;
        }

        /**
         * The loaded {@link Bitmap}.
         * Mutually exclusive with {@link #getDrawable()}.
         */
        @org.checkerframework.dataflow.qual.Pure
        public @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.Nullable Bitmap getBitmap() {
            return bitmap;
        }

        /**
         * The loaded {@link Drawable}.
         * Mutually exclusive with {@link #getBitmap()}.
         */
        @org.checkerframework.dataflow.qual.Pure
        public @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.Nullable Drawable getDrawable() {
            return drawable;
        }

        /**
         * Returns the resulting {@link Picasso.LoadedFrom} generated from a
         * {@link #load(Picasso, Request, Callback)} call.
         */
        @org.checkerframework.dataflow.qual.Pure
        public Picasso.@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull LoadedFrom getLoadedFrom() {
            return loadedFrom;
        }

        /**
         * Returns the resulting EXIF rotation generated from a
         * {@link #load(Picasso, Request, Callback)} call.
         */
        @org.checkerframework.dataflow.qual.Pure
        public  @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull int getExifRotation() {
            return exifRotation;
        }
    }

    public interface Callback {

        @org.checkerframework.dataflow.qual.Impure
        void onSuccess(@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull Result result);

        @org.checkerframework.dataflow.qual.Impure
        void onError(@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull Throwable t);
    }

    /**
     * Whether or not this {@link RequestHandler} can handle a request with the given {@link Request}.
     */
    @org.checkerframework.dataflow.qual.Impure
    public abstract  @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull boolean canHandleRequest(@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.Nullable Request data);

    /**
     * Loads an image for the given {@link Request}.
     * @param request the data from which the image should be resolved.
     */
    @org.checkerframework.dataflow.qual.Impure
    public abstract void load(@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.Nullable Picasso picasso, @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.Nullable Request request, @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull Callback callback) throws IOException;

    @org.checkerframework.dataflow.qual.Pure
     @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull int getRetryCount() {
        return 0;
    }

    @org.checkerframework.dataflow.qual.Impure
     @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull boolean shouldRetry( @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull boolean airplaneMode, @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.Nullable NetworkInfo info) {
        return false;
    }

    @org.checkerframework.dataflow.qual.Pure
     @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull boolean supportsReplay() {
        return false;
    }

    /**
     * Lazily create {@link BitmapFactory.Options} based in given
     * {@link Request}, only instantiating them if needed.
     */
    @org.checkerframework.dataflow.qual.Impure
    static BitmapFactory.@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.Nullable Options createBitmapOptions(@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.Nullable Request data) {
        final boolean justBounds = data.hasSize();
        BitmapFactory.Options options = null;
        if (justBounds || data.config != null || data.purgeable) {
            options = new BitmapFactory.Options();
            options.inJustDecodeBounds = justBounds;
            options.inInputShareable = data.purgeable;
            options.inPurgeable = data.purgeable;
            if (data.config != null) {
                options.inPreferredConfig = data.config;
            }
        }
        return options;
    }

    @org.checkerframework.dataflow.qual.Pure
    static  @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull boolean requiresInSampleSize(BitmapFactory.@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.Nullable Options options) {
        return options != null && options.inJustDecodeBounds;
    }

    @org.checkerframework.dataflow.qual.Impure
    static void calculateInSampleSize( @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull int reqWidth,  @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull int reqHeight, BitmapFactory.@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.Nullable Options options, @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull Request request) {
        calculateInSampleSize(reqWidth, reqHeight, options.outWidth, options.outHeight, options, request);
    }

    @org.checkerframework.dataflow.qual.Impure
    static void calculateInSampleSize( @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull int reqWidth,  @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull int reqHeight,  @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull int width,  @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull int height, BitmapFactory.@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.Nullable Options options, @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull Request request) {
        options.inSampleSize = getSampleSize(reqWidth, reqHeight, width, height, request);
        options.inJustDecodeBounds = false;
    }

    @org.checkerframework.dataflow.qual.Pure
    private static  @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull int getSampleSize( @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull int reqWidth,  @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull int reqHeight,  @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull int width,  @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull int height, @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull Request request) {
        int sampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            final int heightRatio;
            final int widthRatio;
            if (reqHeight == 0) {
                sampleSize = (int) Math.floor((float) width / (float) reqWidth);
            } else if (reqWidth == 0) {
                sampleSize = (int) Math.floor((float) height / (float) reqHeight);
            } else {
                heightRatio = (int) Math.floor((float) height / (float) reqHeight);
                widthRatio = (int) Math.floor((float) width / (float) reqWidth);
                sampleSize = request.centerInside ? Math.max(heightRatio, widthRatio) : Math.min(heightRatio, widthRatio);
            }
        }
        return sampleSize;
    }

    /**
     * Decode a byte stream into a Bitmap. This method will take into account additional information
     * about the supplied request in order to do the decoding efficiently (such as through leveraging
     * {@code inSampleSize}).
     */
    @org.checkerframework.dataflow.qual.Impure
    static @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull Bitmap decodeStream(@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull Source source, @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.Nullable Request request) throws IOException {
        BufferedSource bufferedSource = Okio.buffer(source);
        if (Build.VERSION.SDK_INT >= 28) {
            return decodeStreamP(request, bufferedSource);
        }
        return decodeStreamPreP(request, bufferedSource);
    }

    @org.checkerframework.dataflow.qual.Impure
    private static @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull Bitmap decodeStreamP(@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.Nullable Request request, @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull BufferedSource bufferedSource) throws IOException {
        ImageDecoder.Source imageSource = ImageDecoder.createSource(ByteBuffer.wrap(bufferedSource.readByteArray()));
        return decodeImageSource(imageSource, request);
    }

    @org.checkerframework.dataflow.qual.Impure
    private static @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull Bitmap decodeStreamPreP(@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.Nullable Request request, @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull BufferedSource bufferedSource) throws IOException {
        boolean isWebPFile = Utils.isWebPFile(bufferedSource);
        boolean isPurgeable = request.purgeable && Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP;
        BitmapFactory.Options options = RequestHandler.createBitmapOptions(request);
        boolean calculateSize = RequestHandler.requiresInSampleSize(options);
        Bitmap bitmap;
        // We decode from a byte array because, a) when decoding a WebP network stream, BitmapFactory
        // throws a JNI Exception, so we workaround by decoding a byte array, or b) user requested
        // purgeable, which only affects bitmaps decoded from byte arrays.
        if (isWebPFile || isPurgeable) {
            byte[] bytes = bufferedSource.readByteArray();
            if (calculateSize) {
                BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
                RequestHandler.calculateInSampleSize(request.targetWidth, request.targetHeight, checkNotNull(options, "options == null"), request);
            }
            bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
        } else {
            if (calculateSize) {
                InputStream stream = new SourceBufferingInputStream(bufferedSource);
                BitmapFactory.decodeStream(stream, null, options);
                RequestHandler.calculateInSampleSize(request.targetWidth, request.targetHeight, checkNotNull(options, "options == null"), request);
            }
            bitmap = BitmapFactory.decodeStream(bufferedSource.inputStream(), null, options);
        }
        if (bitmap == null) {
            // Treat null as an IO exception, we will eventually retry.
            throw new IOException("Failed to decode bitmap.");
        }
        return bitmap;
    }

    @org.checkerframework.dataflow.qual.Impure
    static @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull Bitmap decodeResource(@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.Nullable Context context, @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.Nullable Request request) throws IOException {
        if (Build.VERSION.SDK_INT >= 28) {
            return decodeResourceP(context, request);
        }
        Resources resources = Utils.getResources(context, request);
        int id = Utils.getResourceId(resources, request);
        return decodeResourcePreP(resources, id, request);
    }

    @org.checkerframework.dataflow.qual.Impure
    private static @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull Bitmap decodeResourceP(@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.Nullable Context context, final @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.Nullable Request request) throws IOException {
        ImageDecoder.Source imageSource = ImageDecoder.createSource(context.getResources(), request.resourceId);
        return decodeImageSource(imageSource, request);
    }

    @org.checkerframework.dataflow.qual.Impure
    private static @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull Bitmap decodeResourcePreP(@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull Resources resources,  @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull int id, @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.Nullable Request request) {
        final BitmapFactory.Options options = createBitmapOptions(request);
        if (requiresInSampleSize(options)) {
            BitmapFactory.decodeResource(resources, id, options);
            calculateInSampleSize(request.targetWidth, request.targetHeight, checkNotNull(options, "options == null"), request);
        }
        return BitmapFactory.decodeResource(resources, id, options);
    }

    @org.checkerframework.dataflow.qual.Impure
    private static @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull Bitmap decodeImageSource(ImageDecoder.@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull Source imageSource, final @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.Nullable Request request) throws IOException {
        return ImageDecoder.decodeBitmap(imageSource, new ImageDecoder.OnHeaderDecodedListener() {

            @org.checkerframework.dataflow.qual.Impure
            public void onHeaderDecoded(@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull ImageDecoder imageDecoder, ImageDecoder.@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull ImageInfo imageInfo, ImageDecoder.@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull Source source) {
                if (request.hasSize()) {
                    imageDecoder.setTargetSize(request.targetWidth, request.targetHeight);
                }
            }
        });
    }

    @org.checkerframework.dataflow.qual.Impure
    static  @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull boolean isXmlResource(@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull Resources resources,  @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull int drawableId) {
        TypedValue typedValue = new TypedValue();
        resources.getValue(drawableId, typedValue, true);
        CharSequence file = typedValue.string;
        return file != null && file.toString().endsWith(".xml");
    }
}
