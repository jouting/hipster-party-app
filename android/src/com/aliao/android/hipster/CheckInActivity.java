package com.aliao.android.hipster;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
import com.actionbarsherlock.app.SherlockActivity;

import java.io.File;
import java.util.List;

/**
 * Initial activity shown when the user has not yet signed up. Requires the user to enter their
 * user name and photo to sign up as a Hipster user.
 */
public class CheckInActivity extends SherlockActivity {
  // Action code used to launch the camera app and receive a photo in onActivityResult.
  private static final int TAKE_PHOTO_ACTION_CODE = 1000;

  // Regex to encourage people to use their full name.
  private static final String FULL_NAME_REGEX = "[a-zA-z]+([\\s'-][a-zA-Z]+)+";

  // The file name passed to the camera for storing a photo taken by the user.
  private static final String USER_PHOTO_FILE_NAME = "user_photo.jpg";

  // Key used by onSaveInstanceState for restoring the photo bitmap.
  public static final String SAVED_INSTANCE_BITMAP_KEY = "photoBitmap";

  // View references.
  private EditText userNameEditText;
  private View photoFrame;
  private View retakePhotoButton;
  private View takePhotoLabel;
  private ImageView photoImageView;
  private ProgressBar progressBar;

  // Bitmap of the photo taken by the user for saving to the cloud.
  private Bitmap photoBitmap;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // If logged in, skip this activity and go directly to connect.
    if (isLoggedIn()) {
      startActivity(new Intent(this, ConnectActivity.class));
      finish();
      return;
    }

    // Set view layout and get view references.
    setContentView(R.layout.activity_checkin);
    userNameEditText = (EditText) findViewById(R.id.name);
    retakePhotoButton = findViewById(R.id.retake);
    takePhotoLabel = findViewById(R.id.take_photo_label);
    photoFrame = findViewById(R.id.photo_frame);
    photoImageView = (ImageView) findViewById(R.id.photo);
    progressBar = (ProgressBar) findViewById(android.R.id.progress);

    // Setup listeners.
    Button checkInButton = (Button) findViewById(R.id.check_in);
    checkInButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        handleCheckIn();
      }
    });

    photoFrame.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        handleTakePhoto();
      }
    });

    // When restoring the activity, make sure to restore the photo bitmap.
    if (savedInstanceState != null) {
      Bitmap photoBitmap = savedInstanceState.getParcelable(SAVED_INSTANCE_BITMAP_KEY);
      renderPhoto(photoBitmap);
      return;
    }

    // Show the rules dialog. This doesn't happen if the activity was restored.
    showRulesDialog();
  }

  private boolean isLoggedIn() {
    HipsterPartyApp application = (HipsterPartyApp) getApplication();
    HipsterUser currentUser = application.getUserActions().getCurrentUser();
    return currentUser != null;
  }

  private void handleCheckIn() {
    String name = userNameEditText.getText().toString();

    // Show a failure toast if the user didn't take a photo or enter a valid full name.
    int failureMessage = 0;
    if (photoBitmap == null) {
      failureMessage = R.string.messge_take_photo;
    } else if (TextUtils.isEmpty(name) || !name.matches(FULL_NAME_REGEX)) {
      failureMessage = R.string.message_enter_name;
    }
    if (failureMessage != 0) {
      Toast.makeText(CheckInActivity.this, getString(R.string.message_enter_name),
          Toast.LENGTH_SHORT).show();
      return;
    }

    createUser(name);
  }

  private void handleTakePhoto() {
    // Launch an intent to open the camera app, if possible.
    Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    Uri photoUri = Uri.fromFile(getPhotoFile());
    cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
    if (canHandleIntent(this, cameraIntent)) {
      startActivityForResult(cameraIntent, TAKE_PHOTO_ACTION_CODE);
      return;
    }

    // No camera was available. Show an error dialog.
    AlertDialog.Builder builder = new AlertDialog.Builder(this)
        .setMessage(getString(R.string.error_photo_message))
        .setPositiveButton(getString(R.string.not_hipster),
            new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int which) {
                // Exit the app when dismissing no camera message
                finish();
              }
            });
    builder.create();
  }

  // Checks if there is a package that handles the intent.
  private boolean canHandleIntent(Context context, Intent intent) {
    final PackageManager packageManager = context.getPackageManager();
    List<ResolveInfo> list =
        packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
    return !list.isEmpty();
  }

  private void showRulesDialog() {
    // Show a dialog with ironic hipster rules message.
    AlertDialog.Builder builder = new AlertDialog.Builder(this)
        .setTitle(R.string.rules_title)
        .setMessage(R.string.rules_details)
        .setPositiveButton(R.string.rules_confirmation, null /* no click listener needed */);
    builder.show();
  }

  private File getPhotoFile() {
    // TODO(aliao): Handle no-external storage devices.
    return new File(Environment.getExternalStorageDirectory(), USER_PHOTO_FILE_NAME);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == TAKE_PHOTO_ACTION_CODE && resultCode == RESULT_OK) {
      loadPhotoFromCamera();
    }
  }

  private void loadPhotoFromCamera() {
    String photoPath = getPhotoFile().getAbsolutePath();

    // Get the dimensions of the photo frame view.
    int frameWidth = photoFrame.getWidth();
    int frameHeight = photoFrame.getHeight();

    // Get the dimensions of the bitmap.
    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inJustDecodeBounds = true;
    BitmapFactory.decodeFile(photoPath, options);
    int bitmapWidth = options.outWidth;
    int bitmapHeight = options.outHeight;

    // Determine how much to scale down the image.
    int scaleFactor = Math.min(bitmapWidth / frameWidth, bitmapHeight / frameHeight);

    // Decode the image file into a Bitmap sized to fill the frame.
    options.inJustDecodeBounds = false;
    options.inSampleSize = scaleFactor;
    options.inPurgeable = true;

    Bitmap photoBitmap = BitmapFactory.decodeFile(photoPath, options);
    renderPhoto(photoBitmap);
  }

  private void renderPhoto(Bitmap newBitmap) {
    if (photoBitmap == newBitmap) {
       return;
    }

    // Make sure to recycle the old bitmap if the user took another picture.
    if (photoBitmap != null) {
      photoBitmap.recycle();
    }

    photoBitmap = newBitmap;

    // Update the UI to display the new photo.
    if (newBitmap == null) {
      photoImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
      photoImageView.setImageResource(R.drawable.hipster_user);
      retakePhotoButton.setVisibility(View.GONE);
      takePhotoLabel.setVisibility(View.VISIBLE);
    } else {
      photoImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
      photoImageView.setImageBitmap(newBitmap);
      retakePhotoButton.setVisibility(View.VISIBLE);
      takePhotoLabel.setVisibility(View.GONE);
    }
  }

  @Override
  public void onSaveInstanceState(Bundle toSave) {
    super.onSaveInstanceState(toSave);
    toSave.putParcelable(SAVED_INSTANCE_BITMAP_KEY, photoBitmap);
  }

  private void createUser(final String name) {
    HipsterPartyApp application = (HipsterPartyApp) getApplication();
    progressBar.setVisibility(View.VISIBLE);
    application.getUserActions().signUpAsync(name, photoBitmap,
        new HipsterActions.OnSignUpCallback() {
          @Override
          public void onSuccess(HipsterUser nextUser) {
            progressBar.setVisibility(View.GONE);
            Intent connectIntent = new Intent(CheckInActivity.this, ConnectActivity.class);
            startActivity(connectIntent);
            finish();
          }

          @Override
          public void onError(Exception exception) {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(CheckInActivity.this, exception.getMessage(), Toast.LENGTH_LONG).show();
          }
        });
  }
}
