package com.squareup.picasso3;
@RunWith(RobolectricTestRunner.class)
public class BitmapTargetActionTest {
  @Test(expected = AssertionError.class)
  public void throwsErrorWithNullResult() {
    BitmapTarget target = mockTarget();
    BitmapTargetAction request =
        new BitmapTargetAction(mock(Picasso.class), new Target<>(target), null);
    request.complete(null);
  }
  @Test
  public void invokesSuccessIfTargetIsNotNull() {
    Bitmap bitmap = makeBitmap();
    BitmapTarget target = mockTarget();
    BitmapTargetAction request =
        new BitmapTargetAction(mock(Picasso.class), new Target<>(target), null);
    request.complete(new RequestHandler.Result(bitmap, MEMORY));
    verify(target).onBitmapLoaded(bitmap, MEMORY);
  }
  @Test
  public void invokesOnBitmapFailedIfTargetIsNotNullWithErrorDrawable() {
    Drawable errorDrawable = mock(Drawable.class);
    BitmapTarget target = mockTarget();
    Target<BitmapTarget> wrapper = new Target<>(target, errorDrawable);
    BitmapTargetAction request = new BitmapTargetAction(mock(Picasso.class), wrapper, null);
    Exception e = new RuntimeException();
    request.error(e);
    verify(target).onBitmapFailed(e, errorDrawable);
  }
  @Test
  public void invokesOnBitmapFailedIfTargetIsNotNullWithErrorResourceId() {
    Drawable errorDrawable = mock(Drawable.class);
    BitmapTarget target = mockTarget();
    Context context = mock(Context.class);
    Dispatcher dispatcher = mock(Dispatcher.class);
    PlatformLruCache cache = new PlatformLruCache(0);
    Picasso picasso =
        new Picasso(context, dispatcher, UNUSED_CALL_FACTORY, null, cache, null, NO_TRANSFORMERS,
            NO_HANDLERS, mock(Stats.class), ARGB_8888, false, false);
    Resources res = mock(Resources.class);
    Target<BitmapTarget> wrapper = new Target<>(target, RESOURCE_ID_1);
    BitmapTargetAction request = new BitmapTargetAction(picasso, wrapper, null);
    when(context.getResources()).thenReturn(res);
    when(res.getDrawable(RESOURCE_ID_1)).thenReturn(errorDrawable);
    Exception e = new RuntimeException();
    request.error(e);
    verify(target).onBitmapFailed(e, errorDrawable);
  }
  @Test public void recyclingInSuccessThrowsException() {
    BitmapTarget bad = new BitmapTarget() {
      @Override public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
        bitmap.recycle();
      }
      @Override public void onBitmapFailed(Exception e, Drawable errorDrawable) {
        throw new AssertionError();
      }
      @Override public void onPrepareLoad(Drawable placeHolderDrawable) {
        throw new AssertionError();
      }
    };
    Picasso picasso = mock(Picasso.class);
    Bitmap bitmap = makeBitmap();
    BitmapTargetAction tr = new BitmapTargetAction(picasso, new Target<>(bad), null);
    try {
      tr.complete(new RequestHandler.Result(bitmap, MEMORY));
      fail();
    } catch (IllegalStateException ignored) {
    }
  }
}
