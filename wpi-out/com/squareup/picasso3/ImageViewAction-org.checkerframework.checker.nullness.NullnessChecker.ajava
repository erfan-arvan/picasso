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

import android.content.Context;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

@org.checkerframework.framework.qual.AnnotatedFor("org.checkerframework.checker.nullness.NullnessChecker")
class ImageViewAction extends Action<ImageView> {

    @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.Nullable Callback callback;

    @org.checkerframework.dataflow.qual.SideEffectFree
    ImageViewAction(@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.Nullable Picasso picasso, @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull Target<ImageView> target, @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.Nullable Request data, @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.Nullable Callback callback) {
        super(picasso, target, data);
        this.callback = callback;
    }

    @org.checkerframework.dataflow.qual.Impure
    public void complete(@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull ImageViewAction this, RequestHandler.@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.Nullable Result result) {
        if (result == null) {
            throw new AssertionError(String.format("Attempted to complete action with no result!\n%s", this));
        }
        ImageView target = wrapper.target;
        Context context = picasso.context;
        boolean indicatorsEnabled = picasso.indicatorsEnabled;
        PicassoDrawable.setResult(target, context, result, wrapper.noFade, indicatorsEnabled);
        if (callback != null) {
            callback.onSuccess();
        }
    }

    @org.checkerframework.dataflow.qual.Impure
    public void error(@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull ImageViewAction this, @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.Nullable Exception e) {
        ImageView target = wrapper.target;
        Drawable placeholder = target.getDrawable();
        if (placeholder instanceof Animatable) {
            ((Animatable) placeholder).stop();
        }
        if (wrapper.errorResId != 0) {
            target.setImageResource(wrapper.errorResId);
        } else if (wrapper.errorDrawable != null) {
            target.setImageDrawable(wrapper.errorDrawable);
        }
        if (callback != null) {
            callback.onError(e);
        }
    }

    @org.checkerframework.dataflow.qual.Impure
    void cancel(@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull ImageViewAction this) {
        super.cancel();
        if (callback != null) {
            callback = null;
        }
    }
}
