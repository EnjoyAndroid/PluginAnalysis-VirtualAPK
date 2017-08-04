package com.lee.plugincore;

import android.app.Application;
import android.content.Context;

/**
 * Description：
 * Author：Lee
 * Date：2017/8/4 9:42
 */

public class CoreApp extends Application {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        PluginManager.getInstance(base).hook();
    }
}
