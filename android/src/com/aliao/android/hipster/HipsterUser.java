package com.aliao.android.hipster;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * A hipster user abstraction.
 */
public class HipsterUser implements Parcelable {
  private final String userId;
  private final String userName;
  private final String verificationCode;

  public HipsterUser(String userId, String userName, String verificationCode) {
    this.userId = userId;
    this.userName = userName;
    this.verificationCode = verificationCode;
  }

  private HipsterUser(Parcel in) {
    userId = in.readString();
    userName = in.readString();
    verificationCode = in.readString();
  }

  /**
   * Returns user's unique id.
   */
  public String getUserId() {
    return userId;
  }

  /**
   * Returns user's name.
   */
  public String getUserName() {
    return userName;
  }

  /**
   * Returns user's verification code.
   */
  public String getVerificationCode() {
    return verificationCode;
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(userId);
    dest.writeString(userName);
    dest.writeString(verificationCode);
  }

  public static final Parcelable.Creator<HipsterUser> CREATOR =
      new Parcelable.Creator<HipsterUser>() {
    public HipsterUser createFromParcel(Parcel in) {
      return new HipsterUser(in);
    }

    public HipsterUser[] newArray(int size) {
      return new HipsterUser[size];
    }
  };
}