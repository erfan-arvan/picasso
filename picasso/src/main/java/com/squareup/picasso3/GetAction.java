package com.squareup.picasso3;
class GetAction extends Action<Void> {
  GetAction(Picasso picasso, Request data) {
    super(picasso, null, data);
  }
  @Override void complete(RequestHandler.Result result) {
  }
  @Override public void error(Exception e) {
  }
}
