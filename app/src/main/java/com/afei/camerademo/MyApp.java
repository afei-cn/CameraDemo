package com.afei.camerademo;

import android.app.Application;

@SuppressWarnings("SingletonDesignPattern")
public class MyApp extends Application {

    private static MyApp app;

    @Override
    public void onCreate() {
        super.onCreate();
        app = this;
    }

    public static MyApp getInstance() {
        return app;
    }
}
