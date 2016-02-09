package me.bsu.brianandysparrow.models;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;

import java.util.List;

@Table(name = "DBTweets")
public class DBTweet extends Model {

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

    public List<DBVectorClockItem> vectorClockItems() {
        return getMany(DBVectorClockItem.class, "DBTweet");
    }


}
