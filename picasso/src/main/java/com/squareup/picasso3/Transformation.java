package com.squareup.picasso3;
public interface Transformation {
   RequestHandler.Result transform( RequestHandler.Result source);
   String key();
}
