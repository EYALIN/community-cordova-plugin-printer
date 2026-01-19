/*
 * Licensed under MIT License
 */

#import <UIKit/UIKit.h>
#import <Cordova/CDVPlugin.h>

@interface PrinterPlugin : CDVPlugin <UIPrintInteractionControllerDelegate>

- (void) check:(CDVInvokedUrlCommand *)command;
- (void) types:(CDVInvokedUrlCommand *)command;
- (void) pick:(CDVInvokedUrlCommand *)command;
- (void) print:(CDVInvokedUrlCommand *)command;

@end
