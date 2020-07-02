package com.example.background

import android.util.Log
import io.flutter.plugin.common.MethodChannel
import java.util.concurrent.CountDownLatch

/**
 * A concurrent handler of the Flutter background isolate.
 */
internal class LatchResult(latch: CountDownLatch) {
    val result: MethodChannel.Result

    init {
        result = object : MethodChannel.Result {
            override fun success(result: Any?) {
                Log.w("LatchResult","MethodChannel result, success")
                latch.countDown()
            }

            override fun error(errorCode: String, errorMessage: String?, errorDetails: Any?) {
                Log.w("LatchResult","MethodChannel result, error")
                latch.countDown()
            }

            override fun notImplemented() {
                Log.w("LatchResult","MethodChannel result, notImplemented")
                latch.countDown()
            }
        }
    }
}