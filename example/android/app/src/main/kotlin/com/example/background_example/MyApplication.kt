package com.example.background_example

import com.example.background.BackgroundPlugin
import io.flutter.app.FlutterApplication
import io.flutter.plugin.common.PluginRegistry

class MyApplication : FlutterApplication(), PluginRegistry.PluginRegistrantCallback {

    override fun onCreate() {
        super.onCreate()
        BackgroundPlugin.initialize(this, this)
    }

    override fun registerWith(registry: PluginRegistry) {
        BackgroundPlugin.registerWith(registry.registrarFor("com.example.background.BackgroundPlugin"))
    }

}