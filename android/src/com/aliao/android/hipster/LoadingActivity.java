package com.aliao.android.hipster;

import android.content.Intent;
import android.os.Bundle;
import com.actionbarsherlock.app.SherlockActivity;
import com.parse.ParseAnalytics;

/**
 * Created by aliao on 7/14/13.
 */
public class LoadingActivity extends SherlockActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    ParseAnalytics.trackAppOpened(getIntent());
  }

  @Override
  protected void onResume() {
    super.onResume();

    HipsterPartyApp application = (HipsterPartyApp) getApplication();
    HipsterUser currentUser = application.getUserActions().getCurrentUser();
    if (currentUser != null) {
      startActivity(new Intent(this, ConnectActivity.class));
    } else {
      startActivity(new Intent(this, CheckInActivity.class));
    }
  }
}
