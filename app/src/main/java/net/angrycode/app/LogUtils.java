package net.angrycode.app;

import android.util.Log;

/**
 * Created by wecodexyz on 2016/12/23.
 */

public class LogUtils {
    private static final String TAG = LogUtils.class.getSimpleName();
    public static void d(String message) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, message);
        }
    }

    public static void e(String message) {
        if (BuildConfig.DEBUG) {
            Log.e(TAG, message);
        }
    }
}
