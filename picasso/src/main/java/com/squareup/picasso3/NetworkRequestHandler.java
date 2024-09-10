package com.squareup.picasso3;
final class NetworkRequestHandler extends RequestHandler {
  private static final String SCHEME_HTTP = "http";
  private static final String SCHEME_HTTPS = "https";
  private final Call.Factory callFactory;
  final Stats stats;
  NetworkRequestHandler(Call.Factory callFactory, Stats stats) {
    this.callFactory = callFactory;
    this.stats = stats;
  }
  @Override public boolean canHandleRequest( Request data) {
    String scheme = data.uri.getScheme();
    return (SCHEME_HTTP.equals(scheme) || SCHEME_HTTPS.equals(scheme));
  }
  @Override public void load( Picasso picasso,  final Request request, 
  final Callback callback) {
    okhttp3.Request callRequest = createRequest(request);
    callFactory.newCall(callRequest).enqueue(new okhttp3.Callback() {
      @Override public void onResponse(Call call, Response response) {
        if (!response.isSuccessful()) {
          callback.onError(new ResponseException(response.code(), request.networkPolicy));
          return;
        }
        Picasso.LoadedFrom loadedFrom = response.cacheResponse() == null ? NETWORK : DISK;
        ResponseBody body = response.body();
        if (loadedFrom == DISK && body.contentLength() == 0) {
          body.close();
          callback.onError(
              new ContentLengthException("Received response with 0 content-length header."));
          return;
        }
        if (loadedFrom == NETWORK && body.contentLength() > 0) {
          stats.dispatchDownloadFinished(body.contentLength());
        }
        try {
          Bitmap bitmap = decodeStream(body.source(), request);
          callback.onSuccess(new Result(bitmap, loadedFrom));
        } catch (IOException e) {
          body.close();
          callback.onError(e);
        }
      }
      @Override public void onFailure(Call call, IOException e) {
        callback.onError(e);
      }
    });
  }
  @Override int getRetryCount() {
    return 2;
  }
  @Override boolean shouldRetry(boolean airplaneMode,  NetworkInfo info) {
    return info == null || info.isConnected();
  }
  @Override boolean supportsReplay() {
    return true;
  }
  private static okhttp3.Request createRequest(Request request) {
    CacheControl cacheControl = null;
    int networkPolicy = request.networkPolicy;
    if (networkPolicy != 0) {
      if (NetworkPolicy.isOfflineOnly(networkPolicy)) {
        cacheControl = CacheControl.FORCE_CACHE;
      } else {
        CacheControl.Builder builder = new CacheControl.Builder();
        if (!NetworkPolicy.shouldReadFromDiskCache(networkPolicy)) {
          builder.noCache();
        }
        if (!NetworkPolicy.shouldWriteToDiskCache(networkPolicy)) {
          builder.noStore();
        }
        cacheControl = builder.build();
      }
    }
    okhttp3.Request.Builder builder = new okhttp3.Request.Builder().url(request.uri.toString());
    if (cacheControl != null) {
      builder.cacheControl(cacheControl);
    }
    return builder.build();
  }
  static class ContentLengthException extends RuntimeException {
    ContentLengthException(String message) {
      super(message);
    }
  }
  static final class ResponseException extends RuntimeException {
    final int code;
    final int networkPolicy;
    ResponseException(int code, int networkPolicy) {
      super("HTTP " + code);
      this.code = code;
      this.networkPolicy = networkPolicy;
    }
  }
}
