import 'dart:async';
import 'dart:ui';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

void _setup() async {
  print('call setup');
  MethodChannel backgroundChannel =
      const MethodChannel('flutter_background_background');
  // Setup Flutter state needed for MethodChannels.
  WidgetsFlutterBinding.ensureInitialized();

  // This is where the magic happens and we handle background events from the
  // native portion of the plugin.
  backgroundChannel.setMethodCallHandler((MethodCall call) async {
    print("setup handleBackgroundMessage");
    if (call.method == 'handleBackgroundMessage') {
      final CallbackHandle handle =
          CallbackHandle.fromRawHandle(call.arguments['handle']);
      final Function handlerFunction =
          PluginUtilities.getCallbackFromHandle(handle);
      try {
        var dataArg = call.arguments['message'];
        if (dataArg == null) {
          print('Data received from callback is null');
          return;
        }
        await handlerFunction(dataArg);
      } catch (e) {
        print('Unable to handle incoming background message.\n$e');
      }
    }
    return Future.value();
  });
}

_bgFunction(dynamic message) {
  print('Background response $message');
  // Message received in background
  // Remember, this will be a different isolate. So, no widgets
}

class Background {
  static const MethodChannel _channel =
      const MethodChannel('flutter_background');

  static Future<bool> get handleFunction async {
    CallbackHandle setup = PluginUtilities.getCallbackHandle(_setup);
    CallbackHandle handle = PluginUtilities.getCallbackHandle(_bgFunction);

    return _channel.invokeMethod<bool>(
      'handleFunction',
      <String, dynamic>{
        'handle': handle.toRawHandle(),
        'setup': setup.toRawHandle()
      },
    );
  }
}
