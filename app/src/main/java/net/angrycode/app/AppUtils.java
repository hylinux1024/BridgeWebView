package net.angrycode.app;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Build;
import android.util.ArrayMap;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by huangyanglin on 2016/12/26.
 */


public class AppUtils {

    public static Activity getCurrActivity(Context context) {
        Activity activity = null;
        if (context instanceof Activity) {
            activity = (Activity) context;
            return activity;
        }
        try {
            Field mBase = ContextWrapper.class.getDeclaredField("mBase");
            mBase.setAccessible(true);
            Object object = mBase.get(context);
            if (object instanceof Activity) {
                activity = (Activity) object;
                return activity;
            }
        } catch (Exception e) {
            LogUtils.e(e.getMessage());
        }
        return activity;
    }

    public static Activity getActivity() {
        try {
            Class activityThreadClass = Class.forName("android.app.ActivityThread");
            Object activityThread = activityThreadClass.getMethod("currentActivityThread").invoke(null);
            Field activitiesField = activityThreadClass.getDeclaredField("mActivities");
            activitiesField.setAccessible(true);
            Map activities = null;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) { // 4.4 以下使用的是 HashMap
                activities = (HashMap) activitiesField.get(activityThread);
            } else { // 4.4 以上使用的是 ArrayMap
                activities = (ArrayMap) activitiesField.get(activityThread);
            }
            for (Object activityRecord : activities.values()) {
                Class activityRecordClass = activityRecord.getClass();
                Field pausedField = activityRecordClass.getDeclaredField("paused"); // 找到 paused 为 false 的activity
                pausedField.setAccessible(true);
                if (!pausedField.getBoolean(activityRecord)) {
                    Field activityField = activityRecordClass.getDeclaredField("activity");
                    activityField.setAccessible(true);
                    Activity activity = (Activity) activityField.get(activityRecord);
                    return activity;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
