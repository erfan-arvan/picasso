package com.squareup.picasso3;
@RunWith(RobolectricTestRunner.class)
public class PicassoDrawableTest {
  private final Context context = RuntimeEnvironment.application;
  private final Drawable placeholder = new ColorDrawable(RED);
  private final Bitmap bitmap = makeBitmap();
  @Test public void createWithNoPlaceholderAnimation() {
    PicassoDrawable pd = new PicassoDrawable(context, bitmap, null, DISK, false, false);
    assertThat(pd.getBitmap()).isSameAs(bitmap);
    assertThat(pd.placeholder).isNull();
    assertThat(pd.animating).isTrue();
  }
  @Test public void createWithPlaceholderAnimation() {
    PicassoDrawable pd = new PicassoDrawable(context, bitmap, placeholder, DISK, false, false);
    assertThat(pd.getBitmap()).isSameAs(bitmap);
    assertThat(pd.placeholder).isSameAs(placeholder);
    assertThat(pd.animating).isTrue();
  }
  @Test public void createWithBitmapCacheHit() {
    PicassoDrawable pd = new PicassoDrawable(context, bitmap, placeholder, MEMORY, false, false);
    assertThat(pd.getBitmap()).isSameAs(bitmap);
    assertThat(pd.placeholder).isNull();
    assertThat(pd.animating).isFalse();
  }
}
