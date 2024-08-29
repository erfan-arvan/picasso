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

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import okio.Source;
import static android.content.ContentResolver.SCHEME_CONTENT;
import static android.content.ContentUris.parseId;
import static android.provider.MediaStore.Images;
import static android.provider.MediaStore.Images.Thumbnails.FULL_SCREEN_KIND;
import static android.provider.MediaStore.Images.Thumbnails.MICRO_KIND;
import static android.provider.MediaStore.Images.Thumbnails.MINI_KIND;
import static android.provider.MediaStore.Video;
import static com.squareup.picasso3.MediaStoreRequestHandler.PicassoKind.FULL;
import static com.squareup.picasso3.MediaStoreRequestHandler.PicassoKind.MICRO;
import static com.squareup.picasso3.MediaStoreRequestHandler.PicassoKind.MINI;
import static com.squareup.picasso3.Picasso.LoadedFrom.DISK;

@org.checkerframework.framework.qual.AnnotatedFor("org.checkerframework.checker.nullness.NullnessChecker")
class MediaStoreRequestHandler extends ContentStreamRequestHandler {

    private static final @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull String @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull [] CONTENT_ORIENTATION = new String[] { Images.ImageColumns.ORIENTATION };

    @org.checkerframework.dataflow.qual.Impure
    MediaStoreRequestHandler(@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.Nullable Context context) {
        super(context);
    }

    @org.checkerframework.dataflow.qual.Impure
    public  @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull boolean canHandleRequest(@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull MediaStoreRequestHandler this, @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.Nullable Request data) {
        final Uri uri = data.uri;
        return (SCHEME_CONTENT.equals(uri.getScheme()) && MediaStore.AUTHORITY.equals(uri.getAuthority()));
    }

    @org.checkerframework.dataflow.qual.Impure
    public void load(@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull MediaStoreRequestHandler this, @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.Nullable Picasso picasso, @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.Nullable Request request, @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull Callback callback) {
        boolean signaledCallback = false;
        try {
            ContentResolver contentResolver = context.getContentResolver();
            int exifOrientation = getExifOrientation(request);
            String mimeType = contentResolver.getType(request.uri);
            boolean isVideo = mimeType != null && mimeType.startsWith("video/");
            if (request.hasSize()) {
                PicassoKind picassoKind = getPicassoKind(request.targetWidth, request.targetHeight);
                if (!isVideo && picassoKind == FULL) {
                    Source source = getSource(request);
                    Bitmap bitmap = decodeStream(source, request);
                    signaledCallback = true;
                    callback.onSuccess(new Result(bitmap, DISK, exifOrientation));
                    return;
                }
                long id = parseId(request.uri);
                BitmapFactory.Options options = createBitmapOptions(request);
                options.inJustDecodeBounds = true;
                calculateInSampleSize(request.targetWidth, request.targetHeight, picassoKind.width, picassoKind.height, options, request);
                Bitmap bitmap;
                if (isVideo) {
                    // Since MediaStore doesn't provide the full screen kind thumbnail, we use the mini kind
                    // instead which is the largest thumbnail size can be fetched from MediaStore.
                    int kind = (picassoKind == FULL) ? Video.Thumbnails.MINI_KIND : picassoKind.androidKind;
                    bitmap = Video.Thumbnails.getThumbnail(contentResolver, id, kind, options);
                } else {
                    bitmap = Images.Thumbnails.getThumbnail(contentResolver, id, picassoKind.androidKind, options);
                }
                if (bitmap != null) {
                    signaledCallback = true;
                    callback.onSuccess(new Result(bitmap, DISK, exifOrientation));
                    return;
                }
            }
            Source source = getSource(request);
            Bitmap bitmap = decodeStream(source, request);
            signaledCallback = true;
            callback.onSuccess(new Result(bitmap, DISK, exifOrientation));
        } catch (Exception e) {
            if (!signaledCallback) {
                callback.onError(e);
            }
        }
    }

    @org.checkerframework.dataflow.qual.Pure
    static @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull PicassoKind getPicassoKind( @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull int targetWidth,  @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull int targetHeight) {
        if (targetWidth <= MICRO.width && targetHeight <= MICRO.height) {
            return MICRO;
        } else if (targetWidth <= MINI.width && targetHeight <= MINI.height) {
            return MINI;
        }
        return FULL;
    }

    @org.checkerframework.dataflow.qual.Impure
    protected  @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull int getExifOrientation(@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull MediaStoreRequestHandler this, @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.Nullable Request request) {
        Cursor cursor = null;
        try {
            ContentResolver contentResolver = context.getContentResolver();
            cursor = contentResolver.query(request.uri, CONTENT_ORIENTATION, null, null, null);
            if (cursor == null || !cursor.moveToFirst()) {
                return 0;
            }
            return cursor.getInt(0);
        } catch (RuntimeException ignored) {
            // If the orientation column doesn't exist, assume no rotation.
            return 0;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    enum PicassoKind {

        MICRO(MICRO_KIND, 96, 96), MINI(MINI_KIND, 512, 384), FULL(FULL_SCREEN_KIND, -1, -1);

        final  @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull int androidKind;

        final  @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull int width;

        final  @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull int height;

        @org.checkerframework.dataflow.qual.Impure
        PicassoKind( @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull int androidKind,  @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull int width,  @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull int height) {
            this.androidKind = androidKind;
            this.width = width;
            this.height = height;
        }
    }
}
