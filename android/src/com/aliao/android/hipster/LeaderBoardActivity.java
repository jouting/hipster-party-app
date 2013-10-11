package com.aliao.android.hipster;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import com.actionbarsherlock.app.SherlockListActivity;
import com.parse.ParseImageView;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseQueryAdapter;
import com.parse.ParseUser;

import java.util.HashMap;
import java.util.List;

/**
 * Leaderboard activity for showing all Hipster rankings and scores.
 */
public class LeaderBoardActivity extends SherlockListActivity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Create an Parse API adapter to query user and score tables, ordered by score.
    ParseQueryAdapter.QueryFactory<ParseObject> factory =
        new ParseQueryAdapter.QueryFactory<ParseObject>() {
          public ParseQuery<ParseObject> create() {
            ParseQuery<ParseObject> leaderQuery = ParseQuery.getQuery(
                ParseHipsterActions.Score.SCORE_CLASS);
            leaderQuery.orderByDescending(ParseHipsterActions.Score.SCORE_FIELD);
            leaderQuery.include(ParseHipsterActions.Score.USER_FIELD);
            return leaderQuery;
          }
        };
    ParseUserAdapter adapter = new ParseUserAdapter(this, factory);

    // Set the ListView adapter.
    ListView listView = getListView();
    listView.setAdapter(adapter);
    listView.setCacheColorHint(Color.TRANSPARENT);
  }

  private static class ParseUserAdapter extends ParseQueryAdapter<ParseObject> {
    private final Context context;

    // Map from user score object id to overall rank (1-based). Needed because we can't easily add an
    // additional rank field to the current database object.
    private final HashMap<String, Integer> userRankingMap;

    // Placeholder image to display before the user photo is downloaded.
    private final Drawable userPhotoPlaceholder;

    private int loadIconPadding;

    public ParseUserAdapter(Context context, QueryFactory<ParseObject> queryFactory) {
      super(context, queryFactory);
      this.context = context;

      // Save the placeholder reference for reuse.
      userPhotoPlaceholder = context.getResources().getDrawable(R.drawable.ic_person_light);

      // Generate the user rankings. Parse will call onLoaded for each row in the order of the
      // query, so we infer the rank.
      userRankingMap = new HashMap<String, Integer>();
      addOnQueryLoadListener(new ParseQueryAdapter.OnQueryLoadListener<ParseObject>() {
        @Override
        public void onLoading() {
          // TODO(aliao): Maybe put in a spinner.
        }

        @Override
        public void onLoaded(List<ParseObject> parseUsers, Exception e) {

          // Read the current size because Parse supports pagination.
          int currentRank = userRankingMap.size() + 1;
          for (int i = 0; i < parseUsers.size(); i++) {
            userRankingMap.put(parseUsers.get(i).getObjectId(), currentRank + i);
          }
        }
      });

      // Calculate padding based on current density.
      loadIconPadding =
          context.getResources().getDimensionPixelSize(R.dimen.leaderboard_load_more_item_padding);
    }

    @Override
    public View getItemView(ParseObject scoreObject, View view, ViewGroup parent) {
      // Get or create the view holder.
      if (view == null) {
        view = View.inflate(context, R.layout.leaderboard_list_item, null);
        ViewHolder viewHolder = new ViewHolder();
        viewHolder.scoreText = (TextView) view.findViewById(R.id.score);
        viewHolder.nameText = (TextView) view.findViewById(R.id.name);
        viewHolder.rankingText = (TextView) view.findViewById(R.id.rank);
        viewHolder.imageView = (ParseImageView) view.findViewById(R.id.photo);
        view.setTag(viewHolder);
      }
      ViewHolder viewHolder = (ViewHolder) view.getTag();

      // Show the score and rank.
      viewHolder.scoreText.setText(scoreObject.getInt(ParseHipsterActions.Score.SCORE_FIELD)
          + " " + context.getString(R.string.points));
      int rank = userRankingMap.get(scoreObject.getObjectId());
      viewHolder.rankingText.setText(StringUtils.getRankInString(rank));

      // Show the name.
      ParseUser user = scoreObject.getParseUser(ParseHipsterActions.Score.USER_FIELD);
      viewHolder.nameText.setText(user.getUsername());

      // ParseImageView handles loading image asynchronously and cleaning up resources
      ParseImageView imageView = viewHolder.imageView;
      imageView.setParseFile(user.getParseFile(ParseHipsterActions.User.PHOTO_KEY));
      imageView.loadInBackground();
      imageView.setPlaceholder(userPhotoPlaceholder);
      return view;
    }

    @Override
    public View getNextPageView(View v, ViewGroup parent) {
      View loadMoreView = super.getNextPageView(v, parent);

      // Add more padding to allow bigger touch area.
      loadMoreView.setPadding(loadIconPadding, loadIconPadding, loadIconPadding, loadIconPadding);
      return loadMoreView;
    }

    // ViewHolder pattern to avoid extra tree traversal.
    private static class ViewHolder {
      TextView scoreText;
      TextView rankingText;
      TextView nameText;
      ParseImageView imageView;
    }
  }
}
