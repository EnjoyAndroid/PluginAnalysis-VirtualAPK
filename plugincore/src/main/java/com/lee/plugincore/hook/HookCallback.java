package com.lee.plugincore.hook;

import android.os.Handler;
import android.os.Message;

/**
 * Description：
 * Author：Lee
 * Date：2017/8/4 11:35
 */

public class HookCallback implements Handler.Callback {

    public static final int LAUNCH_ACTIVITY = 100;

    @Override
    public boolean handleMessage(Message msg) {
        return false;
    }
}
