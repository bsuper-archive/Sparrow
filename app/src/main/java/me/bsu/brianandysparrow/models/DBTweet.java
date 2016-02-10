package me.bsu.brianandysparrow.models;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;

import java.util.ArrayList;
import java.util.List;

import me.bsu.proto.VectorClockItem;

@Table(name = "DBTweets")
public class DBTweet extends Model implements Comparable<DBTweet> {

    @Column(name = "tweet_id")
    public int tweetID;

    @Column(name = "author")
    public String author;

    @Column(name = "content")
    public String content;

    @Column(name = "recipient")
    public String recipient;

    @Column(name = "sender_uuid")
    public String senderUUID;

    public DBTweet() {
        super();
    }

    public DBTweet(int tweetID, String author, String content, String recipient,
                    String senderUUID) {
        this.tweetID = tweetID;
        this.author = author;
        this.content = content;
        this.recipient = recipient;
        this.senderUUID = senderUUID;
    }

    public List<DBVectorClockItem> dbVectorClockItems() {
        return getMany(DBVectorClockItem.class, "DBTweet");
    }

    public List<VectorClockItem> vectorClockItems() {
        List<VectorClockItem> vcItems = new ArrayList<>();
        for (DBVectorClockItem dbVC : dbVectorClockItems()) {
            vcItems.add(new VectorClockItem.Builder().clock(dbVC.clock).uuid(dbVC.uuid).build());
        }
        return vcItems;
    }

    @Override
    public int compareTo(DBTweet tweet2) {
        List<DBVectorClockItem> vc1List = this.dbVectorClockItems();
        List<DBVectorClockItem> vc2List = tweet2.dbVectorClockItems();

        boolean tweet1HasALowerEntry = false;
        boolean tweet1HasAHigherEntry = false;
        for (DBVectorClockItem vc1 : vc1List) {
            for (DBVectorClockItem vc2 : vc2List) {
                if (vc1.uuid.equals(vc2.uuid)) {
                    int cmp = vc1.compareTo(vc2);
                    if (cmp == -1) {
                        tweet1HasALowerEntry = true;
                    } else if (cmp == 1) {
                        tweet1HasAHigherEntry = true;
                    }
                }
            }
        }
        if (tweet1HasAHigherEntry == tweet1HasALowerEntry) {
            return 0;
        } else {
            return tweet1HasALowerEntry ? -1 : 1;
        }
    }
}
