package com.squareup.picasso3;
final class SourceBufferingInputStream extends InputStream {
  private final BufferedSource source;
  private final Buffer buffer;
  private long position;
  private long markPosition = -1;
  private long markLimit = -1;
  SourceBufferingInputStream(BufferedSource source) {
    this.source = source;
    this.buffer = source.buffer();
  }
  private final Buffer temp = new Buffer();
  private int copyTo(byte[] sink, int offset, int byteCount) {
    buffer.copyTo(temp, position, byteCount);
    return temp.read(sink, offset, byteCount);
  }
  @Override public int read() throws IOException {
    if (!source.request(position + 1)) {
      return -1;
    }
    byte value = buffer.getByte(position++);
    if (position > markLimit) {
      markPosition = -1;
    }
    return value;
  }
  @Override public int read( byte[] b, int off, int len) throws IOException {
    if (off < 0 || len < 0 || len > b.length - off) {
      throw new IndexOutOfBoundsException();
    } else if (len == 0) {
      return 0;
    }
    int count = len;
    if (!source.request(position + count)) {
      count = available();
    }
    if (count == 0) return -1;
    int copied = copyTo(b, off, count);
    position += copied;
    if (position > markLimit) {
      markPosition = -1;
    }
    return copied;
  }
  @Override public long skip(long n) throws IOException {
    source.require(position + n);
    position += n;
    if (position > markLimit) {
      markPosition = -1;
    }
    return n;
  }
  @Override public boolean markSupported() {
    return true;
  }
  @Override public void mark(int readlimit) {
    markPosition = position;
    markLimit = position + readlimit;
  }
  @Override public void reset() throws IOException {
    if (markPosition == -1) {
      throw new IOException("No mark or mark expired");
    }
    position = markPosition;
    markPosition = -1;
    markLimit = -1;
  }
  @Override public int available() {
    return (int) Math.min(buffer.size() - position, Integer.MAX_VALUE);
  }
  @Override public void close() {
  }
}
