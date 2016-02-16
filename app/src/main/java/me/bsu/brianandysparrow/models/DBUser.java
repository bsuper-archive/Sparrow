package me.bsu.brianandysparrow.models;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;

@Table(name = "DBUser")
public class DBUser extends Model {

    @Column(name = "username")
    String username;

    @Column(name = "uuid")
    String uuid;

    @Column(name = "public_key")
    String publicKey;

    @Column(name = "has_encryption")
    boolean hasEncryption;

    public DBUser() {
        super();
    }

    public DBUser(String username, String uuid, String publicKey, boolean hasEncryption) {
        this.username = username;
        this.uuid = uuid;
        this.publicKey = publicKey;
        this.hasEncryption = hasEncryption;
    }

    public String toString() {
        return String.format("%s (%s)", username, hasEncryption ? "encrypted" : "unencrypted");
    }
}
