package com.squareup.picasso3;
@RunWith(RobolectricTestRunner.class)
public class RequestHandlerTest {
  @Test public void bitmapConfig() {
    for (Bitmap.Config config : Bitmap.Config.values()) {
      Request data = new Request.Builder(URI_1).config(config).build();
      Request copy = data.newBuilder().build();
      assertThat(createBitmapOptions(data).inPreferredConfig).isSameAs(config);
      assertThat(createBitmapOptions(copy).inPreferredConfig).isSameAs(config);
    }
  }
  @Test public void requiresComputeInSampleSize() {
    assertThat(requiresInSampleSize(null)).isFalse();
    final BitmapFactory.Options defaultOptions = new BitmapFactory.Options();
    assertThat(requiresInSampleSize(defaultOptions)).isFalse();
    final BitmapFactory.Options justBounds = new BitmapFactory.Options();
    justBounds.inJustDecodeBounds = true;
    assertThat(requiresInSampleSize(justBounds)).isTrue();
  }
  @Test public void calculateInSampleSizeNoResize() {
    final BitmapFactory.Options options = new BitmapFactory.Options();
    Request data = new Request.Builder(URI_1).build();
    calculateInSampleSize(100, 100, 150, 150, options, data);
    assertThat(options.inSampleSize).isEqualTo(1);
  }
  @Test public void calculateInSampleSizeResize() {
    final BitmapFactory.Options options = new BitmapFactory.Options();
    Request data = new Request.Builder(URI_1).build();
    calculateInSampleSize(100, 100, 200, 200, options, data);
    assertThat(options.inSampleSize).isEqualTo(2);
  }
  @Test public void calculateInSampleSizeResizeCenterInside() {
    final BitmapFactory.Options options = new BitmapFactory.Options();
    Request data = new Request.Builder(URI_1).centerInside().resize(100, 100).build();
    calculateInSampleSize(data.targetWidth, data.targetHeight, 400, 200, options, data);
    assertThat(options.inSampleSize).isEqualTo(4);
  }
  @Test public void calculateInSampleSizeKeepAspectRatioWithWidth() {
    final BitmapFactory.Options options = new BitmapFactory.Options();
    Request data = new Request.Builder(URI_1).resize(400, 0).build();
    calculateInSampleSize(data.targetWidth, data.targetHeight, 800, 200, options, data);
    assertThat(options.inSampleSize).isEqualTo(2);
  }
  @Test public void calculateInSampleSizeKeepAspectRatioWithHeight() {
    final BitmapFactory.Options options = new BitmapFactory.Options();
    Request data = new Request.Builder(URI_1).resize(0, 100).build();
    calculateInSampleSize(data.targetWidth, data.targetHeight, 800, 200, options, data);
    assertThat(options.inSampleSize).isEqualTo(2);
  }
  @Test public void nullBitmapOptionsIfNoResizingOrPurgeable() {
    final Request noResize = new Request.Builder(URI_1).build();
    final BitmapFactory.Options noResizeOptions = createBitmapOptions(noResize);
    assertThat(noResizeOptions).isNull();
  }
  @Test public void inJustDecodeBoundsIfResizing() {
    final Request requiresResize = new Request.Builder(URI_1).resize(20, 15).build();
    final BitmapFactory.Options resizeOptions = createBitmapOptions(requiresResize);
    assertThat(resizeOptions).isNotNull();
    assertThat(resizeOptions.inJustDecodeBounds).isTrue();
    assertThat(resizeOptions.inPurgeable).isFalse();
    assertThat(resizeOptions.inInputShareable).isFalse();
  }
  @Test public void inPurgeableIfInPurgeable() {
    final Request request = new Request.Builder(URI_1).purgeable().build();
    final BitmapFactory.Options options = createBitmapOptions(request);
    assertThat(options).isNotNull();
    assertThat(options.inPurgeable).isTrue();
    assertThat(options.inInputShareable).isTrue();
    assertThat(options.inJustDecodeBounds).isFalse();
  }
  @Test public void createWithConfigAndNotInJustDecodeBoundsOrInPurgeable() {
    final Request config = new Request.Builder(URI_1).config(RGB_565).build();
    final BitmapFactory.Options configOptions = createBitmapOptions(config);
    assertThat(configOptions).isNotNull();
    assertThat(configOptions.inJustDecodeBounds).isFalse();
    assertThat(configOptions.inPurgeable).isFalse();
    assertThat(configOptions.inInputShareable).isFalse();
  }
}
