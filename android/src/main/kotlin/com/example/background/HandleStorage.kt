package com.example.background

import android.content.Context
import android.util.Log

internal object HandleStorage {
    private const val SHARED_PREFERENCES_KEY = "background_storage"
    private const val BACKGROUND_SETUP_CALLBACK_HANDLE_KEY = "background_background_setup_handle"
    private const val BACKGROUND_MESSAGE_CALLBACK_HANDLE_KEY = "background_background_message_handle"

    @JvmStatic
    fun saveSetupHandle(context: Context, setupHandle: Long) {
        Log.w("HandleStorage", "saveSetupHandle")
        val preferences = context.getSharedPreferences(SHARED_PREFERENCES_KEY, 0x0000)
        preferences.edit().putLong(BACKGROUND_SETUP_CALLBACK_HANDLE_KEY, setupHandle).apply()
    }

    @JvmStatic
    fun saveMessageHandle(context: Context, messageHandle: Long) {
        Log.w("HandleStorage", "saveMessageHandle")
        val preferences = context.getSharedPreferences(SHARED_PREFERENCES_KEY, 0x0000)
        preferences.edit().putLong(BACKGROUND_MESSAGE_CALLBACK_HANDLE_KEY, messageHandle).apply()
    }

    @JvmStatic
    fun getSetupHandle(context: Context): Long {
        val preferences = context.getSharedPreferences(SHARED_PREFERENCES_KEY, 0x0000)
        return preferences.getLong(BACKGROUND_SETUP_CALLBACK_HANDLE_KEY, 0)
    }

    @JvmStatic
    fun getMessageHandle(context: Context): Long {
        val preferences = context.getSharedPreferences(SHARED_PREFERENCES_KEY, 0x0000)
        return preferences.getLong(BACKGROUND_MESSAGE_CALLBACK_HANDLE_KEY, 0)
    }

    @JvmStatic
    fun hasSetupHandle(context: Context): Boolean {
        val preferences = context.getSharedPreferences(SHARED_PREFERENCES_KEY, 0x0000)
        return preferences.contains(BACKGROUND_SETUP_CALLBACK_HANDLE_KEY)
    }

    @JvmStatic
    fun hasMessageHandle(context: Context): Boolean {
        val preferences = context.getSharedPreferences(SHARED_PREFERENCES_KEY, 0x0000)
        return preferences.contains(BACKGROUND_MESSAGE_CALLBACK_HANDLE_KEY)
    }
}