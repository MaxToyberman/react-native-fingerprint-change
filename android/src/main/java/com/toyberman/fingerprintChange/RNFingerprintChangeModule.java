package com.toyberman.fingerprintChange;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

public class RNFingerprintChangeModule extends ReactContextBaseJavaModule {

    private final ReactApplicationContext reactContext;
    private final String DEFAULT_KEY_NAME = "default_key";
    private final String INIT_KEYSTORE = "INIT_KEYSTORE";
    private Cipher defaultCipher;
    private SharedPreferences spref;
    private KeyStore mKeyStore;
    private KeyGenerator mKeyGenerator;

    public RNFingerprintChangeModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        //check if device has fingerprint
        if (!hasFingerprintHardware(reactContext)) {
            return;
        }


        // The enrolling flow for fingerprint. This is where you ask the user to set up fingerprint
        // for your flow. Use of keys is necessary if you need to know if the set of
        // enrolled fingerprints has changed.
        try {

            defaultCipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/"
                    + KeyProperties.BLOCK_MODE_CBC + "/"
                    + KeyProperties.ENCRYPTION_PADDING_PKCS7);


            mKeyStore = KeyStore.getInstance("AndroidKeyStore");

            mKeyGenerator = KeyGenerator
                    .getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");

            spref = PreferenceManager.getDefaultSharedPreferences(reactContext);
            //when initializing the app we want to create the key only one so we can detect changes
            if (spref.getBoolean(INIT_KEYSTORE, true)) {
                createKeyWithHandler();
                spref.edit().putBoolean(INIT_KEYSTORE, false).apply();
            }

        } catch (KeyStoreException | NoSuchAlgorithmException | NoSuchProviderException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        }

    }


    @ReactMethod
    public void hasFingerPrintChanged(Callback errorCallback, Callback successCallback) {

        if (!hasFingerprintHardware(this.reactContext)) {
            return;
        }


        if (initCipher(defaultCipher, DEFAULT_KEY_NAME)) {
            successCallback.invoke(false);
        } else {
            if (this.reactContext != null) {
                //after we find a change in a fingerprint we need to reinitialize the keystore
                spref.edit().putBoolean(INIT_KEYSTORE, true).apply();
                //createKey(DEFAULT_KEY_NAME, true);
                createKeyWithHandler();
                successCallback.invoke(true);
            }
        }


    }

    private boolean hasFingerprintHardware(Context mContext) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //Fingerprint API only available on from Android 6.0 (M)
            FingerprintManager fingerprintManager = (FingerprintManager) mContext.getSystemService(Context.FINGERPRINT_SERVICE);

            if (fingerprintManager == null) {
                return false;
            }

            return fingerprintManager.isHardwareDetected();
        } else {
            // Supporting devices with SDK < 23
            FingerprintManagerCompat fingerprintManager = FingerprintManagerCompat.from(getReactApplicationContext());

            if (fingerprintManager == null) {
                return false;
            }

            return fingerprintManager.isHardwareDetected();
        }

    }

    @Override
    public String getName() {
        return "RNFingerprintChange";
    }


    private void createKeyWithHandler() {
        // This avoids on Samsung Galaxy Note 8, this bug:
        // java.lang.RuntimeException: Can't create handler inside thread that has not called Looper.prepare()
        // used to happen on "createKey(...)"
        HandlerThread handlerThread = new HandlerThread("FingerPrintKeysHandlerThread");
        handlerThread.start();
        Looper looper = handlerThread.getLooper();
        Handler handler = new Handler(looper);

        handler.post(new Runnable() {
            @Override
            public void run() {
                createKey(DEFAULT_KEY_NAME, true);
            }
        });

        handlerThread.quit();
    }

    @TargetApi(Build.VERSION_CODES.M)
    private boolean initCipher(Cipher cipher, String keyName) {
        try {
            mKeyStore.load(null);
            SecretKey key = (SecretKey) mKeyStore.getKey(keyName, null);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } //catch (KeyStoreException | CertificateException | UnrecoverableKeyException | IOException
        //      | NoSuchAlgorithmException | InvalidKeyException e) {
        //  e.printStackTrace();
        //  return false;
        //}
    }


    /**
     * Creates a symmetric key in the Android Key Store which can only be used after the user has
     * authenticated with fingerprint.
     *
     * @param keyName                          the name of the key to be created
     * @param invalidatedByBiometricEnrollment if {@code false} is passed, the created key will not
     *                                         be invalidated even if a new fingerprint is enrolled.
     *                                         The default value is {@code true}, so passing
     *                                         {@code true} doesn't change the behavior
     *                                         (the key will be invalidated if a new fingerprint is
     *                                         enrolled.). Note that this parameter is only valid if
     *                                         the app works on Android N developer preview.
     */
    @TargetApi(Build.VERSION_CODES.M)
    public void createKey(String keyName, boolean invalidatedByBiometricEnrollment) {


        try {
            mKeyStore.load(null);
            // Set the alias of the entry in Android KeyStore where the key will appear
            // and the constrains (purposes) in the constructor of the Builder

            KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(keyName,
                    KeyProperties.PURPOSE_ENCRYPT |
                            KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    // Require the user to authenticate with a fingerprint to authorize every use
                    // of the key
                    .setUserAuthenticationRequired(true)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7);

            // This is a workaround to avoid crashes on devices whose API level is < 24
            // because KeyGenParameterSpec.Builder#setInvalidatedByBiometricEnrollment is only
            // visible on API level +24.
            // Ideally there should be a compat library for KeyGenParameterSpec.Builder but
            // which isn't available yet.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                builder.setInvalidatedByBiometricEnrollment(invalidatedByBiometricEnrollment);
            }
            mKeyGenerator.init(builder.build());
            mKeyGenerator.generateKey();
        }         // SemIrisManager.java line 948 BUG
        // https://developer.samsung.com/forum/board/thread/view.do?boardName=SDK&messageId=335026&startId=zzzzz~
        catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().equals("Can't create handler inside thread that has not called Looper.prepare()")) {
                // failedOnNote8 = true;
            }
        }

    }


}