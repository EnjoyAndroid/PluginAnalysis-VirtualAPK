package com.lee.plugincore.hook;

import android.app.Instrumentation;

import com.lee.plugincore.PluginManager;

/**
 * Description：
 * Author：Lee
 * Date：2017/8/4 11:27
 */

public class HookInstrumentation extends Instrumentation {

    private Instrumentation mBase;

    private PluginManager mPluginManager;

    public HookInstrumentation(PluginManager pluginManager, Instrumentation base) {
        this.mPluginManager = pluginManager;
        this.mBase = base;
    }


}
