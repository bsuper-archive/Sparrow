package me.bsu.brianandysparrow;

import android.content.Context;
import android.util.Log;

import me.bsu.brianandysparrow.models.DBTweet;
import me.bsu.brianandysparrow.models.DBVectorClockItem;

public class Tests {

    public static final String TAG = "me.bsu.Tests";

    public static void test1(Context context) {
        DBTweet tweet = new DBTweet(1, "brian", "hello world", "", "abc");
        tweet.save();
        DBVectorClockItem a = new DBVectorClockItem("abc", 1, tweet);
        DBVectorClockItem b = new DBVectorClockItem("bcd", 2, tweet);
        a.save();
        b.save();
        Log.d(TAG, tweet.toString());
    }
}
