package com.squareup.picasso3;
public interface Callback {
  void onSuccess();
  void onError( Throwable t);
  class EmptyCallback implements Callback {
    @Override public void onSuccess() {
    }
    @Override public void onError( Throwable t) {
    }
  }
}
