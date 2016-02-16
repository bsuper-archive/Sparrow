package me.bsu.brianandysparrow;

import android.test.AndroidTestCase;

import org.spongycastle.crypto.engines.AESFastEngine;
import org.spongycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.spongycastle.crypto.params.KeyParameter;
import org.spongycastle.jce.provider.BouncyCastleProvider;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.KeyPairGenerator;
import java.security.KeyPair;

/**
 * Created by aschmitt on 2/15/16.
 */
public class CryptoTest extends AndroidTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    // Tell android to use spongy castle
    static {
        Security.insertProviderAt(new BouncyCastleProvider(), 1);
    }

    public void test1() throws Exception {
//        Key AES
        KeyPair keys = CryptoUtils.generateRSAKeyPair();

        byte[] shortData = "Hello My Name is Andy!".getBytes();

        byte[] encryptedData = CryptoUtils.encryptData(shortData, keys.getPublic());
        System.out.println("Encrypted data is: " + encryptedData);

//        byte[] decryptedData = CryptoUtils.decryptData(encryptedData, )
    }

}
