package com.squareup.picasso3;
@RunWith(RobolectricTestRunner.class)
public class AssetRequestHandlerTest {
  @Mock Context context;
  @Before public void setUp() {
    initMocks(this);
  }
  @Test public void truncatesFilePrefix() throws IOException {
    Uri uri = Uri.parse("file:
    Request request = new Request.Builder(uri).build();
    String actual = AssetRequestHandler.getFilePath(request);
    assertThat(actual).isEqualTo("foo/bar.png");
  }
}
