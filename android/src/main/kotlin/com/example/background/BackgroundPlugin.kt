package com.example.background

import android.content.ContentValues.TAG
import android.content.Context
import android.util.Log
import androidx.annotation.NonNull

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry
import io.flutter.plugin.common.PluginRegistry.Registrar
import io.flutter.view.FlutterCallbackInformation
import io.flutter.view.FlutterMain
import io.flutter.view.FlutterNativeView
import io.flutter.view.FlutterRunArguments

/** BackgroundPlugin */
public class BackgroundPlugin : FlutterPlugin {
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        setUpChannel(binding.applicationContext, binding.binaryMessenger)
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {}

    companion object {

        @JvmStatic
        fun <T> initialize(initializer: T) where T : Context, T : PluginRegistry.PluginRegistrantCallback {
            BackgroundListener.initialize(initializer)
        }

        @JvmStatic
        fun registerWith(registry: PluginRegistry) {
            registerWith(registry.registrarFor("com.example.background.BackgroundPlugin"))
        }

        @JvmStatic
        fun registerWith(registrar: Registrar) {
            Log.w(TAG, "registerWith")
            setUpChannel(registrar.context(), registrar.messenger())

        }

        internal fun setUpChannel(context: Context?, messenger: BinaryMessenger?) {
            if (context == null || messenger == null) {
                Log.e("Pushe", "Unhandled exception occurred.\n" +
                        "Either BinaryMessenger or Android Context is null. So plugin can not set MessageHandlers")
                return
            }
            Log.w(TAG, "setUpChannel")
            val channel = MethodChannel(messenger, "flutter_background")
            val callHandler = BackgroundChandler(context, messenger)
            channel.setMethodCallHandler(callHandler)

            // When background was applied
            val backgroundChannel = MethodChannel(messenger, "flutter_background_background")
            backgroundChannel.setMethodCallHandler(callHandler)
            BackgroundListener.setBackgroundChannel(backgroundChannel)
        }

//        fun initialize(context: Context, callback: PluginRegistry.PluginRegistrantCallback) {
//            Log.w(TAG, "initialize")
//            Log.w(TAG, "hasSetupHandle:" + HandleStorage.hasSetupHandle(context))
//            pluginRegistrantCallback = callback
//            if (HandleStorage.hasSetupHandle(context)) {
//                startBackgroundIsolate(context, HandleStorage.getSetupHandle(context))
//            }
//        }
//
//        private fun startBackgroundIsolate(context: Context, callbackHandle: Long) {
//            Log.w(TAG, "startBackgroundIsolate")
//            FlutterMain.ensureInitializationComplete(context, null)
//            val appBundlePath = FlutterMain.findAppBundlePath()
//            val flutterCallback = FlutterCallbackInformation.lookupCallbackInformation(callbackHandle)
//
//            val backgroundFlutterView = FlutterNativeView(context, true)
//            val pluginRegistrantCallback: PluginRegistry.PluginRegistrantCallback? = null
//
//            val args = FlutterRunArguments()
//            args.bundlePath = appBundlePath
//            args.entrypoint = flutterCallback.callbackName
//            args.libraryPath = flutterCallback.callbackLibraryPath
//
//            backgroundFlutterView?.runFromBundle(args)
//
//            pluginRegistrantCallback?.registerWith(backgroundFlutterView?.pluginRegistry)
//        }
//
//        internal fun sendBackgroundMessageToExecute(context: Context) {
//            Log.w(TAG, "sendBackgroundMessageToExecute")
//            Log.w(TAG, "backgroundChannel:$backgroundChannel")
//            val args: MutableMap<String, Any?> = HashMap()
//            if (backgroundMessageHandle == null) {
//                backgroundMessageHandle = HandleStorage.getMessageHandle(context)
//            }
//            args["handle"] = backgroundMessageHandle
//            args["message"] = "after initialize"
//            // The created background channel at step 7
//            backgroundChannel?.invokeMethod("handleBackgroundMessage", args, null)
//        }
    }


//    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
//        channel?.setMethodCallHandler(null)
//    }
//
//    override fun onMethodCall(call: MethodCall, result: Result) {
//        when (call.method) {
//            "handleFunction" -> {
//                val handle: Long = call.argument("handle")!!
//                val setup: Long = call.argument("setup")!!
//                HandleStorage.saveMessageHandle(context, handle)
//                HandleStorage.saveSetupHandle(context, setup)
//                result.success(true)
//            }
//            else -> result.notImplemented()
//        }
//    }


}
