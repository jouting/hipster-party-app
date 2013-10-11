package com.aliao.android.hipster;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import com.parse.FindCallback;
import com.parse.FunctionCallback;
import com.parse.GetDataCallback;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

/**
 * Implementation of Hipster actions using Facebook's Parse API.
 */
public class ParseHipsterActions implements HipsterActions {

  // Cloud API method names.
  private static final String CLOUD_GET_NEXT = "findHipster";
  private static final String CLOUD_CONNECT = "connect";

  // Connect method field names.
  private static final String CONNECT_FROM_USER = "userFrom";
  private static final String CONNECT_TO_USER = "userTo";

  // Reference to current user. Should only be read from the main UI thread.
  private HipsterUser currentUser;

  private ParseHipsterActions() {
    if (ParseUser.getCurrentUser() != null) {
      currentUser = createHipsterUser(ParseUser.getCurrentUser());
    }
  }

  public static ParseHipsterActions newInstance() {
    return new ParseHipsterActions();
  }

  @Override
  public HipsterUser getCurrentUser() {
    return currentUser;
  }

  @Override
  public void getUserScoreAsync(final HipsterUser user, final OnScoreCallback callback) {
    ParseQuery<ParseUser> userParseQuery = ParseUser.getQuery();
    userParseQuery.whereEqualTo("objectId", user.getUserId());
    userParseQuery.findInBackground(new FindCallback<ParseUser>() {
      public void done(List<ParseUser> objects, ParseException error) {
        if (error != null) {
          // Something went wrong.
          callback.onError(new HipsterException(error));
          return;
        }

        if (objects == null || objects.isEmpty()) {
          callback.onError(new HipsterException("Can't find user " + user.getUserName()));
          return;
        }

        // The query was successful.
        ParseUser parseUser = objects.get(0);
        ParseQuery<ParseObject> query = ParseQuery.getQuery(Score.SCORE_CLASS);
        query.whereEqualTo(Score.USER_FIELD, parseUser);
        query.findInBackground(new FindCallback<ParseObject>() {
          @Override
          public void done(List<ParseObject> parseObjects, ParseException error) {
            if (error != null) {
              callback.onError(new HipsterException(error));
              return;
            }
            if (parseObjects == null || parseObjects.size() == 0) {
              callback.onError(new HipsterException("Error fetching score."));
              return;
            }
            callback.onSuccess(user, parseObjects.get(0).getInt(Score.SCORE_FIELD));
          }
        });
      }
    });
  }

  @Override
  public void getUserPhotoAsync(final HipsterUser user, final OnPhotoCallback callback) {
    ParseQuery<ParseUser> userParseQuery = ParseUser.getQuery();
    userParseQuery.whereEqualTo("objectId", user.getUserId());
    userParseQuery.findInBackground(new FindCallback<ParseUser>() {
      public void done(List<ParseUser> objects, ParseException error) {
        if (error != null) {
          // Something went wrong.
          callback.onError(new HipsterException(error));
          return;
        }

        if (objects == null || objects.size() == 0) {
          callback.onError(new HipsterException("Can't find user " + user.getUserName()));
          return;
        }

        // The query was successful.
        ParseUser parseUser = objects.get(0);
        ParseFile photoFile = parseUser.getParseFile(User.PHOTO_KEY);
        photoFile.getDataInBackground(new GetDataCallback() {
          @Override
          public void done(byte[] bytes, ParseException error) {
            if (error != null) {
              callback.onError(new HipsterException(error));
              return;
            }
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            callback.onSuccess(user, bitmap);
          }
        });
      }
    });
  }

  @Override
  public void signUpAsync(final String userName, final Bitmap photo,
      final OnSignUpCallback callback) {
    AsyncTask<Void, Void, ParseUser> createUserTask = new AsyncTask<Void, Void, ParseUser>() {
      byte[] photoBytes;
      Exception exception;

      @Override
      protected void onPreExecute() {
        super.onPreExecute();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        photo.compress(Bitmap.CompressFormat.PNG, 100, stream);
        photoBytes = stream.toByteArray();
      }

      @Override
      protected ParseUser doInBackground(Void... params) {
        // Upload user photo to a remote file.
        ParseFile photo = new ParseFile(User.PHOTO_FILE_NAME, photoBytes);
        try {
          photo.save();
        } catch (ParseException error) {
          exception = error;
          return null;
        }

        // Sign up a Parse user with photo field links to the remote photo file
        ParseUser user = new ParseUser();
        user.setUsername(userName.toLowerCase());
        user.setPassword(userName.toLowerCase());
        String verifyCode = generateCode();
        user.put(User.VERIFY_CODE_KEY, verifyCode);
        user.put(User.PHOTO_KEY, photo);
        try {
          user.signUp();
        } catch (ParseException error) {
          exception = error;
          return null;
        }

        // Add a row to score table links to the current Parse user
        ParseObject score = new ParseObject(Score.SCORE_CLASS);
        score.put(Score.USER_FIELD, user);
        score.put(Score.SCORE_FIELD, 0);
        try {
          score.save();
        } catch (ParseException error) {
          exception = error;
          return null;
        }

        return user;
      }

      @Override
      protected void onPostExecute(ParseUser parseUser) {
        super.onPostExecute(parseUser);
        // Error happened during sign up
        if (exception != null) {
          exception.printStackTrace();
          callback.onError(new HipsterException(exception));
          return;
        }

        currentUser = createHipsterUser(parseUser);
        callback.onSuccess(currentUser);
      }
    };
    createUserTask.execute();
  }

  // Generate 3 digit code.
  private String generateCode() {
    Random random = new Random();
    return String.valueOf(random.nextInt(10)) + String.valueOf(random.nextInt(10)) + String
        .valueOf(random.nextInt(10));
  }

  @Override
  public void getRandomHipsterAsync(final OnNextUserFoundCallback callback) {
    HashMap<String, String> parameters = new HashMap<String, String>();
    parameters.put("userId", currentUser.getUserId());
    ParseCloud.callFunctionInBackground(CLOUD_GET_NEXT, parameters,
        new FunctionCallback<ParseUser>() {
          public void done(ParseUser parseUser, ParseException error) {
            if (error != null) {
              callback.onError(new HipsterException(error));
              return;
            }

            HipsterUser hipsterUser = createHipsterUser(parseUser);
            callback.onSuccess(hipsterUser);
          }
        }
    );
  }

  @Override
  public void connectUserAsync(final HipsterUser userFrom, final HipsterUser userTo,
      final OnConnectUserCallback callback) {
    HashMap<String, String> parameters = new HashMap<String, String>();
    parameters.put(CONNECT_FROM_USER, userFrom.getUserId());
    parameters.put(CONNECT_TO_USER, userTo.getUserId());
    parameters.put(User.VERIFY_CODE_KEY, userTo.getVerificationCode());
    ParseCloud.callFunctionInBackground(CLOUD_CONNECT, parameters,
        new FunctionCallback<String>() {
          public void done(String response, ParseException error) {
            if (error != null) {
              callback.onError(new HipsterException(error));
              return;
            }
            callback.onSuccess(userFrom, userTo);
          }
        });
  }

  HipsterUser createHipsterUser(ParseUser parseUser) {
    return new HipsterUser(parseUser.getObjectId(), parseUser.getUsername(),
        (String) parseUser.get(User.VERIFY_CODE_KEY));
  }

  // user data fields
  static class User {
    static final String PHOTO_FILE_NAME = "user_photo";
    static final String VERIFY_CODE_KEY = "verifyCode";
    static final String PHOTO_KEY = "photo";
  }

  // Score class map to Score table on Parse server
  static class Score {
    static final String SCORE_CLASS = "Score";
    static final String USER_FIELD = "userId";
    static final String SCORE_FIELD = "score";
  }
}
