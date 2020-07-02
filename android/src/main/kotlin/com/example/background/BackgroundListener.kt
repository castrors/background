package com.example.background

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Pair
import com.example.background.HandleStorage.getMessageHandle
import com.example.background.HandleStorage.getSetupHandle
import com.example.background.HandleStorage.hasSetupHandle
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.PluginRegistry.PluginRegistrantCallback
import io.flutter.view.FlutterCallbackInformation
import io.flutter.view.FlutterMain
import io.flutter.view.FlutterNativeView
import io.flutter.view.FlutterRunArguments
import org.json.JSONObject
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean

@Suppress("SpellCheckingInspection")
internal object BackgroundListener {

    private const val TAG = "Background"
    private var backgroundFlutterView: FlutterNativeView? = null
    private var pluginRegistrantCallback: PluginRegistrantCallback? = null
    private val isIsolateRunning = AtomicBoolean(false)
    private var backgroundMessageHandle: Long? = null
    private val backgroundMessageQueue = Collections.synchronizedList(LinkedList<String>())
    private var backgroundChannel: MethodChannel? = null
    private var result: MethodChannel.Result? = null
//    private var engine: FlutterEngine? = null

    /**
     * In order to be able to use the library callbacks, you need to override the application class and make a custom one.
     * 1. Create a class called `App` in the root of `android/app/packageName`,
     * extend `io.flutter.app.FlutterApplication` and implement `io.flutter.plugin.common.PluginRegistry.PluginRegistrantCallback`.
     * 2. Introduce it to the `AndroidManifest.xml` located at, `android/app/src/main/` as `android:name=".App" (or the full packageName of the class)
     * 3. Add `PushePlugin.initialize(this)` in `onCreate` method of the app class (override it if it does not exist).
     * 4. Add `PushePlugin.registerWith(registry);` in the `registerWith` method (override it if it does not exist).
     * @param initializer is the instance of your app which follows the extending and implementing rule above.
     * @param <T> forces the instance to firstly, extend the FlutterApplication (Context) and secondly, implement PluginRegistrantCallback
     * 5. You're good at native side, you may follow the rest due to [ https://docs.pushe.co ]
     */
    fun <T> initialize(initializer: T) where T : Context, T : PluginRegistrantCallback {
        setPluginRegistrant(initializer)
        setBackgroundCallbacks(initializer)
    }

    /**
     * Initializes native sdk listeners.
     * SDK will call these callbacks and plugin will handle the sending stuff.
     */
    fun setBackgroundCallbacks(context: Context) {
        val c = context.applicationContext
        FlutterMain.ensureInitializationComplete(c, null)
        if (hasSetupHandle(context)) {
            startBackgroundIsolate(c, getSetupHandle(c))
        }
    }

    /**
     * Called when platform is ready and there is an isolate to run dart code, so any waiting message will run instantly.
     * It will iterate over the wating messages and execute them one by one.
     */
    fun onInitialized(context: Context) {
        Log.w(TAG, "Plugin initialized. Platform isolate is running")
        isIsolateRunning.set(true)
        synchronized(backgroundMessageQueue) {
            if (backgroundMessageQueue.isEmpty()) {
                return@synchronized
            }
            Log.w(TAG,"Iterating over pending messages to execute")
            for (s in backgroundMessageQueue) {
                sendBackgroundMessageToExecute(context, s, null)
            }
            backgroundMessageQueue.clear()
        }
    }

    /**
     * If native callbacks were called when app is on the foreground, this function will handle the sending.
     * The method will broadcase an intent to throughout the app and the receiver (PushePlugin itself)
     *    will receive it and send it to the dart side for execution.
     */
    @SafeVarargs
    private fun handleForegroundMessage(context: Context, action: String, vararg data: Pair<String?, String?>) {
        val main = Handler(Looper.getMainLooper())
        main.post {
            FlutterMain.ensureInitializationComplete(context, null)
            val i = Intent(action)
            for (datum in data) {
                i.putExtra(datum.first, datum.second)
            }
            context.sendBroadcast(i)
        }
    }

    /**
     * If native callbacks were called when app is in the background, this function will handle the sending.
     * The method will wake up flutter engine and tries to initialize the dart side by calling the setup method
     *     using its saved handle key. The setup will then call the dart side method which is passed in by the developer.
     */
    private fun handleBackgroundMessage(c: Context, backgroundMessage: String) {
        if (!isIsolateRunning.get()) {
            Log.w(TAG,"Isolate is not running, adding message to queue")
            backgroundMessageQueue.add(backgroundMessage)
        } else {
            Log.w(TAG,"Isolate is running, executing message")
            val latch = CountDownLatch(1)
            sendBackgroundMessageToExecute(c, backgroundMessage, latch)
            try {
                latch.await()
            } catch (ex: InterruptedException) {
                Log.i(TAG, "Exception waiting to execute Dart callback", ex)
            }
        }
    }

    /**
     * Runs the setup callback method in background which will start the Pushe's top level function
     * The top level function will call `onInitialized` of this class and by calling it, isolate will be ready for execution and wating messages will be sent.
     */
    @Suppress("SENSELESS_COMPARISON")
    fun startBackgroundIsolate(context: Context, callbackHandle: Long) {
        FlutterMain.ensureInitializationComplete(context, null)
        val appBundlePath = FlutterMain.findAppBundlePath()
        val flutterCallback = FlutterCallbackInformation.lookupCallbackInformation(callbackHandle)
        if (flutterCallback == null) {
            Log.e(TAG, "Fatal: failed to find callback")
            return
        }

        backgroundFlutterView = FlutterNativeView(context, true)
        if (appBundlePath != null && !isIsolateRunning.get()) {
            if (pluginRegistrantCallback == null) {
                Log.w(TAG,"Fatal: pluginRegistrantCallback is not set. Background callback will not be initialized.\n" +
                        "You must override your application class and call `initialize`." +
                        " Checkout https://docs.pushe.co for more info.")
                return
            }
            val args = FlutterRunArguments()
            args.bundlePath = appBundlePath
            args.entrypoint = flutterCallback.callbackName
            args.libraryPath = flutterCallback.callbackLibraryPath

            if (backgroundFlutterView == null) {
                Log.w(TAG,"Failed to configure native view")
                return
            }

            backgroundFlutterView?.runFromBundle(args)
            pluginRegistrantCallback?.registerWith(backgroundFlutterView?.pluginRegistry)
        }
    }


    /**
     * This function uses a background channel to send native messages to dart side. The dart side setup function will handle the rest.
     * Will be available as soon as the `onInitialized` gets called.
     */
    fun sendBackgroundMessageToExecute(context: Context, message: String, latch: CountDownLatch?) {
        Log.w(TAG,"sendBackgroundMessageToExecute: Sending background message to Dart side")
        if (backgroundChannel == null) {
            Log.e(TAG, "Fatal: background channel is not set. Flutter callback will not be executed.\n" +
                    " This means the PushePlugin is not registered when app is started." +
                    " You must override you application class and call `initialize` in the onCreate." +
                    " Checkout https://docs.pushe.co for more info.")
            return
        }
        // If another thread is waiting, then wake that thread when the callback returns a result.
        if (latch != null) {
            result = LatchResult(latch).result
        }
        val args: MutableMap<String, Any?> = HashMap()
        if (backgroundMessageHandle == null) {
            backgroundMessageHandle = getMessageHandle(context)
        }
        args["handle"] = backgroundMessageHandle
        args["message"] = message
        backgroundChannel?.invokeMethod("handleBackgroundMessage", args, null)
        latch?.countDown()
    }

    /**
     * Initializes background channle (for sending messages to dart side)
     */
    fun setBackgroundChannel(channel: MethodChannel) {
        backgroundChannel = channel
    }

    /**
     * PluginRegistrantCallback is a necessary component for background execution.
     *   It will register the background native view so it will be able to call setup method in the dart side.
     */
    fun setPluginRegistrant(callback: PluginRegistrantCallback) {
        Log.w(TAG,"setPluginRegistrant: pluginRegistrantCallback initialized")
        pluginRegistrantCallback = callback
    }
}