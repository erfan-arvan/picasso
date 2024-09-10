package com.squareup.picasso3;
@RunWith(RobolectricTestRunner.class)
public class ImageViewActionTest {
  @Test(expected = AssertionError.class)
  public void throwsErrorWithNullResult() {
    ImageView target = mockImageViewTarget();
    ImageViewAction action =
        new ImageViewAction(mock(Picasso.class), new Target<>(target), null, null);
    action.complete(null);
  }
  @Test
  public void invokesTargetAndCallbackSuccessIfTargetIsNotNull() {
    Bitmap bitmap = makeBitmap();
    Dispatcher dispatcher = mock(Dispatcher.class);
    PlatformLruCache cache = new PlatformLruCache(0);
    Picasso picasso =
        new Picasso(RuntimeEnvironment.application, dispatcher, UNUSED_CALL_FACTORY, null, cache,
            null, NO_TRANSFORMERS, NO_HANDLERS, mock(Stats.class), Bitmap.Config.ARGB_8888, false,
            false);
    ImageView target = mockImageViewTarget();
    Callback callback = mockCallback();
    ImageViewAction request =
        new ImageViewAction(picasso, new Target<>(target), null, callback);
    request.complete(new RequestHandler.Result(bitmap, MEMORY));
    verify(target).setImageDrawable(any(PicassoDrawable.class));
    verify(callback).onSuccess();
  }
  @Test
  public void invokesTargetAndCallbackErrorIfTargetIsNotNullWithErrorResourceId() {
    ImageView target = mockImageViewTarget();
    Callback callback = mockCallback();
    Picasso mock = mock(Picasso.class);
    Target<ImageView> wrapper = new Target<>(target, RESOURCE_ID_1);
    ImageViewAction request = new ImageViewAction(mock, wrapper, null, callback);
    Exception e = new RuntimeException();
    request.error(e);
    verify(target).setImageResource(RESOURCE_ID_1);
    verify(callback).onError(e);
  }
  @Test
  public void invokesErrorIfTargetIsNotNullWithErrorResourceId() {
    ImageView target = mockImageViewTarget();
    Callback callback = mockCallback();
    Picasso mock = mock(Picasso.class);
    Target<ImageView> wrapper = new Target<>(target, RESOURCE_ID_1);
    ImageViewAction request = new ImageViewAction(mock, wrapper, null, callback);
    Exception e = new RuntimeException();
    request.error(e);
    verify(target).setImageResource(RESOURCE_ID_1);
    verify(callback).onError(e);
  }
  @Test
  public void invokesErrorIfTargetIsNotNullWithErrorDrawable() {
    Drawable errorDrawable = mock(Drawable.class);
    ImageView target = mockImageViewTarget();
    Callback callback = mockCallback();
    Picasso mock = mock(Picasso.class);
    Target<ImageView> wrapper = new Target<>(target, errorDrawable);
    ImageViewAction request = new ImageViewAction(mock, wrapper, null, callback);
    Exception e = new RuntimeException();
    request.error(e);
    verify(target).setImageDrawable(errorDrawable);
    verify(callback).onError(e);
  }
  @Test
  public void clearsCallbackOnCancel() {
    Picasso picasso = mock(Picasso.class);
    ImageView target = mockImageViewTarget();
    Callback callback = mockCallback();
    ImageViewAction request =
        new ImageViewAction(picasso, new Target<>(target), null, callback);
    request.cancel();
    assertThat(request.callback).isNull();
  }
  @Test
  public void stopPlaceholderAnimationOnError() {
    Picasso picasso = mock(Picasso.class);
    AnimationDrawable placeholder = mock(AnimationDrawable.class);
    ImageView target = mockImageViewTarget();
    when(target.getDrawable()).thenReturn(placeholder);
    ImageViewAction request =
        new ImageViewAction(picasso, new Target<>(target), null, null);
    request.error(new RuntimeException());
    verify(placeholder).stop();
  }
}
