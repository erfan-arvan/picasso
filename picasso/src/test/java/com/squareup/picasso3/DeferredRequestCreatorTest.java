package com.squareup.picasso3;
@RunWith(RobolectricTestRunner.class)
public class DeferredRequestCreatorTest {
  @Captor ArgumentCaptor<Action> actionCaptor;
  @Before public void setUp() {
    initMocks(this);
  }
  @Test public void initWhileAttachedAddsAttachAndPreDrawListener() {
    ImageView target = mockFitImageViewTarget(true);
    ViewTreeObserver observer = target.getViewTreeObserver();
    DeferredRequestCreator request =
        new DeferredRequestCreator(mock(RequestCreator.class), target, null);
    verify(observer).addOnPreDrawListener(request);
  }
  @Test public void initWhileDetachedAddsAttachListenerWhichDefersPreDrawListener() {
    ImageView target = mockFitImageViewTarget(true);
    when(target.getWindowToken()).thenReturn(null);
    ViewTreeObserver observer = target.getViewTreeObserver();
    DeferredRequestCreator request =
        new DeferredRequestCreator(mock(RequestCreator.class), target, null);
    verify(target).addOnAttachStateChangeListener(request);
    verifyNoMoreInteractions(observer);
    request.onViewAttachedToWindow(target);
    verify(observer).addOnPreDrawListener(request);
    request.onViewDetachedFromWindow(target);
    verify(observer).removeOnPreDrawListener(request);
  }
  @Test public void cancelWhileAttachedRemovesAttachListener() {
    ImageView target = mockFitImageViewTarget(true);
    DeferredRequestCreator request = new DeferredRequestCreator(mock(RequestCreator.class), target, null);
    verify(target).addOnAttachStateChangeListener(request);
    request.cancel();
    verify(target).removeOnAttachStateChangeListener(request);
  }
  @Test public void cancelClearsCallback() {
    ImageView target = mockFitImageViewTarget(true);
    Callback callback = mockCallback();
    DeferredRequestCreator request =
        new DeferredRequestCreator(mock(RequestCreator.class), target, callback);
    assertThat(request.callback).isNotNull();
    request.cancel();
    assertThat(request.callback).isNull();
  }
  @Test public void cancelClearsTag() {
    ImageView target = mockFitImageViewTarget(true);
    RequestCreator creator = mock(RequestCreator.class);
    when(creator.getTag()).thenReturn("TAG");
    DeferredRequestCreator request = new DeferredRequestCreator(creator, target, null);
    request.cancel();
    verify(creator).clearTag();
  }
  @Test public void onLayoutSkipsIfViewIsAttachedAndViewTreeObserverIsDead() {
    ImageView target = mockFitImageViewTarget(false);
    RequestCreator creator = mock(RequestCreator.class);
    DeferredRequestCreator request = new DeferredRequestCreator(creator, target, null);
    ViewTreeObserver viewTreeObserver = target.getViewTreeObserver();
    request.onPreDraw();
    verify(viewTreeObserver).addOnPreDrawListener(request);
    verify(viewTreeObserver).isAlive();
    verifyNoMoreInteractions(viewTreeObserver);
    verifyZeroInteractions(creator);
  }
  @Test public void waitsForAnotherLayoutIfWidthOrHeightIsZero() {
    ImageView target = mockFitImageViewTarget(true);
    when(target.getWidth()).thenReturn(0);
    when(target.getHeight()).thenReturn(0);
    RequestCreator creator = mock(RequestCreator.class);
    DeferredRequestCreator request = new DeferredRequestCreator(creator, target, null);
    request.onPreDraw();
    verify(target.getViewTreeObserver(), never()).removeOnPreDrawListener(request);
    verifyZeroInteractions(creator);
  }
  @Test public void cancelSkipsIfViewTreeObserverIsDead() {
    ImageView target = mockFitImageViewTarget(false);
    RequestCreator creator = mock(RequestCreator.class);
    DeferredRequestCreator request = new DeferredRequestCreator(creator, target, null);
    request.cancel();
    verify(target.getViewTreeObserver(), never()).removeOnPreDrawListener(request);
  }
  @Test public void preDrawSubmitsRequestAndCleansUp() {
    Picasso picasso = mock(Picasso.class);
    when(picasso.transformRequest(any(Request.class))).thenAnswer(TRANSFORM_REQUEST_ANSWER);
    RequestCreator creator = new RequestCreator(picasso, URI_1, 0);
    ImageView target = mockFitImageViewTarget(true);
    when(target.getWidth()).thenReturn(100);
    when(target.getHeight()).thenReturn(100);
    ViewTreeObserver observer = target.getViewTreeObserver();
    DeferredRequestCreator request = new DeferredRequestCreator(creator, target, null);
    request.onPreDraw();
    verify(observer).removeOnPreDrawListener(request);
    verify(picasso).enqueueAndSubmit(actionCaptor.capture());
    Action value = actionCaptor.getValue();
    assertThat(value).isInstanceOf(ImageViewAction.class);
    assertThat(value.getRequest().targetWidth).isEqualTo(100);
    assertThat(value.getRequest().targetHeight).isEqualTo(100);
  }
}
