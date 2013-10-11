package com.aliao.android.hipster;

import android.graphics.Bitmap;

/**
 * Interface that should be implemented to handle user actions.
 */
public interface HipsterActions {

  /**
   * Returns the current user on the device.
   */
  public HipsterUser getCurrentUser();

  /**
   * Signs up a new user with user name and photo, call back will be called on UI thread when action
   * is done.
   */
  public void signUpAsync(String userName, Bitmap photo, OnSignUpCallback callback);

  /**
   * Gets specific user's score, call back will be called on UI thread when action is done.
   */
  public void getUserScoreAsync(HipsterUser user, OnScoreCallback callback);

  /**
   * Gets specific user's photo, call back will be called on UI thread when action is done.
   */
  public void getUserPhotoAsync(HipsterUser user, OnPhotoCallback callback);

  /**
   * Gets next random user to connect to, call back will be called on UI thread when action is done.
   */
  public void getRandomHipsterAsync(OnNextUserFoundCallback callback);

  /**
   * Connects two users, call back will be called on UI thread when action is done.
   */
  public void connectUserAsync(
      HipsterUser userFrom, HipsterUser userTo, OnConnectUserCallback callback);

  /**
   * Interface definition for a callback to be invoked when the sign up action has been done.
   */
  public interface OnSignUpCallback {

    /**
     * Called when a user is successfully signed up.
     */
    public void onSuccess(HipsterUser nextUser);

    /**
     * Called when an error has occurred.
     */
    public void onError(Exception exception);
  }

  /**
   * Interface definition for a callback to be invoked when an action is performed to retrieve
   * user's score.
   */
  public interface OnScoreCallback {

    /**
     * Called when a user's score is successfully retrieved.
     */
    public void onSuccess(HipsterUser user, int score);

    /**
     * Called when an error has occurred.
     */
    public void onError(Exception exception);
  }

  /**
   * Interface definition for a callback to be invoked when an action is performed to retrieve
   * user's photo.
   */
  public interface OnPhotoCallback {

    /**
     * Called when a user's photo is successfully retrieved.
     */
    public void onSuccess(HipsterUser user, Bitmap photo);

    /**
     * Called when an error has occurred.
     */
    public void onError(Exception exception);
  }

  /**
   * Interface definition for a callback to be invoked when an action is performed to connect two
   * users.
   */
  public interface OnConnectUserCallback {

    /**
     * Called when two users are successfully connected.
     */
    public void onSuccess(HipsterUser userFrom, HipsterUser userTo);

    /**
     * Called when an error has occurred.
     */
    public void onError(Exception exception);
  }

  /**
   * Interface definition for a callback to be invoked when an action is performed to find next
   * available user to connect.
   */
  public interface OnNextUserFoundCallback {

    /**
     * Called when the next available random user is successfully retrieved.
     */
    public void onSuccess(HipsterUser nextUser);

    /**
     * Called when an error has occurred.
     */
    public void onError(Exception exception);

  }
}
