package com.squareup.picasso3;

import android.content.Context;
import android.net.Uri;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.MockitoAnnotations.initMocks;

@org.checkerframework.framework.qual.AnnotatedFor("org.checkerframework.checker.nullness.NullnessChecker")
public class AssetRequestHandlerTest {

    @org.checkerframework.checker.initialization.qual.FBCBottom @org.checkerframework.checker.nullness.qual.MonotonicNonNull Context context;

    @org.checkerframework.dataflow.qual.Impure
    public void setUp() {
        initMocks(this);
    }

    @org.checkerframework.dataflow.qual.Impure
    public void truncatesFilePrefix() throws IOException {
        Uri uri = Uri.parse("file:///android_asset/foo/bar.png");
        Request request = new Request.Builder(uri).build();
        String actual = AssetRequestHandler.getFilePath(request);
        assertThat(actual).isEqualTo("foo/bar.png");
    }
}
