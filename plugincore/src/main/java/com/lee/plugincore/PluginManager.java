package com.lee.plugincore;

import android.app.Instrumentation;
import android.content.Context;

import com.lee.plugincore.hook.HookHandler;
import com.lee.plugincore.hook.HookInstrumentation;

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

    public void init() {
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

            // hook mH
            Field mHField = activityThreadClass.getDeclaredField("mH");
            mHField.setAccessible(true);
            mHField.set(sCurrentActivityThread, new HookHandler());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
