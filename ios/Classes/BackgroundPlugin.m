#import "BackgroundPlugin.h"
#if __has_include(<background/background-Swift.h>)
#import <background/background-Swift.h>
#else
// Support project import fallback if the generated compatibility header
// is not copied when this plugin is created as a library.
// https://forums.swift.org/t/swift-static-libraries-dont-copy-generated-objective-c-header/19816
#import "background-Swift.h"
#endif

@implementation BackgroundPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftBackgroundPlugin registerWithRegistrar:registrar];
}
@end
