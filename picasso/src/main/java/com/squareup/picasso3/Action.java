package com.squareup.picasso3;
abstract class Action<T> {
  final Picasso picasso;
  final Request request;
  final Target<T> wrapper;
  boolean willReplay;
  boolean cancelled;
  Action(Picasso picasso, Target<T> wrapper, Request request) {
    this.picasso = picasso;
    this.request = request;
    this.wrapper = wrapper;
  }
  abstract void complete(RequestHandler.Result result);
  abstract void error(Exception e);
  void cancel() {
    cancelled = true;
  }
  Request getRequest() {
    return request;
  }
  T getTarget() {
    return wrapper.target;
  }
  String getKey() {
    return request.key;
  }
  boolean isCancelled() {
    return cancelled;
  }
  boolean willReplay() {
    return willReplay;
  }
  Picasso getPicasso() {
    return picasso;
  }
  Priority getPriority() {
    return request.priority;
  }
  Object getTag() {
    return request.tag != null ? request.tag : this;
  }
}
