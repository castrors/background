package com.example.background_example;

import android.content.Context;

import com.example.background.BackgroundPlugin;

import io.flutter.app.FlutterApplication;
import io.flutter.plugin.common.PluginRegistry;

public class MyApplication extends FlutterApplication implements PluginRegistry.PluginRegistrantCallback {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        BackgroundPlugin.initialize(this);
    }

    @Override
    public void registerWith(io.flutter.plugin.common.PluginRegistry registry) {
        BackgroundPlugin.registerWith(registry);
    }
}