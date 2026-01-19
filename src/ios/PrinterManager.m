/*
 * Licensed under MIT License
 */

#import <UIKit/UIKit.h>
#import "PrinterManager.h"

@implementation PrinterManager

/**
 * Creates a URL from content string.
 */
+ (NSURL *)urlFromContent:(NSString *)content
{
    if (!content || content.length == 0) {
        return nil;
    }

    // Handle base64 data
    if ([content hasPrefix:@"base64:"]) {
        return nil; // Base64 handled separately
    }

    // Handle file:// URLs
    if ([content hasPrefix:@"file://"]) {
        return [NSURL URLWithString:content];
    }

    // Handle http/https URLs
    if ([content hasPrefix:@"http://"] || [content hasPrefix:@"https://"]) {
        return [NSURL URLWithString:content];
    }

    // Handle asset paths
    if ([content hasPrefix:@"www/"]) {
        NSString *wwwPath = [[NSBundle mainBundle] pathForResource:@"www" ofType:nil];
        NSString *filePath = [content stringByReplacingOccurrencesOfString:@"www/" withString:@""];
        NSString *fullPath = [wwwPath stringByAppendingPathComponent:filePath];
        return [NSURL fileURLWithPath:fullPath];
    }

    // Handle res:// protocol
    if ([content hasPrefix:@"res://"]) {
        NSString *resourcePath = [content stringByReplacingOccurrencesOfString:@"res://" withString:@""];
        NSString *path = [[NSBundle mainBundle] pathForResource:resourcePath ofType:nil];
        if (path) {
            return [NSURL fileURLWithPath:path];
        }
    }

    // Try as file path
    if ([[NSFileManager defaultManager] fileExistsAtPath:content]) {
        return [NSURL fileURLWithPath:content];
    }

    return [NSURL URLWithString:content];
}

/**
 * Loads data from URL.
 */
+ (NSData *)dataFromURL:(NSURL *)url
{
    if (!url) {
        return nil;
    }

    if ([url isFileURL]) {
        return [NSData dataWithContentsOfURL:url];
    }

    // For remote URLs, load synchronously (should be called from background thread)
    NSURLRequest *request = [NSURLRequest requestWithURL:url
                                             cachePolicy:NSURLRequestReloadIgnoringLocalCacheData
                                         timeoutInterval:30];

    __block NSData *data = nil;
    dispatch_semaphore_t semaphore = dispatch_semaphore_create(0);

    NSURLSessionDataTask *task = [[NSURLSession sharedSession] dataTaskWithRequest:request
                                                                 completionHandler:^(NSData *responseData, NSURLResponse *response, NSError *error) {
        if (!error && responseData) {
            data = responseData;
        }
        dispatch_semaphore_signal(semaphore);
    }];

    [task resume];
    dispatch_semaphore_wait(semaphore, DISPATCH_TIME_FOREVER);

    return data;
}

/**
 * Checks if URL can be printed.
 */
+ (BOOL)canPrintURL:(NSString *)content
{
    if (!content || content.length == 0) {
        return YES; // Will print current webview
    }

    // HTML content can always be printed
    if ([content hasPrefix:@"<"]) {
        return YES;
    }

    // Check if URL content type is printable
    NSURL *url = [self urlFromContent:content];
    if (url) {
        return [UIPrintInteractionController canPrintURL:url];
    }

    // Plain text can always be printed
    return YES;
}

@end
