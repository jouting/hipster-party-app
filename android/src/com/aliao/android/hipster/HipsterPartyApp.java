package com.aliao.android.hipster;

import android.app.Application;
import com.parse.Parse;

/**
 * Application object.
 */
public class HipsterPartyApp extends Application {

  private HipsterActions hipsterActions;

  /**
   * Application level hipster actions.
   */
  public HipsterActions getUserActions() {
    return hipsterActions;
  }

  @Override
  public void onCreate() {
    super.onCreate();
    Parse.initialize(this, getString(R.string.parse_application_id),
        getString(R.string.parse_client_key));
    hipsterActions = ParseHipsterActions.newInstance();
  }
}
