package com.squareup.picasso3;
class PicassoExecutorService extends ThreadPoolExecutor {
  private static final int DEFAULT_THREAD_COUNT = 3;
  PicassoExecutorService() {
    super(DEFAULT_THREAD_COUNT, DEFAULT_THREAD_COUNT, 0, TimeUnit.MILLISECONDS,
        new PriorityBlockingQueue<Runnable>(), new Utils.PicassoThreadFactory());
  }
  @Override
  public Future<?> submit(Runnable task) {
    PicassoFutureTask ftask = new PicassoFutureTask((BitmapHunter) task);
    execute(ftask);
    return ftask;
  }
  private static final class PicassoFutureTask extends FutureTask<BitmapHunter>
      implements Comparable<PicassoFutureTask> {
    private final BitmapHunter hunter;
    PicassoFutureTask(BitmapHunter hunter) {
      super(hunter, null);
      this.hunter = hunter;
    }
    @Override
    public int compareTo(PicassoFutureTask other) {
      Picasso.Priority p1 = hunter.getPriority();
      Picasso.Priority p2 = other.hunter.getPriority();
      return (p1 == p2 ? hunter.sequence - other.hunter.sequence : p2.ordinal() - p1.ordinal());
    }
  }
}
