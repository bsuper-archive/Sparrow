package me.bsu.brianandysparrow;

import android.util.Log;

import org.spongycastle.crypto.engines.AESFastEngine;
import org.spongycastle.crypto.engines.RSAEngine;
import org.spongycastle.crypto.generators.RSAKeyPairGenerator;
import org.spongycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.spongycastle.crypto.params.AsymmetricKeyParameter;
import org.spongycastle.crypto.params.KeyParameter;
import org.spongycastle.crypto.params.RSAKeyParameters;
import org.spongycastle.crypto.prng.FixedSecureRandom;
import org.spongycastle.crypto.util.PublicKeyFactory;
import org.spongycastle.jcajce.provider.symmetric.AES;
import org.spongycastle.jce.provider.BouncyCastleProvider;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.KeyPairGenerator;
import java.security.KeyPair;
import java.security.Key;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.KeyGenerator;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.SecretKey;

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
    private static Cipher AESCipher;
    private static Cipher RSACipher;

    // Tell android to use spongy castle
    static {
        Security.insertProviderAt(new BouncyCastleProvider(), 1);
        try {
            RSACipher = Cipher.getInstance("RSA/ECB/PKCS1Padding", "SC");
            AESCipher = Cipher.getInstance("AES", "SC");
        } catch (Exception e) {
            Log.d(TAG, "Failed to init the Cipher");
        }
    }

    /******************
     * Key Generators *
     ******************/

    static KeyPair generateRSAKeyPair() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA", "SC");
        keyGen.initialize(RSA_KEY_SIZE);
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
        return getRSAResult(dataKey, RSAPublicKey, true);
    }

    static SecretKey decryptKey(byte[] dataKey, PrivateKey RSAPrivKey) {
        return new SecretKeySpec(getRSAResult(dataKey, RSAPrivKey, false), "AES");
    }

    static byte[] getRSAResult(byte[] data, Key key, Boolean encrypt) {
        byte[] result = null;
        try {
            if (encrypt) {
                RSACipher.init(Cipher.ENCRYPT_MODE, (PublicKey) key);
            } else {
                RSACipher.init(Cipher.DECRYPT_MODE, (PrivateKey) key);
            }

            result = RSACipher.doFinal(data);
        } catch(Exception e) {
            Log.d(TAG, "RSA failed");
        }
        return result;
    }

    /**********************************
     * AES Encryption for data blocks *
     **********************************/

    static byte[] encryptData(byte[] data, Key key) throws Exception {
        return getAESResult(data, key, true);
    }

    static byte[] decryptData(byte[] data, Key key) throws Exception {
        return getAESResult(data, key, false);
    }

    static byte[] getAESResult(byte[] data, Key key, Boolean encrypt) throws Exception {
        AlgorithmParameterSpec iv = new IvParameterSpec("ASDFASDFKLJASLASDFASDFASDFASDFLKASDFASDFASDFASDASDFASDFJHHJHJASDFIEW".getBytes()); // this could be randomized for extra security, but eh
        int mode = (encrypt ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE);
        byte[] result = null;
        AESCipher.init(mode, key, iv);
        result = AESCipher.doFinal(data);

        return result;
    }
}
