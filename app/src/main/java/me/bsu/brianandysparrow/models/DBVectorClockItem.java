package me.bsu.brianandysparrow.models;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;

/**
 * DATABASE MODEL FOR VECTOR CLOCK ITEMS
 */
@Table(name = "DBVectorClockItems")
public class DBVectorClockItem extends Model implements Comparable<DBVectorClockItem> {

    @Column(name = "UUID")
    public String uuid;

    @Column(name = "clock")
    public int clock;

    @Column(name = "DBTweet")
    public DBTweet dbTweet;

    public DBVectorClockItem() {
        super();
    }

    public DBVectorClockItem(String uuid, int clock, DBTweet dbTweet) {
        this.uuid = uuid;
        this.clock = clock;
        this.dbTweet = dbTweet;
    }

    public DBVectorClockItem(String uuid, DBTweet dbTweet) {
        this.uuid = uuid;
        this.clock = 0;
        this.dbTweet = dbTweet;
    }

    @Override
    public int compareTo(DBVectorClockItem another) {
        return ((Integer) clock).compareTo(another.clock);
    }


    @Override
    public String toString() {
        return String.format("VC - UUID: %s | Clock: %d", uuid, clock);
    }
}
