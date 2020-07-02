import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:background/background.dart';

void main() {
  const MethodChannel channel = MethodChannel('background');

  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    channel.setMockMethodCallHandler((MethodCall methodCall) async {
      return '42';
    });
  });

  tearDown(() {
    channel.setMockMethodCallHandler(null);
  });

  test('getPlatformVersion', () async {
    expect(await Background.platformVersion, '42');
  });
}
