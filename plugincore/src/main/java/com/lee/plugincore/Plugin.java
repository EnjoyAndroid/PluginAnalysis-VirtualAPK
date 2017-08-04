package com.lee.plugincore;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.InstrumentationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageParser;
import android.content.pm.PermissionInfo;
import android.content.pm.ProviderInfo;
import android.content.pm.ServiceInfo;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.util.Log;

import com.lee.plugincore.utils.Constants;
import com.lee.plugincore.utils.DexUtil;
import com.lee.plugincore.utils.PackageParserCompat;
import com.lee.plugincore.utils.ReflectUtil;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import dalvik.system.DexClassLoader;

/**
 * Description：
 * Author：Lee
 * Date：2017/8/4 15:52
 */

public class Plugin {

    private final static String TAG = "Plugin";

    private final String mLocation;
    private PluginManager mPluginManager;
    private Context mHostContext;
    private Context mPluginContext;
    private final File mNativeLibDir;
    private final PackageParser.Package mPackage;
    private final PackageInfo mPackageInfo;
    private AssetManager mAssets;
    private Resources mResources;
    private ClassLoader mClassLoader;
//    private PluginPackageManager mPackageManager;

    private Map<ComponentName, ActivityInfo> mActivityInfos;
    private Map<ComponentName, ServiceInfo> mServiceInfos;
    private Map<ComponentName, ActivityInfo> mReceiverInfos;
    private Map<ComponentName, ProviderInfo> mProviderInfos;
    private Map<String, ProviderInfo> mProviders; // key is authorities of provider
    private Map<ComponentName, InstrumentationInfo> mInstrumentationInfos;

    private Application mApplication;

    Plugin(PluginManager pluginManager, Context context, File apk) throws Exception {
        this.mPluginManager = pluginManager;
        this.mHostContext = context;
        this.mLocation = apk.getAbsolutePath();

        this.mPackage = PackageParserCompat.parsePackage(context, apk, PackageParser.PARSE_MUST_BE_APK);
        Log.i(TAG, "mPackage: " + mPackage.toString());
        this.mPackage.applicationInfo.metaData = this.mPackage.mAppMetaData;
        this.mPackageInfo = new PackageInfo();
        this.mPackageInfo.applicationInfo = this.mPackage.applicationInfo;
        this.mPackageInfo.applicationInfo.sourceDir = apk.getAbsolutePath();
        this.mPackageInfo.signatures = this.mPackage.mSignatures;
        this.mPackageInfo.packageName = this.mPackage.packageName;
        this.mPackageInfo.versionCode = this.mPackage.mVersionCode;
        this.mPackageInfo.versionName = this.mPackage.mVersionName;
        this.mPackageInfo.permissions = new PermissionInfo[0];
//        this.mPackageManager = new LoadedPlugin.PluginPackageManager();
//        this.mPluginContext = new PluginContext(this);
        this.mNativeLibDir = context.getDir(Constants.NATIVE_DIR, Context.MODE_PRIVATE);
//        this.mResources = createResources(context, apk);
        this.mAssets = this.mResources.getAssets();
        this.mClassLoader = createClassLoader(context, apk, this.mNativeLibDir, context.getClassLoader());

//        tryToCopyNativeLib(apk);

        // Cache instrumentations
        Map<ComponentName, InstrumentationInfo> instrumentations = new HashMap<ComponentName, InstrumentationInfo>();
        for (PackageParser.Instrumentation instrumentation : this.mPackage.instrumentation) {
            instrumentations.put(instrumentation.getComponentName(), instrumentation.info);
        }
        this.mInstrumentationInfos = Collections.unmodifiableMap(instrumentations);
        this.mPackageInfo.instrumentation = instrumentations.values().toArray(new InstrumentationInfo[instrumentations.size()]);

        // Cache activities
        Map<ComponentName, ActivityInfo> activityInfos = new HashMap<ComponentName, ActivityInfo>();
        for (PackageParser.Activity activity : this.mPackage.activities) {
            activityInfos.put(activity.getComponentName(), activity.info);
        }
        this.mActivityInfos = Collections.unmodifiableMap(activityInfos);
        this.mPackageInfo.activities = activityInfos.values().toArray(new ActivityInfo[activityInfos.size()]);

        // Cache services
        Map<ComponentName, ServiceInfo> serviceInfos = new HashMap<ComponentName, ServiceInfo>();
        for (PackageParser.Service service : this.mPackage.services) {
            serviceInfos.put(service.getComponentName(), service.info);
        }
        this.mServiceInfos = Collections.unmodifiableMap(serviceInfos);
        this.mPackageInfo.services = serviceInfos.values().toArray(new ServiceInfo[serviceInfos.size()]);

        // Cache providers
        Map<String, ProviderInfo> providers = new HashMap<String, ProviderInfo>();
        Map<ComponentName, ProviderInfo> providerInfos = new HashMap<ComponentName, ProviderInfo>();
        for (PackageParser.Provider provider : this.mPackage.providers) {
            providers.put(provider.info.authority, provider.info);
            providerInfos.put(provider.getComponentName(), provider.info);
        }
        this.mProviders = Collections.unmodifiableMap(providers);
        this.mProviderInfos = Collections.unmodifiableMap(providerInfos);
        this.mPackageInfo.providers = providerInfos.values().toArray(new ProviderInfo[providerInfos.size()]);

        // Register broadcast receivers dynamically
        Map<ComponentName, ActivityInfo> receivers = new HashMap<ComponentName, ActivityInfo>();
        for (PackageParser.Activity receiver : this.mPackage.receivers) {
            receivers.put(receiver.getComponentName(), receiver.info);

            try {
                BroadcastReceiver br = BroadcastReceiver.class.cast(Plugin.class.getClassLoader().loadClass(receiver.getComponentName().getClassName()).newInstance());
                for (PackageParser.ActivityIntentInfo aii : receiver.intents) {
                    this.mHostContext.registerReceiver(br, aii);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        this.mReceiverInfos = Collections.unmodifiableMap(receivers);
        this.mPackageInfo.receivers = receivers.values().toArray(new ActivityInfo[receivers.size()]);
    }


    private static ClassLoader createClassLoader(Context context, File apk, File libsDir, ClassLoader parent) {
        File dexOutputDir = context.getDir(Constants.OPTIMIZE_DIR, Context.MODE_PRIVATE);
        String dexOutputPath = dexOutputDir.getAbsolutePath();
        DexClassLoader loader = new DexClassLoader(apk.getAbsolutePath(), dexOutputPath, libsDir.getAbsolutePath(), parent);

        if (Constants.COMBINE_CLASSLOADER) {
            try {
                DexUtil.insertDex(loader);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return loader;
    }

    private static AssetManager createAssetManager(Context context, File apk) {
        try {
            AssetManager am = AssetManager.class.newInstance();
            ReflectUtil.invoke(AssetManager.class, am, "addAssetPath", apk.getAbsolutePath());
            return am;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
