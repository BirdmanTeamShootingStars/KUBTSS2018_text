package com.example.hongu.kubtss2018_text;

import android.app.Application;

/**
 * Created by hongu on 2017/09/19.
 */

public class MyApplication extends Application {
    private static MyApplication instance = null;
    @Override
    public void onCreate() {
        super.onCreate();

        instance = this;
    }

    public static MyApplication getInstance() {
        return instance;
    }
}
