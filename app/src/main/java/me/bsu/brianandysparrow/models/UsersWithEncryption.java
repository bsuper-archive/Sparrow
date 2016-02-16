package me.bsu.brianandysparrow.models;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;

@Table(name = "UsersWithEncryption")
public class UsersWithEncryption extends Model {

    @Column(name = "username")
    String username;

    @Column(name = "uuid")
    String uuid;

    @Column(name = "public_key")
    String publicKey;

    public UsersWithEncryption() {
        super();
    }

    public UsersWithEncryption(String username, String uuid, String publicKey) {
        this.username = username;
        this.uuid = uuid;
        this.publicKey = publicKey;
    }
}
