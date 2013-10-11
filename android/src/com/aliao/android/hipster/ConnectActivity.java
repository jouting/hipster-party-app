package com.aliao.android.hipster;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

/**
 * The main activity used to connect to other Hipsters!
 */
public class ConnectActivity extends SherlockActivity implements View.OnClickListener {

  // Keys to restore the activity instance state.
  private static final String SAVED_INSTANCE_TARGET_HIPSTER_KEY = "target_hipster";
  private static final String SAVED_INSTANCE_TARGET_PHOTO_KEY = "target_photo";
  private static final String SAVED_INSTANCE_SCORE_KEY = "score";

  // Current user score not fetched sentinel value.
  private static final int NO_SCORE = -1;

  // View references.
  private TextView scoreLabel;
  private ImageView hipsterImage;
  private TextView hipsterName;
  private EditText hipsterCodeInput;
  private View connectButton;
  private TextView skipButton;
  private ProgressBar loadingSpinner;

  // Current user score
  private int score = NO_SCORE;

  // Target references.
  private HipsterUser targetHipster;
  private Bitmap targetHipsterBitmap;

  // Available server actions.
  private HipsterActions actions;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_connect);

    // Obtain UI references.
    hipsterImage = (ImageView) findViewById(R.id.photo);
    hipsterName = (TextView) findViewById(R.id.name);
    hipsterCodeInput = (EditText) findViewById(R.id.code);
    connectButton = findViewById(R.id.connect);
    connectButton.setOnClickListener(this);
    skipButton = (TextView) findViewById(R.id.skip);
    skipButton.setOnClickListener(this);
    loadingSpinner = (ProgressBar) findViewById(android.R.id.progress);

    // Get server actions.
    HipsterPartyApp application = (HipsterPartyApp) getApplication();
    actions = application.getUserActions();

    // Display the current user's verification code in the action bar.
    HipsterUser currentUser = actions.getCurrentUser();
    ActionBar actionBar = getSupportActionBar();
    String codeTitle = String.format(getString(R.string.subtitle_code),
        currentUser.getVerificationCode());
    actionBar.setSubtitle(codeTitle);

    // Get the saved state if available.
    if (savedInstanceState != null) {
      targetHipster = savedInstanceState.getParcelable(SAVED_INSTANCE_TARGET_HIPSTER_KEY);
      targetHipsterBitmap = savedInstanceState.getParcelable(SAVED_INSTANCE_TARGET_PHOTO_KEY);
      score = savedInstanceState.getInt(SAVED_INSTANCE_SCORE_KEY);
    }

    // Show the target hipster info.
    if (targetHipster == null) {
      fetchTargetHipster();
    } else {
      renderTargetHipster(targetHipster);

      // Render the bitmap only when the target hipster is present.
      if (targetHipsterBitmap == null) {
        fetchHipsterPhoto(targetHipster);
      } else {
        renderHipsterPhoto(targetHipsterBitmap);
      }
    }
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putParcelable(SAVED_INSTANCE_TARGET_HIPSTER_KEY, targetHipster);
    outState.putParcelable(SAVED_INSTANCE_TARGET_PHOTO_KEY, targetHipsterBitmap);
    outState.putInt(SAVED_INSTANCE_SCORE_KEY, score);
  }

  private void fetchCurrentUserScore() {
    actions.getUserScoreAsync(actions.getCurrentUser(), new HipsterActions.OnScoreCallback() {
      @Override
      public void onSuccess(HipsterUser user, int score) {
        ConnectActivity.this.score = score;
        renderCurrentUserScore(score);
      }

      @Override
      public void onError(Exception exception) {
        Toast.makeText(ConnectActivity.this, exception.getMessage(), Toast.LENGTH_SHORT).show();
      }
    });
  }

  private void renderCurrentUserScore(int score) {
    if (score == NO_SCORE) {
      scoreLabel.setText("");
    } else {
      scoreLabel.setText(score + " " + getString(R.string.points_abbreviation));
    }
  }

  private void fetchTargetHipster() {
    renderLoadingState(true);

    actions.getRandomHipsterAsync(new HipsterActions.OnNextUserFoundCallback() {
      @Override
      public void onSuccess(HipsterUser nextUser) {
        renderLoadingState(false);
        targetHipster = nextUser;
        renderTargetHipster(nextUser);
        fetchHipsterPhoto(nextUser);
      }

      @Override
      public void onError(Exception exception) {
        renderLoadingState(false);
        Toast.makeText(ConnectActivity.this, exception.getMessage(), Toast.LENGTH_SHORT).show();
      }
    });
  }

  private void renderLoadingState(boolean isFetching) {
    loadingSpinner.setVisibility(!isFetching ? View.GONE : View.VISIBLE);
    connectButton.setEnabled(!isFetching);
    skipButton.setEnabled(!isFetching);
  }

  private void renderTargetHipster(HipsterUser hipster) {
    String userName = hipster.getUserName();
    hipsterName.setText(userName);

    String codeHint = getString(R.string.enter_code);
    hipsterCodeInput.setText("");
    hipsterCodeInput.setHint(String.format(codeHint, userName));

    String skipText = getString(R.string.skip);
    skipButton.setText(String.format(skipText, userName));
  }

  private void fetchHipsterPhoto(HipsterUser connectToUser) {
    hipsterImage.setImageResource(R.drawable.hipster_user);

    if (targetHipsterBitmap != null) {
      targetHipsterBitmap.recycle();
      targetHipsterBitmap = null;
    }

    actions.getUserPhotoAsync(connectToUser, new HipsterActions.OnPhotoCallback() {
      @Override
      public void onSuccess(HipsterUser user, Bitmap photo) {
        targetHipsterBitmap = photo;
        renderHipsterPhoto(photo);
      }

      @Override
      public void onError(Exception exception) {
        Toast.makeText(ConnectActivity.this, exception.getMessage(), Toast.LENGTH_SHORT).show();
      }
    });
  }

  private void renderHipsterPhoto(Bitmap bitmap) {
    hipsterImage.setImageBitmap(bitmap);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getSupportMenuInflater().inflate(R.menu.connect, menu);

    // This custom menu item displays the user's score.
    MenuItem scoreMenuItem = menu.findItem(R.id.menu_score);
    scoreLabel = (TextView) scoreMenuItem.getActionView();

    // Show the current user score.
    if (score == NO_SCORE) {
      fetchCurrentUserScore();
    } else {
      renderCurrentUserScore(score);
    }

    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onMenuItemSelected(int featureId, MenuItem item) {
    int itemId = item.getItemId();

    if (itemId == R.id.menu_refresh) {
      fetchCurrentUserScore();
      return true;
    }

    if (itemId == R.id.menu_leaderboard) {
      startActivity(new Intent(this, LeaderBoardActivity.class));
      return true;
    }
    return super.onMenuItemSelected(featureId, item);
  }

  @Override
  public void onClick(View v) {
    int id = v.getId();

    if (id == R.id.skip) {
      fetchTargetHipster();
      return;
    }

    if (id == R.id.connect) {
      // Can't connect if there is no target hipster.
      if (targetHipster == null) {
        return;
      }

      // Client-side check for the hipster code. A more secure implementation might check on the
      // server, but this isn't critical.
      if (verifyCode(targetHipster, hipsterCodeInput.getText().toString())) {
        connect(actions.getCurrentUser(), targetHipster);
      }
    }
  }

  private boolean verifyCode(HipsterUser hipsterUser, String typedCode) {
    if (!hipsterUser.getVerificationCode().equals(typedCode)) {
      Toast.makeText(ConnectActivity.this, getString(R.string.error_connect_code) +
          hipsterUser.getUserName(), Toast.LENGTH_SHORT).show();
      return false;
    }

    return true;
  }

  private void connect(HipsterUser currentUser, HipsterUser targetHipster) {
    renderLoadingState(true);

    connectButton.setEnabled(false);
    loadingSpinner.setVisibility(View.VISIBLE);
    HipsterPartyApp application = (HipsterPartyApp) getApplication();
    application.getUserActions().connectUserAsync(currentUser, targetHipster,
        new HipsterActions.OnConnectUserCallback() {
          @Override
          public void onSuccess(HipsterUser userFrom, HipsterUser userTo) {
            renderLoadingState(false);
            fetchCurrentUserScore();
            fetchTargetHipster();
          }

          @Override
          public void onError(Exception exception) {
            renderLoadingState(false);
            Toast.makeText(ConnectActivity.this, exception.getMessage(), Toast.LENGTH_SHORT).show();
          }
        });
  }
}
