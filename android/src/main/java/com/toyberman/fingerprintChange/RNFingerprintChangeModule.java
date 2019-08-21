package com.toyberman.fingerprintChange;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.RequiresApi;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

public class RNFingerprintChangeModule extends ReactContextBaseJavaModule {

    private final ReactApplicationContext reactContext;
    private final String LAST_KEY_ID = "LAST_KEY_ID";
    private SharedPreferences spref;

    public RNFingerprintChangeModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;

        spref = PreferenceManager.getDefaultSharedPreferences(reactContext);
    }

    /**
     * Using reflection to access the fingerprint internal api.
     *
     * @param context
     * @return
     * @throws NoSuchMethodException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws ClassNotFoundException
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    private int getFingerprintInfo(Context context) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, ClassNotFoundException {
        FingerprintManager fingerprintManager = (FingerprintManager) context.getSystemService(Context.FINGERPRINT_SERVICE);
        Method method = FingerprintManager.class.getDeclaredMethod("getEnrolledFingerprints");
        Object obj = method.invoke(fingerprintManager);

        int fingerprintsIdSum = 0;
        if (obj != null) {
            Class<?> clazz = Class.forName("android.hardware.fingerprint.Fingerprint");
            Method getFingerId = clazz.getDeclaredMethod("getFingerId");

            for (int i = 0; i < ((List) obj).size(); i++) {
                Object item = ((List) obj).get(i);
                if (item != null) {
                    fingerprintsIdSum += (int) getFingerId.invoke(item);
                }
            }

        }

        return fingerprintsIdSum;

    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @ReactMethod
    public void hasFingerPrintChanged(Callback errorCallback, Callback successCallback) {

        // if no fingerprint hardware return false
        if (!hasFingerprintHardware(this.reactContext)) {
            successCallback.invoke(false);
            return;
        }

        try {
            // get current fingers id sum
            int fingersId = getFingerprintInfo(this.reactContext);
            // last saved key
            int lastKeyId = spref.getInt(LAST_KEY_ID, -1);
            if (lastKeyId != fingersId && lastKeyId!= -1) {
                successCallback.invoke(true);
            } else {
                successCallback.invoke(false);
            }
            if(lastKeyId==-1)
             spref.edit().putInt(LAST_KEY_ID, fingersId).apply();
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | ClassNotFoundException e) {
            successCallback.invoke(false);
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

}
