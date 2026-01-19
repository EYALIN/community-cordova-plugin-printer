# community-cordova-plugin-printer

A Cordova plugin to print documents, images, and web pages from your mobile app.

## Supported Platforms

- Android
- iOS
- Browser

## Installation

```bash
cordova plugin add community-cordova-plugin-printer
```

Or from GitHub:

```bash
cordova plugin add https://github.com/EYALIN/community-cordova-plugin-printer.git
```

## Usage

### Check if Printing is Available

```javascript
cordova.plugins.printer.check((available, printers) => {
  if (available) {
    console.log('Printing is available');
    console.log('Available printers:', printers);
  } else {
    console.log('Printing is not available');
  }
});
```

### Get Supported Content Types

```javascript
cordova.plugins.printer.types((types) => {
  console.log('Supported types:', types);
});
```

### Print Content

#### Print HTML Content

```javascript
cordova.plugins.printer.print('<h1>Hello World</h1>', {
  name: 'My Document'
}, (success) => {
  console.log('Print completed:', success);
});
```

#### Print a PDF File

```javascript
cordova.plugins.printer.print('file:///path/to/document.pdf', {
  name: 'My PDF'
}, (success) => {
  console.log('Print completed:', success);
});
```

#### Print an Image

```javascript
cordova.plugins.printer.print('file:///path/to/image.png', {
  name: 'My Image'
}, (success) => {
  console.log('Print completed:', success);
});
```

### Print Options

| Option | Type | Description |
|--------|------|-------------|
| `name` | string | The name of the print job |
| `duplex` | boolean | Enable double-sided printing |
| `landscape` | boolean | Print in landscape orientation |
| `grayscale` | boolean | Print in grayscale |
| `copies` | number | Number of copies to print |
| `printer` | string | Printer ID to use |
| `paper` | object | Paper size options |

#### Paper Size Options

```javascript
{
  paper: {
    name: 'A4',  // or 'A3', 'A5', 'A6', 'Letter', 'Legal', 'Tabloid'
    width: { size: 8500, unit: 'mil' },  // Custom width
    height: { size: 11000, unit: 'mil' }  // Custom height
  }
}
```

### Pick a Printer (iOS only)

```javascript
cordova.plugins.printer.pick((printerId) => {
  console.log('Selected printer:', printerId);
});
```

## TypeScript Support

TypeScript definitions are included. Import the types:

```typescript
import 'community-cordova-plugin-printer';

declare const cordova: Cordova;

cordova.plugins.printer.print('<h1>Hello</h1>', {
  name: 'Test'
}, (success: boolean) => {
  console.log(success);
});
```

## Angular/Ionic Usage

For Angular and Ionic projects, you can use the provided service wrapper:

```typescript
import { PrinterService } from './services/printer.service';

constructor(private printer: PrinterService) {}

async printDocument() {
  const available = await this.printer.check();
  if (available) {
    const success = await this.printer.print('<h1>Hello</h1>', { name: 'Test' });
    console.log('Print result:', success);
  }
}
```

## Credits

Based on the original [cordova-plugin-printer](https://github.com/niceonedaviddevs/cordova-plugin-printer) by appPlant GmbH.

## License

MIT License

## Support

If you find this plugin helpful, please consider:
- Starring the repository on GitHub
- Reporting issues or suggesting improvements
- Contributing to the codebase
