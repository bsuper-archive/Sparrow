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
import java.security.Key;

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

        // Data we want to send
        String data = "Hello My Name is Andy!";

        // Generate AES key and RSA key pair
        Key SymKey = CryptoUtils.generateAESKey();
        KeyPair AsymKeyPair = CryptoUtils.generateRSAKeyPair();

        // Encrypt Data using AES SymKey
        byte[] encryptedData = CryptoUtils.encryptData(data.getBytes(), SymKey);

        // Encrypt AES key using key pair
        byte[] encryptedKey = CryptoUtils.encryptKey(SymKey.getEncoded(), AsymKeyPair.getPublic());

        // Send data over to other side......not actually

        // Decrypt key using RSA private key
        byte[] decryptedKey = CryptoUtils.decryptKey(encryptedKey, AsymKeyPair.getPrivate());

        // Decrypt data using decrypted key
        byte[] decryptedData = CryptoUtils.decryptData(encryptedData, decrytedKey);

        System.out.println("Decrypted data is: " + encryptedData);
        assertEquals("Decrypted data didn't match", );
        
    }

}
