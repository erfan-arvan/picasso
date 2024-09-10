package com.squareup.picasso3;
public final class Request {
  private static final long TOO_LONG_LOG = TimeUnit.SECONDS.toNanos(5);
  private static final int KEY_PADDING = 50; 
  static final char KEY_SEPARATOR = '\n';
  int id;
  long started;
  final int memoryPolicy;
  final int networkPolicy;
  public final Uri uri;
  public final int resourceId;
  public final String stableKey;
  final List<Transformation> transformations;
  public final int targetWidth;
  public final int targetHeight;
  public final boolean centerCrop;
  public final int centerCropGravity;
  public final boolean centerInside;
  public final boolean onlyScaleDown;
  public final float rotationDegrees;
  public final float rotationPivotX;
  public final float rotationPivotY;
  public final boolean hasRotationPivot;
  public final boolean purgeable;
  public final Bitmap.Config config;
  public final Priority priority;
  public final String key;
  public final Object tag;
  Request(Builder builder) {
    this.uri = builder.uri;
    this.resourceId = builder.resourceId;
    this.stableKey = builder.stableKey;
    if (builder.transformations == null) {
      this.transformations = Collections.emptyList();
    } else {
      this.transformations = Collections.unmodifiableList(new ArrayList<>(builder.transformations));
    }
    this.targetWidth = builder.targetWidth;
    this.targetHeight = builder.targetHeight;
    this.centerCrop = builder.centerCrop;
    this.centerInside = builder.centerInside;
    this.centerCropGravity = builder.centerCropGravity;
    this.onlyScaleDown = builder.onlyScaleDown;
    this.rotationDegrees = builder.rotationDegrees;
    this.rotationPivotX = builder.rotationPivotX;
    this.rotationPivotY = builder.rotationPivotY;
    this.hasRotationPivot = builder.hasRotationPivot;
    this.purgeable = builder.purgeable;
    this.config = builder.config;
    this.priority = builder.priority;
    if (Looper.myLooper() == Looper.getMainLooper()) {
      this.key = createKey();
    } else {
      this.key = createKey(new StringBuilder());
    }
    this.tag = builder.tag;
    this.memoryPolicy = builder.memoryPolicy;
    this.networkPolicy = builder.networkPolicy;
  }
  @Override public String toString() {
    final StringBuilder builder = new StringBuilder("Request{");
    if (resourceId > 0) {
      builder.append(resourceId);
    } else {
      builder.append(uri);
    }
    if (!transformations.isEmpty()) {
      for (Transformation transformation : transformations) {
        builder.append(' ').append(transformation.key());
      }
    }
    if (stableKey != null) {
      builder.append(" stableKey(").append(stableKey).append(')');
    }
    if (targetWidth > 0) {
      builder.append(" resize(").append(targetWidth).append(',').append(targetHeight).append(')');
    }
    if (centerCrop) {
      builder.append(" centerCrop");
    }
    if (centerInside) {
      builder.append(" centerInside");
    }
    if (rotationDegrees != 0) {
      builder.append(" rotation(").append(rotationDegrees);
      if (hasRotationPivot) {
        builder.append(" @ ").append(rotationPivotX).append(',').append(rotationPivotY);
      }
      builder.append(')');
    }
    if (purgeable) {
      builder.append(" purgeable");
    }
    if (config != null) {
      builder.append(' ').append(config);
    }
    builder.append('}');
    return builder.toString();
  }
  String logId() {
    long delta = System.nanoTime() - started;
    if (delta > TOO_LONG_LOG) {
      return plainId() + '+' + TimeUnit.NANOSECONDS.toSeconds(delta) + 's';
    }
    return plainId() + '+' + TimeUnit.NANOSECONDS.toMillis(delta) + "ms";
  }
  String plainId() {
    return "[R" + id + ']';
  }
  String getName() {
    if (uri != null) {
      return String.valueOf(uri.getPath());
    }
    return Integer.toHexString(resourceId);
  }
  public boolean hasSize() {
    return targetWidth != 0 || targetHeight != 0;
  }
  boolean needsMatrixTransform() {
    return hasSize() || rotationDegrees != 0;
  }
  public Builder newBuilder() {
    return new Builder(this);
  }
  private String createKey() {
    String result = createKey(MAIN_THREAD_KEY_BUILDER);
    MAIN_THREAD_KEY_BUILDER.setLength(0);
    return result;
  }
  private String createKey(StringBuilder builder) {
    Request data = this;
    if (data.stableKey != null) {
      builder.ensureCapacity(data.stableKey.length() + KEY_PADDING);
      builder.append(data.stableKey);
    } else if (data.uri != null) {
      String path = data.uri.toString();
      builder.ensureCapacity(path.length() + KEY_PADDING);
      builder.append(path);
    } else {
      builder.ensureCapacity(KEY_PADDING);
      builder.append(data.resourceId);
    }
    builder.append(KEY_SEPARATOR);
    if (data.rotationDegrees != 0) {
      builder.append("rotation:").append(data.rotationDegrees);
      if (data.hasRotationPivot) {
        builder.append('@').append(data.rotationPivotX).append('x').append(data.rotationPivotY);
      }
      builder.append(KEY_SEPARATOR);
    }
    if (data.hasSize()) {
      builder.append("resize:").append(data.targetWidth).append('x').append(data.targetHeight);
      builder.append(KEY_SEPARATOR);
    }
    if (data.centerCrop) {
      builder.append("centerCrop:").append(data.centerCropGravity).append(KEY_SEPARATOR);
    } else if (data.centerInside) {
      builder.append("centerInside").append(KEY_SEPARATOR);
    }
    for (int i = 0, count = data.transformations.size(); i < count; i++) {
      builder.append(data.transformations.get(i).key());
      builder.append(KEY_SEPARATOR);
    }
    return builder.toString();
  }
  public static final class Builder {
     Uri uri;
    int resourceId;
     String stableKey;
    int targetWidth;
    int targetHeight;
    boolean centerCrop;
    int centerCropGravity;
    boolean centerInside;
    boolean onlyScaleDown;
    float rotationDegrees;
    float rotationPivotX;
    float rotationPivotY;
    boolean hasRotationPivot;
    boolean purgeable;
     List<Transformation> transformations;
     Bitmap.Config config;
     Priority priority;
     Object tag;
    int memoryPolicy;
    int networkPolicy;
    public Builder( Uri uri) {
      setUri(uri);
    }
    public Builder(@DrawableRes int resourceId) {
      setResourceId(resourceId);
    }
    Builder( Uri uri, int resourceId,  Bitmap.Config bitmapConfig) {
      this.uri = uri;
      this.resourceId = resourceId;
      this.config = bitmapConfig;
    }
    Builder(Request request) {
      uri = request.uri;
      resourceId = request.resourceId;
      stableKey = request.stableKey;
      targetWidth = request.targetWidth;
      targetHeight = request.targetHeight;
      centerCrop = request.centerCrop;
      centerInside = request.centerInside;
      centerCropGravity = request.centerCropGravity;
      rotationDegrees = request.rotationDegrees;
      rotationPivotX = request.rotationPivotX;
      rotationPivotY = request.rotationPivotY;
      hasRotationPivot = request.hasRotationPivot;
      purgeable = request.purgeable;
      onlyScaleDown = request.onlyScaleDown;
      if (request.transformations != null) {
        transformations = new ArrayList<>(request.transformations);
      }
      config = request.config;
      priority = request.priority;
      memoryPolicy = request.memoryPolicy;
      networkPolicy = request.networkPolicy;
    }
    boolean hasImage() {
      return uri != null || resourceId != 0;
    }
    boolean hasSize() {
      return targetWidth != 0 || targetHeight != 0;
    }
    boolean hasPriority() {
      return priority != null;
    }
    public Builder setUri( Uri uri) {
      if (uri == null) {
        throw new IllegalArgumentException("Image URI may not be null.");
      }
      this.uri = uri;
      this.resourceId = 0;
      return this;
    }
    public Builder setResourceId(@DrawableRes int resourceId) {
      if (resourceId == 0) {
        throw new IllegalArgumentException("Image resource ID may not be 0.");
      }
      this.resourceId = resourceId;
      this.uri = null;
      return this;
    }
    public Builder stableKey( String stableKey) {
      this.stableKey = stableKey;
      return this;
    }
    public Builder tag( Object tag) {
      if (tag == null) {
        throw new IllegalArgumentException("Tag invalid.");
      }
      if (this.tag != null) {
        throw new IllegalStateException("Tag already set.");
      }
      this.tag = tag;
      return this;
    }
    Builder clearTag() {
      this.tag = null;
      return this;
    }
    Object getTag() {
      return tag;
    }
    public Builder resize(@Px int targetWidth, @Px int targetHeight) {
      if (targetWidth < 0) {
        throw new IllegalArgumentException("Width must be positive number or 0.");
      }
      if (targetHeight < 0) {
        throw new IllegalArgumentException("Height must be positive number or 0.");
      }
      if (targetHeight == 0 && targetWidth == 0) {
        throw new IllegalArgumentException("At least one dimension has to be positive number.");
      }
      this.targetWidth = targetWidth;
      this.targetHeight = targetHeight;
      return this;
    }
    public Builder clearResize() {
      targetWidth = 0;
      targetHeight = 0;
      centerCrop = false;
      centerInside = false;
      return this;
    }
    public Builder centerCrop() {
      return centerCrop(Gravity.CENTER);
    }
    public Builder centerCrop(int alignGravity) {
      if (centerInside) {
        throw new IllegalStateException("Center crop can not be used after calling centerInside");
      }
      centerCrop = true;
      centerCropGravity = alignGravity;
      return this;
    }
    public Builder clearCenterCrop() {
      centerCrop = false;
      centerCropGravity = Gravity.CENTER;
      return this;
    }
    public Builder centerInside() {
      if (centerCrop) {
        throw new IllegalStateException("Center inside can not be used after calling centerCrop");
      }
      centerInside = true;
      return this;
    }
    public Builder clearCenterInside() {
      centerInside = false;
      return this;
    }
    public Builder onlyScaleDown() {
      if (targetHeight == 0 && targetWidth == 0) {
        throw new IllegalStateException("onlyScaleDown can not be applied without resize");
      }
      onlyScaleDown = true;
      return this;
    }
    public Builder clearOnlyScaleDown() {
      onlyScaleDown = false;
      return this;
    }
    public Builder rotate(float degrees) {
      rotationDegrees = degrees;
      return this;
    }
    public Builder rotate(float degrees, float pivotX, float pivotY) {
      rotationDegrees = degrees;
      rotationPivotX = pivotX;
      rotationPivotY = pivotY;
      hasRotationPivot = true;
      return this;
    }
    public Builder clearRotation() {
      rotationDegrees = 0;
      rotationPivotX = 0;
      rotationPivotY = 0;
      hasRotationPivot = false;
      return this;
    }
    public Builder purgeable() {
      purgeable = true;
      return this;
    }
    public Builder config( Bitmap.Config config) {
      checkNotNull(config, "config == null");
      this.config = config;
      return this;
    }
    public Builder priority( Priority priority) {
      checkNotNull(priority, "priority == null");
      if (this.priority != null) {
        throw new IllegalStateException("Priority already set.");
      }
      this.priority = priority;
      return this;
    }
    public Builder transform( Transformation transformation) {
      checkNotNull(transformation, "transformation == null");
      if (transformation.key() == null) {
        throw new IllegalArgumentException("Transformation key must not be null.");
      }
      if (transformations == null) {
        transformations = new ArrayList<>(2);
      }
      transformations.add(transformation);
      return this;
    }
    public Builder transform( List<? extends Transformation> transformations) {
      checkNotNull(transformations, "transformations == null");
      for (int i = 0, size = transformations.size(); i < size; i++) {
        transform(transformations.get(i));
      }
      return this;
    }
    public Builder memoryPolicy( MemoryPolicy policy,
         MemoryPolicy... additional) {
      if (policy == null) {
        throw new NullPointerException("policy == null");
      }
      this.memoryPolicy |= policy.index;
      if (additional == null) {
        throw new NullPointerException("additional == null");
      }
      for (MemoryPolicy memoryPolicy : additional) {
        if (memoryPolicy == null) {
          throw new NullPointerException("additional[i] == null");
        }
        this.memoryPolicy |= memoryPolicy.index;
      }
      return this;
    }
    public Builder networkPolicy( NetworkPolicy policy,
         NetworkPolicy... additional) {
      if (policy == null) {
        throw new NullPointerException("policy == null");
      }
      this.networkPolicy |= policy.index;
      if (additional == null) {
        throw new NullPointerException("additional == null");
      }
      for (NetworkPolicy networkPolicy : additional) {
        if (networkPolicy == null) {
          throw new NullPointerException("additional[i] == null");
        }
        this.networkPolicy |= networkPolicy.index;
      }
      return this;
    }
    public Request build() {
      if (centerInside && centerCrop) {
        throw new IllegalStateException("Center crop and center inside can not be used together.");
      }
      if (centerCrop && (targetWidth == 0 && targetHeight == 0)) {
        throw new IllegalStateException(
            "Center crop requires calling resize with positive width and height.");
      }
      if (centerInside && (targetWidth == 0 && targetHeight == 0)) {
        throw new IllegalStateException(
            "Center inside requires calling resize with positive width and height.");
      }
      if (priority == null) {
        priority = Priority.NORMAL;
      }
      return new Request(this);
    }
  }
}
