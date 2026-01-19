/*
 * Licensed under MIT License
 */

#import <UIKit/UIKit.h>
#import <Foundation/Foundation.h>

@interface PrinterManager : NSObject

/**
 * Creates a URL from content string.
 */
+ (NSURL *)urlFromContent:(NSString *)content;

/**
 * Loads data from URL.
 */
+ (NSData *)dataFromURL:(NSURL *)url;

/**
 * Checks if URL can be printed.
 */
+ (BOOL)canPrintURL:(NSString *)content;

@end
