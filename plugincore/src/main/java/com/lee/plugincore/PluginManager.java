package com.lee.plugincore;

import android.app.Instrumentation;
import android.content.Context;
import android.os.Handler;

import com.lee.plugincore.hook.HookCallback;
import com.lee.plugincore.hook.HookInstrumentation;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Field;

/**
 * Description：
 * Author：Lee
 * Date：2017/8/4 10:12
 */

public class PluginManager {

    private Context mHostContext;
    private static PluginManager sInstance = null;

    public static PluginManager getInstance(Context context) {
        if (sInstance == null) {
            synchronized (PluginManager.class) {
                if (sInstance == null)
                    sInstance = new PluginManager(context);
            }
        }
        return sInstance;
    }

    private PluginManager(Context context) {
        mHostContext = context;
    }

    public void hook() {
        try {
            //get sCurrentActivityThread
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            Field sCurrentActivityThreadField = activityThreadClass.getDeclaredField("sCurrentActivityThread");
            sCurrentActivityThreadField.setAccessible(true);
            Object sCurrentActivityThread = sCurrentActivityThreadField.get(activityThreadClass);

            // hook Instrumentation
            Field mInstrumentationField = activityThreadClass.getDeclaredField("mInstrumentation");
            mInstrumentationField.setAccessible(true);
            Instrumentation mInstrumentation = (Instrumentation) mInstrumentationField.get(sCurrentActivityThreadField);
            mInstrumentationField.set(sCurrentActivityThread, new HookInstrumentation(this, mInstrumentation));

            // hook mCallback
            Field mHField = activityThreadClass.getDeclaredField("mH");
            mHField.setAccessible(true);
            Handler mH = (Handler) mHField.get(sCurrentActivityThread);
            Field mCallbackField = mH.getClass().getDeclaredField("mCallback");
            mCallbackField.setAccessible(true);
            mCallbackField.set(mH, new HookCallback());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void install(File apk) throws FileNotFoundException {
        if (null == apk) {
            throw new IllegalArgumentException("error : apk is null.");
        }

        if (!apk.exists()) {
            throw new FileNotFoundException(apk.getAbsolutePath());
        }
    }
}
