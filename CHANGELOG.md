# Changelog

All notable changes to this project will be documented in this file.

## [1.0.0] - 2026-01-18

### Initial Release

This is the first release of `community-cordova-plugin-printer`, a modernized fork of the original [cordova-plugin-printer](https://github.com/niceonedaviddevs/cordova-plugin-printer) by appPlant GmbH.

### Changes from Original

- **iOS**: Added proper UIKit framework imports for Cordova iOS 8.0.0 compatibility
- **iOS**: Renamed class prefix from APP* to CDV* for consistency
- **Android**: Updated to use AndroidX instead of android.support.* libraries
- **Android**: Updated package namespace to `com.community.cordova.printer`
- Added TypeScript definitions
- Updated plugin structure and documentation

### Features

- Print HTML content
- Print PDF files
- Print images (PNG, JPEG, GIF)
- Print plain text
- Configure print options (duplex, landscape, grayscale, copies)
- Select paper size
- Check printer availability
- Pick printer (iOS)
- Browser platform support

### Supported Platforms

- Android 10.0.0+
- iOS 6.0.0+
- Browser

### Credits

A huge thank you to appPlant GmbH for creating and maintaining the original cordova-plugin-printer plugin.
