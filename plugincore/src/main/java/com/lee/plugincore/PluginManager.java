package com.lee.plugincore;

import android.app.Instrumentation;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Handler;

import com.lee.plugincore.hook.HookCallback;
import com.lee.plugincore.hook.HookInstrumentation;
import com.lee.plugincore.utils.PluginUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Description：
 * Author：Lee
 * Date：2017/8/4 10:12
 */

public class PluginManager {

    private Context mHostContext;
    private static PluginManager sInstance = null;
    private Instrumentation mInstrumentation;
    private Map<String, LoadedPlugin> mPlugins = new ConcurrentHashMap<>();

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

    public Context getHostContext() {
        return this.mHostContext;
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
            mInstrumentation = (Instrumentation) mInstrumentationField.get(sCurrentActivityThreadField);
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

    public LoadedPlugin getLoadedPlugin(Intent intent) {
        ComponentName component = PluginUtil.getComponent(intent);
        return getLoadedPlugin(component.getPackageName());
    }

    public LoadedPlugin getLoadedPlugin(ComponentName component) {
        return this.getLoadedPlugin(component.getPackageName());
    }

    public LoadedPlugin getLoadedPlugin(String packageName) {
        return this.mPlugins.get(packageName);
    }


    public List<LoadedPlugin> getAllLoadedPlugins() {
        List<LoadedPlugin> list = new ArrayList<>();
        list.addAll(mPlugins.values());
        return list;
    }

    public ResolveInfo resolveActivity(Intent intent) {
        return this.resolveActivity(intent, 0);
    }

    public ResolveInfo resolveActivity(Intent intent, int flags) {
        for (LoadedPlugin plugin : this.mPlugins.values()) {
            ResolveInfo resolveInfo = plugin.resolveActivity(intent, flags);
            if (null != resolveInfo) {
                return resolveInfo;
            }
        }

        return null;
    }

    /**
     * used in PluginPackageManager, do not invoke it from outside.
     */
    @Deprecated
    public List<ResolveInfo> queryIntentActivities(Intent intent, int flags) {
        List<ResolveInfo> resolveInfos = new ArrayList<ResolveInfo>();

        for (LoadedPlugin plugin : this.mPlugins.values()) {
            List<ResolveInfo> result = plugin.queryIntentActivities(intent, flags);
            if (null != result && result.size() > 0) {
                resolveInfos.addAll(result);
            }
        }

        return resolveInfos;
    }

    public Instrumentation getInstrumentation() {
        return this.mInstrumentation;
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
