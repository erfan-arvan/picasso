package com.squareup.picasso3;
@RunWith(RobolectricTestRunner.class)
@Config(shadows = { Shadows.ShadowVideoThumbnails.class, Shadows.ShadowImageThumbnails.class })
public class MediaStoreRequestHandlerTest {
  @Mock Context context;
  @Before public void setUp() {
    initMocks(this);
  }
  @Test public void decodesVideoThumbnailWithVideoMimeType() {
    final Bitmap bitmap = makeBitmap();
    Request request =
        new Request.Builder(MEDIA_STORE_CONTENT_1_URL, 0, ARGB_8888).resize(100, 100).build();
    Action action = mockAction(MEDIA_STORE_CONTENT_KEY_1, request);
    MediaStoreRequestHandler requestHandler = create("video/");
    requestHandler.load(null, action.getRequest(), new RequestHandler.Callback() {
      @Override public void onSuccess(Result result) {
        assertBitmapsEqual(result.getBitmap(), bitmap);
      }
      @Override public void onError( Throwable t) {
        fail(t.getMessage());
      }
    });
  }
  @Test public void decodesImageThumbnailWithImageMimeType() {
    final Bitmap bitmap = makeBitmap(20, 20);
    Request request =
        new Request.Builder(MEDIA_STORE_CONTENT_1_URL, 0, ARGB_8888).resize(100, 100).build();
    Action action = mockAction(MEDIA_STORE_CONTENT_KEY_1, request);
    MediaStoreRequestHandler requestHandler = create("image/png");
    requestHandler.load(null, action.getRequest(), new RequestHandler.Callback() {
      @Override public void onSuccess(Result result) {
        assertBitmapsEqual(result.getBitmap(), bitmap);
      }
      @Override public void onError( Throwable t) {
        fail(t.getMessage());
      }
    });
  }
  @Test public void getPicassoKindMicro() {
    assertThat(getPicassoKind(96, 96)).isEqualTo(MICRO);
    assertThat(getPicassoKind(95, 95)).isEqualTo(MICRO);
  }
  @Test public void getPicassoKindMini() {
    assertThat(getPicassoKind(512, 384)).isEqualTo(MINI);
    assertThat(getPicassoKind(100, 100)).isEqualTo(MINI);
  }
  @Test public void getPicassoKindFull() {
    assertThat(getPicassoKind(513, 385)).isEqualTo(FULL);
    assertThat(getPicassoKind(1000, 1000)).isEqualTo(FULL);
    assertThat(getPicassoKind(1000, 384)).isEqualTo(FULL);
    assertThat(getPicassoKind(1000, 96)).isEqualTo(FULL);
    assertThat(getPicassoKind(96, 1000)).isEqualTo(FULL);
  }
  private MediaStoreRequestHandler create(String mimeType) {
    ContentResolver contentResolver = mock(ContentResolver.class);
    when(contentResolver.getType(any(Uri.class))).thenReturn(mimeType);
    return create(contentResolver);
  }
  private MediaStoreRequestHandler create(ContentResolver contentResolver) {
    when(context.getContentResolver()).thenReturn(contentResolver);
    return new MediaStoreRequestHandler(context);
  }
  static void assertBitmapsEqual(Bitmap a, Bitmap b) {
    ShadowBitmap shadowA = shadowOf(a);
    ShadowBitmap shadowB = shadowOf(b);
    if (shadowA.getHeight() != shadowB.getHeight()) {
      fail();
    }
    if (shadowA.getWidth() != shadowB.getWidth()) {
      fail();
    }
    if (shadowA.getDescription() != null ? !shadowA.getDescription().equals(shadowB.getDescription()) : shadowB.getDescription() != null) {
      fail();
    }
  }
}
