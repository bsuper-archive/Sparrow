package me.bsu.brianandysparrow;

import android.content.Context;
import android.test.AndroidTestCase;
import android.util.Log;

import com.activeandroid.query.Delete;
import com.activeandroid.query.Select;

import java.util.Collections;
import java.util.List;

import me.bsu.brianandysparrow.models.DBTweet;
import me.bsu.brianandysparrow.models.DBVectorClockItem;
import me.bsu.proto.VectorClockItem;

public class UnitTests extends AndroidTestCase {

    public static final String TAG = "UnitTests";
    Context context;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        context = getContext();
    }

    public void test1() throws Exception {
        assertEquals(4, 2 + 2);
    }

    public void test2() throws Exception {
        removeAllItemsFromDB();

        DBTweet tweet = new DBTweet(1, "brian", "hello world", "", "abc");
        tweet.save();
        DBVectorClockItem a = new DBVectorClockItem("abc", 1, tweet);
        DBVectorClockItem b = new DBVectorClockItem("bcd", 2, tweet);
        a.save();
        b.save();
        Log.d(TAG, tweet.toString());
        assertEquals(1, new Select().from(DBTweet.class).execute().size());
    }

    public void test3() throws Exception {
        removeAllItemsFromDB();

        DBTweet tweet1 = new DBTweet(1, "brian", "hello world", "", "abc");
        tweet1.save();
        DBVectorClockItem vc1a = new DBVectorClockItem("abc", 1, tweet1);
        DBVectorClockItem vc1b = new DBVectorClockItem("bcd", 2, tweet1);
        vc1a.save();
        vc1b.save();

        DBTweet tweet2 = new DBTweet(2, "andy", "hello world", "", "bcd");
        tweet2.save();
        DBVectorClockItem vc2a = new DBVectorClockItem("abc", 1, tweet2);
        DBVectorClockItem vc2b = new DBVectorClockItem("bcd", 3, tweet2);
        vc2a.save();
        vc2b.save();

        List<VectorClockItem> newVectorClockItems = Utils.createVectorClockArrayForNewMessage(context);
        assertEquals("There should only be 2 vector clocks", (int) 2, newVectorClockItems.size());
        for (VectorClockItem vc : newVectorClockItems) {
            if (vc.uuid.equals("abc")) {
                assertEquals("ABC should vector clock 1", 1, (int) vc.clock);
            }
            if (vc.uuid.equals("bcd")) {
                assertEquals("BCD should vector clock 3", 3, (int) vc.clock);
            }
        }
    }

    public void test4() throws Exception {
        removeAllItemsFromDB();

        Log.d(TAG, "====VC TEST====");
        DBTweet tweet1 = new DBTweet(1, "brian", "hello world1", "", "brian");
        tweet1.save();
        DBVectorClockItem vc1a = new DBVectorClockItem("brian", 1, tweet1);
        vc1a.save();

        DBTweet tweet2 = new DBTweet(2, "andy", "hello world2", "brian", "andy");
        tweet2.save();
        DBVectorClockItem vc2a = new DBVectorClockItem("brian", 1, tweet2);
        DBVectorClockItem vc2b = new DBVectorClockItem("andy", 2, tweet2);
        vc2a.save();
        vc2b.save();

        DBTweet tweet3 = new DBTweet(3, "joe", "hello world3", "brian", "joe");
        tweet3.save();
        DBVectorClockItem vc3a = new DBVectorClockItem("joe", 2, tweet3);
        DBVectorClockItem vc3b = new DBVectorClockItem("brian", 1, tweet3);
        vc3a.save();
        vc3b.save();

        DBTweet tweet4 = new DBTweet(4, "brian", "hello world4", "", "brian");
        tweet4.save();
        DBVectorClockItem vc4a = new DBVectorClockItem("brian", 2, tweet4);
        vc4a.save();

        DBTweet tweet5 = new DBTweet(5, "andy", "hello world5", "", "andy");
        tweet5.save();
        DBVectorClockItem vc5a = new DBVectorClockItem("andy", 3, tweet5);
        DBVectorClockItem vc5b = new DBVectorClockItem("brian", 1, tweet5);
        DBVectorClockItem vc5c = new DBVectorClockItem("joe", 1, tweet5);
        vc5a.save();
        vc5b.save();
        vc5c.save();

        List<DBTweet> dbTweets = new Select().from(DBTweet.class).execute();
        Collections.sort(dbTweets);

        for (DBTweet t : dbTweets) {
            Log.d(TAG, t.toString());
        }

        assertEquals(dbTweets.size(), 5);
    }



    public static void removeAllItemsFromDB() {
        new Delete().from(DBVectorClockItem.class).execute();
        new Delete().from(DBTweet.class).execute();
//        SQLiteUtils.execSql("DROP TABLE DBVectorClockItems");
//        SQLiteUtils.execSql("DROP TABLE DBTweets");
    }
}
