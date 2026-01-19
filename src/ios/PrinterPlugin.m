/*
 * Licensed under MIT License
 */

#import <UIKit/UIKit.h>
#import "PrinterPlugin.h"
#import "PrinterManager.h"

@interface PrinterPlugin ()

@property (nonatomic) UIPrinter *previousPrinter;

@end

@implementation PrinterPlugin

#pragma mark -
#pragma mark Interface

/**
 * Checks if the printing service is available.
 */
- (void) check:(CDVInvokedUrlCommand *)command
{
    [self.commandDelegate runInBackground:^{
        NSDictionary *options = [command argumentAtIndex:0 withDefault:@{}];
        BOOL available = [UIPrintInteractionController isPrintingAvailable];

        NSMutableDictionary *result = [NSMutableDictionary dictionary];
        result[@"avail"] = @(available);
        result[@"printers"] = @[];

        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK
                                                      messageAsDictionary:result];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
}

/**
 * List all printable document types (utis).
 */
- (void) types:(CDVInvokedUrlCommand *)command
{
    [self.commandDelegate runInBackground:^{
        NSSet *utis = UIPrintInteractionController.printableUTIs;

        CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK
                                                     messageAsArray:utis.allObjects];
        [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
    }];
}

/**
 * Displays system interface for selecting a printer.
 */
- (void) pick:(CDVInvokedUrlCommand *)command
{
    [self.commandDelegate runInBackground:^{
        NSMutableDictionary* settings = [[command argumentAtIndex:0 withDefault:@{}] mutableCopy];
        settings[@"callbackId"] = command.callbackId;

        [self presentPickerWithSettings:settings];
    }];
}

/**
 * Sends the printing content to the printer controller and opens them.
 */
- (void) print:(CDVInvokedUrlCommand *)command
{
    [self.commandDelegate runInBackground:^{
        NSDictionary* options = [command argumentAtIndex:0 withDefault:@{}];
        NSString* content = options[@"content"];
        NSMutableDictionary* settings = [options mutableCopy];
        settings[@"callbackId"] = command.callbackId;

        [self printContent:content withSettings:settings];
    }];
}

#pragma mark -
#pragma mark UIPrintInteractionControllerDelegate

/**
 * Asks the delegate for an object encapsulating the paper size and printing area.
 */
- (UIPrintPaper *) printInteractionController:(UIPrintInteractionController *)ctrl
                                  choosePaper:(NSArray *)paperList
{
    NSDictionary *paperSpec = ctrl.printInfo.jobName ? @{} : @{};
    CGSize size = CGSizeMake(612, 792); // Letter size default

    return [UIPrintPaper bestPaperForPageSize:size withPapersFromArray:paperList];
}

#pragma mark -
#pragma mark Core

/**
 * Displays system interface for selecting a printer.
 */
- (void) presentPickerWithSettings:(NSDictionary *)settings
{
    UIPrinterPickerController* controller =
    [UIPrinterPickerController printerPickerControllerWithInitiallySelectedPrinter:nil];

    UIPrinterPickerCompletionHandler handler =
    ^(UIPrinterPickerController *ctrl, BOOL selected, NSError *e) {
        [self returnPickerResultForController:ctrl callbackId:settings[@"callbackId"]];
    };

    dispatch_async(dispatch_get_main_queue(), ^{
        if (UI_USER_INTERFACE_IDIOM() == UIUserInterfaceIdiomPad) {
            CGRect rect = [self rectFromDictionary:settings[@"bounds"]];
            [controller presentFromRect:rect inView:self.webView animated:YES completionHandler:handler];
        } else {
            [controller presentAnimated:YES completionHandler:handler];
        }
    });
}

/**
 * Loads the content into the print controller.
 */
- (void) printContent:(NSString *)content withSettings:(NSDictionary *)settings
{
    __block id item;

    UIPrintInteractionController* ctrl = [UIPrintInteractionController sharedPrintController];
    ctrl.delegate = self;

    // Configure print info
    UIPrintInfo *printInfo = [UIPrintInfo printInfo];
    printInfo.jobName = settings[@"name"] ?: @"Print Job";
    printInfo.duplex = [settings[@"duplex"] boolValue] ? UIPrintInfoDuplexLongEdge : UIPrintInfoDuplexNone;
    printInfo.orientation = [settings[@"landscape"] boolValue] ? UIPrintInfoOrientationLandscape : UIPrintInfoOrientationPortrait;
    printInfo.outputType = [settings[@"grayscale"] boolValue] ? UIPrintInfoOutputGrayscale : UIPrintInfoOutputGeneral;
    ctrl.printInfo = printInfo;

    if ([self strIsNullOrEmpty:content])
    {
        dispatch_sync(dispatch_get_main_queue(), ^{
            item = self.webView.viewPrintFormatter;
        });
    }
    else if ([content hasPrefix:@"<"])
    {
        dispatch_sync(dispatch_get_main_queue(), ^{
            item = [[UIMarkupTextPrintFormatter alloc] initWithMarkupText:content];
        });
    }
    else if ([NSURL URLWithString:content].scheme)
    {
        NSURL *url = [PrinterManager urlFromContent:content];
        if ([UIPrintInteractionController canPrintURL:url]) {
            item = [PrinterManager dataFromURL:url];
        }
    }
    else
    {
        dispatch_sync(dispatch_get_main_queue(), ^{
            item = [[UISimpleTextPrintFormatter alloc] initWithText:content];
        });
    }

    [self useController:ctrl toPrintItem:item withSettings:settings];
}

/**
 * Print the rendered content.
 */
- (void) useController:(UIPrintInteractionController *)ctrl
           toPrintItem:(id)item
          withSettings:(NSDictionary *)settings
{
    NSString* printer = settings[@"printer"];

    if ([item isKindOfClass:UIPrintFormatter.class])
    {
        UIPrintPageRenderer *renderer = [[UIPrintPageRenderer alloc] init];
        [renderer addPrintFormatter:item startingAtPageAtIndex:0];
        ctrl.printPageRenderer = renderer;
    }
    else
    {
        ctrl.printingItem = item;
    }

    if ([self strIsNullOrEmpty:printer])
    {
        [self presentController:ctrl withSettings:settings];
    }
    else
    {
        [self printToPrinter:ctrl withSettings:settings];
    }
}

/**
 * Sends the content directly to the specified printer.
 */
- (void) printToPrinter:(UIPrintInteractionController *)ctrl
          withSettings:(NSDictionary *)settings
{
    NSString* callbackId = settings[@"callbackId"];
    NSString* printerURL = settings[@"printer"];
    UIPrinter* printer = [self printerWithURL:printerURL];

    dispatch_async(dispatch_get_main_queue(), ^{
        [ctrl printToPrinter:printer completionHandler:
         ^(UIPrintInteractionController *ctrl, BOOL ok, NSError *e) {
             [self rememberPrinter:(ok ? printer : NULL)];
             [self sendResultWithMessageAsBool:ok callbackId:callbackId];
         }];
    });
}

/**
 * Opens the print controller so that the user can choose a printer.
 */
- (void) presentController:(UIPrintInteractionController *)ctrl
              withSettings:(NSDictionary *)settings
{
    NSString* callbackId = settings[@"callbackId"];
    CGRect rect = [self rectFromDictionary:settings[@"bounds"]];

    UIPrintInteractionCompletionHandler handler =
    ^(UIPrintInteractionController *ctrl, BOOL ok, NSError *e) {
        [self sendResultWithMessageAsBool:ok callbackId:callbackId];
    };

    dispatch_async(dispatch_get_main_queue(), ^{
        if (UI_USER_INTERFACE_IDIOM() == UIUserInterfaceIdiomPad)
        {
            [ctrl presentFromRect:rect inView:self.webView animated:YES completionHandler:handler];
        }
        else
        {
            [ctrl presentAnimated:YES completionHandler:handler];
        }
    });
}

#pragma mark -
#pragma mark Helper

/**
 * Tells the system to pre-select the given printer next time.
 */
- (void) rememberPrinter:(UIPrinter*)printer
{
    [UIPrinterPickerController printerPickerControllerWithInitiallySelectedPrinter:(_previousPrinter = printer)];
}

/**
 * Returns an object that can be used to connect to the network printer.
 */
- (UIPrinter *)printerWithURL:(NSString *)urlAsString
{
    NSURL* url = [NSURL URLWithString:urlAsString];
    UIPrinter* printer;

    if (_previousPrinter && [_previousPrinter.URL.absoluteString isEqualToString:urlAsString])
    {
        printer = _previousPrinter;
    }
    else
    {
        printer = [UIPrinter printerWithURL:url];
    }

    return printer;
}

/**
 * Convert Dictionary into Rect object.
 */
- (CGRect) rectFromDictionary:(NSDictionary *)pos
{
    CGFloat left = 40, top = 30, width = 0, height = 0;

    if (pos)
    {
        top = [pos[@"top"] floatValue];
        left = [pos[@"left"] floatValue];
        width = [pos[@"width"] floatValue];
        height = [pos[@"height"] floatValue];
    }

    return CGRectMake(left, top, width, height);
}

/**
 * Test if the given string is null or empty.
 */
- (BOOL) strIsNullOrEmpty:(NSString *)string
{
    return [string isEqual:[NSNull null]] || string == nil || string.length == 0;
}

/**
 * Calls the callback function with the result of the selected printer.
 */
- (void) returnPickerResultForController:(UIPrinterPickerController *)ctrl
                              callbackId:(NSString *)callbackId
{
    UIPrinter* printer = ctrl.selectedPrinter;
    CDVPluginResult* result;

    [self rememberPrinter:printer];

    if (printer) {
        result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK
                                   messageAsString:printer.URL.absoluteString];
    } else {
        result = [CDVPluginResult resultWithStatus:CDVCommandStatus_NO_RESULT];
    }

    [self.commandDelegate sendPluginResult:result callbackId:callbackId];
}

/**
 * Sends the plugin result with a boolean argument.
 */
- (void) sendResultWithMessageAsBool:(BOOL)msg callbackId:(NSString *)callbackId
{
    CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsBool:msg];
    [self.commandDelegate sendPluginResult:result callbackId:callbackId];
}

@end
