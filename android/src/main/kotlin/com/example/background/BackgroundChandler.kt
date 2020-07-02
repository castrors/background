package com.example.background

import android.content.BroadcastReceiver
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import com.example.background.HandleStorage.saveMessageHandle
import com.example.background.HandleStorage.saveSetupHandle
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.view.FlutterMain

/**
 * BackgroundChandler
 *
 * @author Mahdi Malvandi
 */
@Suppress("SpellCheckingInspection")
internal class BackgroundChandler(private val context: Context,
                             private val messenger: BinaryMessenger) : BroadcastReceiver(), MethodCallHandler {

    /**
     * Register a receiver to get Pushe foreground callbacks
     * See `onReceive` at the end of the file
     */
    init {
        val i = IntentFilter()
        i.addAction(context.packageName + ".nr") // Receive
        i.addAction(context.packageName + ".nc") // Click
        i.addAction(context.packageName + ".nbc") // Button click
        i.addAction(context.packageName + ".nd") // Dismiss
        i.addAction(context.packageName + ".nccr") // CustomContent receive
        context.registerReceiver(this, i)
    }

    private val notificationTypes = listOf("IdType.AndroidId", "IdType.GoogleAdvertisingId", "IdType.CustomId")
    private val eventTypes = listOf("EventAction.custom", "EventAction.sign_up", "EventAction.login", "EventAction.purchase", "EventAction.achievement", "EventAction.level")

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        //        val notificationModule = Pushe.getPusheService(PusheNotification::class.java)
//        val analyticsModule = Pushe.getPusheService(PusheAnalytics::class.java)

        when (call.method) {
            "handleFunction" -> {
                val handle: Long = call.argument("handle")!!
                val setup: Long = call.argument("setup")!!
                HandleStorage.saveMessageHandle(context, handle)
                HandleStorage.saveSetupHandle(context, setup)
                result.success(true)
            }
//            "Pushe.initialize" -> Pushe.initialize()
//            "Pushe.setUserConsentGiven" -> setUserConsentGiven(call, result)
//            "Pushe.getUserConsentStatus" -> result.success(Pushe.getUserConsentStatus())
//            "Pushe.getDeviceId" -> result.success(Pushe.getDeviceId())
//            "Pushe.getAndroidId" -> result.success(Pushe.getAndroidId())
//            "Pushe.getGoogleAdvertisingId" -> result.success(Pushe.getGoogleAdvertisingId())
//            "Pushe.getCustomId" -> result.success(Pushe.getCustomId())
//            "Pushe.setCustomId" -> setCustomId(call, result)
//            "Pushe.getUserEmail" -> result.success(Pushe.getUserEmail())
//            "Pushe.setUserEmail" -> setUserEmail(call, result)
//            "Pushe.getUserPhoneNumber" -> result.success(Pushe.getUserPhoneNumber())
//            "Pushe.setUserPhoneNumber" -> setUserPhoneNumber(call, result)
//            "Pushe.subscribe" -> subscribeToTopic(call, result)
//            "Pushe.unsubscribe" -> unsubscribeFromTopic(call, result)
//            "Pushe.enableNotifications" -> setNotificationEnabled(true, result, notificationModule)
//            "Pushe.disableNotifications" -> setNotificationEnabled(false, result, notificationModule)
//            "Pushe.enableCustomSound" -> setCustomSoundEnabled(true, result, notificationModule)
//            "Pushe.disableCustomSound" -> setCustomSoundEnabled(false, result, notificationModule)
//            "Pushe.isNotificationEnable" -> isNotificationEnabled(result, notificationModule)
//            "Pushe.isCustomSoundEnabled" -> isCustomSoundEnabled(result, notificationModule)
//            "Pushe.createNotificationChannel" -> createNotificationChannel(call, result, notificationModule)
//            "Pushe.removeNotificationChannel" -> removeNotificationChannel(call, result, notificationModule)
//            "Pushe.isInitialized" -> result.success(Pushe.isInitialized())
//            "Pushe.isRegistered" -> result.success(Pushe.isRegistered())
//            "Pushe.sendUserNotification" -> sendUserNotification(call, result, notificationModule)
//            "Pushe.sendAdvancedUserNotification" -> sendAdvancedNotification(call, result, notificationModule)
//            "Pushe.sendEvent" -> sendEvent(call, result, analyticsModule)
//            "Pushe.sendEcommerceData" -> sendEcommerceData(call, result, analyticsModule)
//            "Pushe.initNotificationListenerManually" -> initializeForegroundNotifications()
//            "Pushe.setRegistrationCompleteListener" -> Pushe.setRegistrationCompleteListener {
//                result.success(true)
//            }
//            "Pushe.setInitializationCompleteListener" -> Pushe.setInitializationCompleteListener {
//                result.success(true)
//            }
//            "Pushe.addTags" -> addTag(call, result)
//            "Pushe.removeTags" -> removeTags(call, result)
//            "Pushe.getSubscribedTags" -> result.success(Pushe.getSubscribedTags())
//            "Pushe.getSubscribedTopics" -> result.success(Pushe.getSubscribedTopics())
            "Background.notificationListener" -> setNotificationListeners(call, result)
            "Background.platformInitialized" -> initializeListenerPlatform(result)
            else -> result.notImplemented()
        }
    }

    ///// Do stuff when a method was called



    private fun initializeListenerPlatform(result: MethodChannel.Result) {
        BackgroundListener.onInitialized(context)
        result.success(true)
    }


    private fun setNotificationListeners(call: MethodCall, result: MethodChannel.Result) {
        var setupCallbackHandle: Long = 0
        var backgroundMessageHandle: Long = 0
        try {
            val setup = call.argument<Any>("setupHandle")
            val background = call.argument<Any>("backgroundHandle")
            setupCallbackHandle = if (setup is Int) {
                setup.toLong()
            } else {
                setup as Long
            }
            backgroundMessageHandle = if (background is Int) {
                background.toLong()
            } else {
                background as Long
            }
        } catch (e: Exception) {
            Log.w(TAG,"There was an exception when getting callback handle from Dart side")
            e.printStackTrace()
        }
        saveSetupHandle(context, setupCallbackHandle)
        BackgroundListener.startBackgroundIsolate(context, setupCallbackHandle)
        saveMessageHandle(context, backgroundMessageHandle)
        result.success(true)
    }

//    private fun initializeForegroundNotifications() {
//        BackgroundListener.setNotificationCallbacks(context.applicationContext)
//    }

    ///// Do stuff when a callback was received

    /**
     * Handle foreground notification callbacks
     * Callback are broadcasted, so this will receive the broadcast and send the parsed intent data as json to the dart side.
     */
    override fun onReceive(context: Context, intent: Intent) {
        val channel = MethodChannel(messenger, "plus.pushe.co/pushe_flutter")
        FlutterMain.ensureInitializationComplete(context, null)
        val action = if (intent.action == null) "" else intent.action
        if (action.isEmpty()) {
            return
        }
        Log.w(TAG, action)

//        when (action) {
//            context.packageName + ".nr" -> {
//                channel.invokeMethod("Pushe.onNotificationReceived", getNotificationJsonFromIntent(intent).toString())
//            }
//            context.packageName + ".nc" -> {
//                channel.invokeMethod("Pushe.onNotificationClicked", getNotificationJsonFromIntent(intent).toString())
//            }
//            context.packageName + ".nbc" -> {
//                channel.invokeMethod("Pushe.onNotificationButtonClicked", getNotificationJsonFromIntent(intent).toString())
//            }
//            context.packageName + ".nccr" -> {
//                channel.invokeMethod("Pushe.onCustomContentReceived", getCustomContentFromIntent(intent).toString())
//            }
//            context.packageName + ".nd" -> {
//                channel.invokeMethod("Pushe.onNotificationDismissed", getNotificationJsonFromIntent(intent).toString())
//            }
//        }
    }

    ///// Utils

    /**
     * Check that if the the MethodCall which was received from dart side, contains all the keys in the arguement.
     */
    private fun MethodCall.hasAll(vararg keys: String): Boolean {
        var hasAll = true
        keys.forEach {
            if (!hasArgument(it)) {
                hasAll = false
                return@forEach
            }
        }
        return hasAll
    }


}