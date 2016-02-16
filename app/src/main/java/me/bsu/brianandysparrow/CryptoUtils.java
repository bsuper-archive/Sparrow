package me.bsu.brianandysparrow;

import android.util.Log;

import org.spongycastle.crypto.engines.AESFastEngine;
import org.spongycastle.crypto.engines.RSAEngine;
import org.spongycastle.crypto.generators.RSAKeyPairGenerator;
import org.spongycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.spongycastle.crypto.params.KeyParameter;
import org.spongycastle.crypto.prng.FixedSecureRandom;
import org.spongycastle.jcajce.provider.symmetric.AES;
import org.spongycastle.jce.provider.BouncyCastleProvider;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.KeyPairGenerator;
import java.security.KeyPair;
import java.security.Key;
import java.security.SecureRandom;

import javax.crypto.KeyGenerator;

/**
 * Created by aschmitt on 2/15/16.
 *
 * In a message we send a key encrypted using RSA and
 * a block of data that can be decrypted by that key using AES.
 */
public class CryptoUtils {

    private static final String TAG = "me.bsu.CryptoUtils";
    private static final int RSA_KEY_SIZE = 1024;
    private static final int AES_KEY_SIZE = 256;
    private static final AESFastEngine AESCipher = new AESFastEngine();
    private static final RSAEngine RSACipher = new RSAEngine();

    // Tell android to use spongy castle
    static {
        Security.insertProviderAt(new BouncyCastleProvider(), 1);
    }

    /******************
     * Key Generators *
     ******************/

    static KeyPair generateRSAKeyPair() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA", "SC");
        keyGen.initialize(RSA_KEY_SIZE, new SecureRandom());
        return keyGen.generateKeyPair();
    };

    static Key generateAESKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES", "SC");
        keyGen.init(AES_KEY_SIZE);
        return keyGen.generateKey();
    }

    /***************************
     * RSA Encryption for keys *
     ***************************/

    static byte[] encryptKey(byte[] dataKey, PublicKey RSAPublicKey) {
        RSACipher.init(true, new KeyParameter(RSAPublicKey.getEncoded()));
        return RSACipher.processBlock(dataKey, 0, dataKey.length);
    }

    static byte[] decryptKey(byte[] dataKey, PrivateKey RSAPrivKey) {
        RSACipher.init(false, new KeyParameter(RSAPrivKey.getEncoded()));
        return RSACipher.processBlock(dataKey, 0, dataKey.length);
    }

    /**********************************
     * AES Encryption for data blocks *
     **********************************/

    static byte[] encryptData(byte[] data, PublicKey pubKey) {
        return getAESResult(data, pubKey, true);
    }

    static byte[] decryptData(byte[] data, PublicKey pubKey) {
        return getAESResult(data, pubKey, false);
    }

    static byte[] getAESResult(byte[] data, PublicKey pubKey, Boolean encrypt) {
        AESCipher.init(encrypt, new KeyParameter(pubKey.getEncoded()));

        byte[] result = new byte[data.length];
        int numBytesProcessed = AESCipher.processBlock(data, 0, result, data.length);

        if (! (numBytesProcessed == data.length) ) {
            Log.d(TAG, "Did not process correct number of bytes");
        }

        return result;
    }
}
