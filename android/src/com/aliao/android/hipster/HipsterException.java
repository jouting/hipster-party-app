package com.aliao.android.hipster;

/**
 * Exception class encapsulating Hipster app errors.
 */
class HipsterException extends Exception {

  public HipsterException(String detailMessage) {
    super(detailMessage);
  }

  public HipsterException(String detailMessage, Throwable throwable) {
    super(detailMessage, throwable);
  }

  public HipsterException(Throwable throwable) {
    super(throwable);
  }

  public HipsterException() {
  }
}
