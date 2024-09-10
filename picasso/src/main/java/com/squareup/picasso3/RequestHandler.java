package com.squareup.picasso3;
public abstract class RequestHandler {
  public static final class Result {
    private final Picasso.LoadedFrom loadedFrom;
     private final Bitmap bitmap;
     private final Drawable drawable;
    private final int exifRotation;
    public Result( Bitmap bitmap,  Picasso.LoadedFrom loadedFrom) {
      this(checkNotNull(bitmap, "bitmap == null"), null, loadedFrom, 0);
    }
    public Result( Bitmap bitmap,  Picasso.LoadedFrom loadedFrom,
        int exifRotation) {
      this(checkNotNull(bitmap, "bitmap == null"), null, loadedFrom, exifRotation);
    }
    public Result( Drawable drawable,  Picasso.LoadedFrom loadedFrom) {
      this(null, checkNotNull(drawable, "drawable == null"), loadedFrom, 0);
    }
    private Result(
         Bitmap bitmap,
         Drawable drawable,
         Picasso.LoadedFrom loadedFrom,
        int exifRotation) {
      this.bitmap = bitmap;
      this.drawable = drawable;
      this.loadedFrom = checkNotNull(loadedFrom, "loadedFrom == null");
      this.exifRotation = exifRotation;
    }
     public Bitmap getBitmap() {
      return bitmap;
    }
     public Drawable getDrawable() {
      return drawable;
    }
     public Picasso.LoadedFrom getLoadedFrom() {
      return loadedFrom;
    }
    public int getExifRotation() {
      return exifRotation;
    }
  }
  public interface Callback {
    void onSuccess( Result result);
    void onError( Throwable t);
  }
  public abstract boolean canHandleRequest( Request data);
  public abstract void load( Picasso picasso,  Request request,
       Callback callback) throws IOException;
  int getRetryCount() {
    return 0;
  }
  boolean shouldRetry(boolean airplaneMode,  NetworkInfo info) {
    return false;
  }
  boolean supportsReplay() {
    return false;
  }
  static BitmapFactory.Options createBitmapOptions(Request data) {
    final boolean justBounds = data.hasSize();
    final boolean hasConfig = data.config != null;
    BitmapFactory.Options options = null;
    if (justBounds || hasConfig || data.purgeable) {
      options = new BitmapFactory.Options();
      options.inJustDecodeBounds = justBounds;
      options.inInputShareable = data.purgeable;
      options.inPurgeable = data.purgeable;
      if (hasConfig) {
        options.inPreferredConfig = data.config;
      }
    }
    return options;
  }
  static boolean requiresInSampleSize(BitmapFactory.Options options) {
    return options != null && options.inJustDecodeBounds;
  }
  static void calculateInSampleSize(int reqWidth, int reqHeight, BitmapFactory.Options options,
      Request request) {
    calculateInSampleSize(reqWidth, reqHeight, options.outWidth, options.outHeight, options,
        request);
  }
  static void calculateInSampleSize(int reqWidth, int reqHeight, int width, int height,
      BitmapFactory.Options options, Request request) {
    options.inSampleSize = getSampleSize(reqWidth, reqHeight, width, height, request);
    options.inJustDecodeBounds = false;
  }
  private static int getSampleSize(int reqWidth, int reqHeight, int width, int height,
      Request request) {
    int sampleSize = 1;
    if (height > reqHeight || width > reqWidth) {
      final int heightRatio;
      final int widthRatio;
      if (reqHeight == 0) {
        sampleSize = (int) Math.floor((float) width / (float) reqWidth);
      } else if (reqWidth == 0) {
        sampleSize = (int) Math.floor((float) height / (float) reqHeight);
      } else {
        heightRatio = (int) Math.floor((float) height / (float) reqHeight);
        widthRatio = (int) Math.floor((float) width / (float) reqWidth);
        sampleSize = request.centerInside
            ? Math.max(heightRatio, widthRatio)
            : Math.min(heightRatio, widthRatio);
      }
    }
    return sampleSize;
  }
  static Bitmap decodeStream(Source source, Request request) throws IOException {
    BufferedSource bufferedSource = Okio.buffer(source);
    if (Build.VERSION.SDK_INT >= 28) {
      return decodeStreamP(request, bufferedSource);
    }
    return decodeStreamPreP(request, bufferedSource);
  }
  @RequiresApi(28)
  @SuppressLint("Override")
  private static Bitmap decodeStreamP(Request request, BufferedSource bufferedSource)
      throws IOException {
    ImageDecoder.Source imageSource =
        ImageDecoder.createSource(ByteBuffer.wrap(bufferedSource.readByteArray()));
    return decodeImageSource(imageSource, request);
  }
  private static Bitmap decodeStreamPreP(Request request, BufferedSource bufferedSource)
      throws IOException {
    boolean isWebPFile = Utils.isWebPFile(bufferedSource);
    boolean isPurgeable = request.purgeable && Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP;
    BitmapFactory.Options options = RequestHandler.createBitmapOptions(request);
    boolean calculateSize = RequestHandler.requiresInSampleSize(options);
    Bitmap bitmap;
    if (isWebPFile || isPurgeable) {
      byte[] bytes = bufferedSource.readByteArray();
      if (calculateSize) {
        BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
        RequestHandler.calculateInSampleSize(request.targetWidth, request.targetHeight, options,
            request);
      }
      bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
    } else {
      if (calculateSize) {
        InputStream stream = new SourceBufferingInputStream(bufferedSource);
        BitmapFactory.decodeStream(stream, null, options);
        RequestHandler.calculateInSampleSize(request.targetWidth, request.targetHeight, options,
            request);
      }
      bitmap = BitmapFactory.decodeStream(bufferedSource.inputStream(), null, options);
    }
    if (bitmap == null) {
      throw new IOException("Failed to decode bitmap.");
    }
    return bitmap;
  }
  static Bitmap decodeResource(Context context, Request request)
      throws IOException {
    if (Build.VERSION.SDK_INT >= 28) {
      return decodeResourceP(context, request);
    }
    Resources resources = Utils.getResources(context, request);
    int id = Utils.getResourceId(resources, request);
    return decodeResourcePreP(resources, id, request);
  }
  @RequiresApi(28)
  private static Bitmap decodeResourceP(Context context, final Request request) throws IOException {
    ImageDecoder.Source imageSource =
        ImageDecoder.createSource(context.getResources(), request.resourceId);
    return decodeImageSource(imageSource, request);
  }
  private static Bitmap decodeResourcePreP(Resources resources, int id, Request request) {
    final BitmapFactory.Options options = createBitmapOptions(request);
    if (requiresInSampleSize(options)) {
      BitmapFactory.decodeResource(resources, id, options);
      calculateInSampleSize(request.targetWidth, request.targetHeight, options, request);
    }
    return BitmapFactory.decodeResource(resources, id, options);
  }
  @RequiresApi(28)
  private static Bitmap decodeImageSource(ImageDecoder.Source imageSource, final Request request)
      throws IOException {
    return ImageDecoder.decodeBitmap(imageSource, new ImageDecoder.OnHeaderDecodedListener() {
      @Override
      public void onHeaderDecoded( ImageDecoder imageDecoder,
           ImageDecoder.ImageInfo imageInfo,  ImageDecoder.Source source) {
        if (request.hasSize()) {
          imageDecoder.setTargetSize(request.targetWidth, request.targetHeight);
        }
      }
    });
  }
  static boolean isXmlResource(Resources resources, @DrawableRes int drawableId) {
    TypedValue typedValue = new TypedValue();
    resources.getValue(drawableId, typedValue, true);
    CharSequence file = typedValue.string;
    return file != null && file.toString().endsWith(".xml");
  }
}
