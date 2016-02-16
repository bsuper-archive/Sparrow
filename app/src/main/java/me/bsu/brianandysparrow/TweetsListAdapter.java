package me.bsu.brianandysparrow;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import me.bsu.brianandysparrow.models.DBTweet;

/**
 * Adapter used to populate the Recycler View from a list of DBTweets
 */
public class TweetsListAdapter extends RecyclerView.Adapter<TweetsListAdapter.ViewHolder> {

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView mTweetContentTextView, mTweetSenderTextView;
        public ViewHolder(View v) {
            super(v);
            mTweetSenderTextView = (TextView) v.findViewById(R.id.tweet_sender_textview);
            mTweetContentTextView = (TextView) v.findViewById(R.id.tweet_content_textview);
        }
    }

    List<DBTweet> dbTweets;
    public TweetsListAdapter(List<DBTweet> dbTweets) {
        this.dbTweets = dbTweets;
    }

    @Override
    public TweetsListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_message, parent, false);

        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(TweetsListAdapter.ViewHolder holder, int position) {
        DBTweet t = dbTweets.get(position);
        holder.mTweetContentTextView.setText(t.content);
        holder.mTweetSenderTextView.setText(t.author);
    }

    @Override
    public int getItemCount() {
        return dbTweets.size();
    }
}
