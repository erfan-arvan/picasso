package com.squareup.picasso3;
public final class SourceBufferingInputStreamTest {
  @Test public void replay() throws IOException {
    Buffer data = new Buffer().writeUtf8("Hello, world!");
    BufferedSource source = Okio.buffer(new OneByteSource(data));
    InputStream stream = new SourceBufferingInputStream(source);
    byte[] out = new byte[5];
    assertEquals(5, stream.read(out));
    assertEquals("Hello", new String(out, "UTF-8"));
    assertEquals(5, source.buffer().size());
    assertEquals("Hello, world!", source.readUtf8());
  }
  @Test public void available() throws IOException {
    Buffer data = new Buffer().writeUtf8("Hello, world!");
    BufferedSource source = Okio.buffer((Source) data);
    InputStream stream = new SourceBufferingInputStream(source);
    assertEquals(0, stream.available());
    stream.read();
    assertEquals(12, stream.available());
    Buffer buffer = new Buffer().writeUtf8("Hello, world!");
    stream = new SourceBufferingInputStream(buffer);
    assertEquals(13, stream.available());
  }
  @Test public void read() throws IOException {
    Buffer data = new Buffer().writeUtf8("Hello, world!");
    BufferedSource source = Okio.buffer((Source) data);
    InputStream stream = new SourceBufferingInputStream(source);
    byte[] bytes = new byte[5];
    int len = stream.read(bytes);
    assertEquals(5, len);
    assertArrayEquals(new byte[] {'H', 'e', 'l', 'l', 'o'}, bytes);
    len = stream.read(bytes);
    assertEquals(5, len);
    assertArrayEquals(new byte[] {',', ' ', 'w', 'o', 'r'}, bytes);
    len = stream.read(bytes);
    assertEquals(3, len);
    assertArrayEquals(new byte[] {'l', 'd', '!', 'o', 'r'}, bytes);
    len = stream.read(bytes);
    assertEquals(-1, len);
  }
  @Test public void markReset() throws IOException {
    Buffer data = new Buffer().writeUtf8("Hello, world!");
    BufferedSource source = Okio.buffer((Source) data);
    InputStream stream = new SourceBufferingInputStream(source);
    stream.mark(2);
    byte[] bytes = new byte[4];
    int len = stream.read(bytes, 0, 2);
    assertEquals(2, len);
    assertArrayEquals(new byte[] {'H', 'e', 0, 0}, bytes);
    len = stream.read(bytes);
    assertEquals(len, 4);
    assertArrayEquals(new byte[] {'l', 'l', 'o', ','}, bytes);
    try {
      stream.reset();
      fail("expected IOException on reset");
    } catch (IOException expected) {}
    stream.mark(2);
    len = stream.read(bytes, 0, 2);
    assertEquals(2, len);
    assertArrayEquals(new byte[] {' ', 'w', 'o', ','}, bytes);
    stream.reset();
    len = stream.read(bytes);
    assertEquals(4, len);
    assertArrayEquals(new byte[] {' ', 'w', 'o', 'r'}, bytes);
  }
  private static final class OneByteSource implements Source {
    private final Source upstream;
    OneByteSource(Source upstream) {
      this.upstream = upstream;
    }
    @Override public long read( Buffer sink, long byteCount) throws IOException {
      if (byteCount > 0) {
        return upstream.read(sink, 1);
      }
      return 0;
    }
    @Override public Timeout timeout() {
      return upstream.timeout();
    }
    @Override public void close() throws IOException {
      upstream.close();
    }
  }
}
