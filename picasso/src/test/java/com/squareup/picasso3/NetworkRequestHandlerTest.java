package com.squareup.picasso3;
@RunWith(RobolectricTestRunner.class)
public class NetworkRequestHandlerTest {
  final BlockingDeque<Response> responses = new LinkedBlockingDeque<>();
  final BlockingDeque<okhttp3.Request> requests = new LinkedBlockingDeque<>();
  @Mock Picasso picasso;
  @Mock Stats stats;
  @Mock Dispatcher dispatcher;
  private NetworkRequestHandler networkHandler;
  @Before public void setUp() {
    initMocks(this);
    networkHandler = new NetworkRequestHandler(new Call.Factory() {
      @Override public Call newCall(Request request) {
        requests.add(request);
        try {
          return new PremadeCall(request, responses.takeFirst());
        } catch (InterruptedException e) {
          throw new AssertionError(e);
        }
      }
    }, stats);
  }
  @Test public void doesNotForceLocalCacheOnlyWithAirplaneModeOffAndRetryCount() throws Exception {
    responses.add(responseOf(ResponseBody.create(null, new byte[10])));
    Action action = TestUtils.mockAction(URI_KEY_1, URI_1);
    final CountDownLatch latch = new CountDownLatch(1);
    networkHandler.load(picasso, action.getRequest(), new RequestHandler.Callback() {
      @Override public void onSuccess(Result result) {
        try {
          assertThat(requests.takeFirst().cacheControl().toString()).isEmpty();
          latch.countDown();
        } catch (InterruptedException e) {
          throw new AssertionError(e);
        }
      }
      @Override public void onError( Throwable t) {
        throw new AssertionError(t);
      }
    });
    assertThat(latch.await(10, SECONDS)).isTrue();
  }
  @Test public void withZeroRetryCountForcesLocalCacheOnly() throws Exception {
    responses.add(responseOf(ResponseBody.create(null, new byte[10])));
    Action action = TestUtils.mockAction(URI_KEY_1, URI_1);
    PlatformLruCache cache = new PlatformLruCache(0);
    BitmapHunter hunter =
        new BitmapHunter(picasso, dispatcher, cache, stats, action, networkHandler);
    hunter.retryCount = 0;
    hunter.hunt();
    assertThat(requests.takeFirst().cacheControl().toString()).isEqualTo(CacheControl.FORCE_CACHE.toString());
  }
  @Test public void shouldRetryTwiceWithAirplaneModeOffAndNoNetworkInfo() {
    Action action = TestUtils.mockAction(URI_KEY_1, URI_1);
    PlatformLruCache cache = new PlatformLruCache(0);
    BitmapHunter hunter =
        new BitmapHunter(picasso, dispatcher, cache, stats, action, networkHandler);
    assertThat(hunter.shouldRetry(false, null)).isTrue();
    assertThat(hunter.shouldRetry(false, null)).isTrue();
    assertThat(hunter.shouldRetry(false, null)).isFalse();
  }
  @Test public void shouldRetryWithUnknownNetworkInfo() {
    assertThat(networkHandler.shouldRetry(false, null)).isTrue();
    assertThat(networkHandler.shouldRetry(true, null)).isTrue();
  }
  @Test public void shouldRetryWithConnectedNetworkInfo() {
    NetworkInfo info = mockNetworkInfo();
    when(info.isConnected()).thenReturn(true);
    assertThat(networkHandler.shouldRetry(false, info)).isTrue();
    assertThat(networkHandler.shouldRetry(true, info)).isTrue();
  }
  @Test public void shouldNotRetryWithDisconnectedNetworkInfo() {
    NetworkInfo info = mockNetworkInfo();
    when(info.isConnectedOrConnecting()).thenReturn(false);
    assertThat(networkHandler.shouldRetry(false, info)).isFalse();
    assertThat(networkHandler.shouldRetry(true, info)).isFalse();
  }
  @Test public void noCacheAndKnownContentLengthDispatchToStats() throws Exception {
    responses.add(responseOf(ResponseBody.create(null, new byte[10])));
    Action action = TestUtils.mockAction(URI_KEY_1, URI_1);
    final CountDownLatch latch = new CountDownLatch(1);
    networkHandler.load(picasso, action.getRequest(), new RequestHandler.Callback() {
      @Override public void onSuccess(Result result) {
        verify(stats).dispatchDownloadFinished(10);
        latch.countDown();
      }
      @Override public void onError( Throwable t) {
        throw new AssertionError(t);
      }
    });
    assertThat(latch.await(10, SECONDS)).isTrue();
  }
  @Test public void unknownContentLengthFromDiskThrows() throws Exception {
    final AtomicBoolean closed = new AtomicBoolean();
    ResponseBody body = new ResponseBody() {
      @Override public MediaType contentType() { return null; }
      @Override public long contentLength() { return 0; }
      @Override public BufferedSource source() { return new Buffer(); }
      @Override public void close() {
        closed.set(true);
        super.close();
      }
    };
    responses.add(responseOf(body)
        .newBuilder()
        .cacheResponse(responseOf(null))
        .build());
    Action action = TestUtils.mockAction(URI_KEY_1, URI_1);
    final CountDownLatch latch = new CountDownLatch(1);
    networkHandler.load(picasso, action.getRequest(), new RequestHandler.Callback() {
      @Override public void onSuccess(Result result) {
        throw new AssertionError();
      }
      @Override public void onError( Throwable t) {
        verifyZeroInteractions(stats);
        assertTrue(closed.get());
        latch.countDown();
      }
    });
    assertThat(latch.await(10, SECONDS)).isTrue();
  }
  @Test public void cachedResponseDoesNotDispatchToStats() throws Exception {
    responses.add(responseOf(ResponseBody.create(null, new byte[10]))
        .newBuilder()
        .cacheResponse(responseOf(null))
        .build());
    Action action = TestUtils.mockAction(URI_KEY_1, URI_1);
    final CountDownLatch latch = new CountDownLatch(1);
    networkHandler.load(picasso, action.getRequest(), new RequestHandler.Callback() {
      @Override public void onSuccess(Result result) {
        verifyZeroInteractions(stats);
        latch.countDown();
      }
      @Override public void onError( Throwable t) {
        throw new AssertionError(t);
      }
    });
    assertThat(latch.await(10, SECONDS)).isTrue();
  }
  private static Response responseOf(ResponseBody body) {
    return new Response.Builder()
        .code(200)
        .protocol(HTTP_1_1)
        .request(new okhttp3.Request.Builder().url("http:
        .message("OK")
        .body(body)
        .build();
  }
}
