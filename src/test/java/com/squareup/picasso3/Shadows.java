package com.squareup.picasso3;
import javax.annotation.Nullable;
import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.provider.MediaStore;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import static com.squareup.picasso3.TestUtils.makeBitmap;

final class Shadows {

    @Implements(MediaStore.Video.Thumbnails.class)
    public static class ShadowVideoThumbnails {

        @Implementation
        public static Bitmap getThumbnail(@Nullable ContentResolver cr, @Nullable long origId, @Nullable int kind, @Nullable BitmapFactory.Options options) {
            return makeBitmap();
        }

        private ShadowVideoThumbnails() {
        }
    }

    @Implements(MediaStore.Images.Thumbnails.class)
    public static class ShadowImageThumbnails {

        @Implementation
        public static Bitmap getThumbnail(@Nullable ContentResolver cr, @Nullable long origId, @Nullable int kind, @Nullable BitmapFactory.Options options) {
            return makeBitmap(20, 20);
        }

        private ShadowImageThumbnails() {
        }
    }

    private Shadows() {
    }
}
