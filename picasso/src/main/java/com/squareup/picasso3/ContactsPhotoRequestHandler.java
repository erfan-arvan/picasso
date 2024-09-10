package com.squareup.picasso3;
class ContactsPhotoRequestHandler extends RequestHandler {
  private static final int ID_DISPLAY_PHOTO = 4;
  private static final UriMatcher matcher;
  static {
    matcher = new UriMatcher(UriMatcher.NO_MATCH);
    matcher.addURI(ContactsContract.AUTHORITY, "contacts/lookup/*/#", ID_LOOKUP);
    matcher.addURI(ContactsContract.AUTHORITY, "contacts/lookup/*", ID_LOOKUP);
    matcher.addURI(ContactsContract.AUTHORITY, "contacts/#/photo", ID_THUMBNAIL);
    matcher.addURI(ContactsContract.AUTHORITY, "contacts/#", ID_CONTACT);
    matcher.addURI(ContactsContract.AUTHORITY, "display_photo/#", ID_DISPLAY_PHOTO);
  }
  private final Context context;
  ContactsPhotoRequestHandler(Context context) {
    this.context = context;
  }
  @Override public boolean canHandleRequest( Request data) {
    final Uri uri = data.uri;
    return (SCHEME_CONTENT.equals(uri.getScheme())
        && ContactsContract.Contacts.CONTENT_URI.getHost().equals(uri.getHost())
        && matcher.match(data.uri) != UriMatcher.NO_MATCH);
  }
  @Override
  public void load( Picasso picasso,  Request request,  Callback callback) {
    boolean signaledCallback = false;
    try {
      Source source = getSource(request);
      if (source == null) {
        signaledCallback = true;
        callback.onError(new IOException("no contact found"));
        return;
      }
      Bitmap bitmap = decodeStream(source, request);
      signaledCallback = true;
      callback.onSuccess(new Result(bitmap, DISK));
    } catch (Exception e) {
      if (!signaledCallback) {
        callback.onError(e);
      }
    }
  }
  private Source getSource(Request data) throws IOException {
    ContentResolver contentResolver = context.getContentResolver();
    Uri uri = data.uri;
    InputStream is;
    switch (matcher.match(uri)) {
      case ID_LOOKUP:
        uri = ContactsContract.Contacts.lookupContact(contentResolver, uri);
        if (uri == null) {
          return null;
        }
      case ID_CONTACT:
        is = openContactPhotoInputStream(contentResolver, uri, true);
        break;
      case ID_THUMBNAIL:
      case ID_DISPLAY_PHOTO:
        is = contentResolver.openInputStream(uri);
        break;
      default:
        throw new IllegalStateException("Invalid uri: " + uri);
    }
    return is == null ? null : Okio.source(is);
  }
}
