/*
 * Copyright (c) 2024 Community Cordova Plugin Authors
 * Licensed under the MIT License
 *
 * TypeScript definitions for community-cordova-plugin-printer
 */

/**
 * Paper size units
 */
export type PaperUnit = 'mm' | 'in' | 'pt' | 'mil';

/**
 * Paper size dimension
 */
export interface PaperDimension {
    size: number;
    unit?: PaperUnit;
}

/**
 * Paper size configuration
 */
export interface PaperSize {
    /** Named paper size: A3, A4, A5, A6, Letter, Legal, Tabloid */
    name?: string;
    /** Custom paper width */
    width?: PaperDimension;
    /** Custom paper height */
    height?: PaperDimension;
}

/**
 * Print options
 */
export interface PrintOptions {
    /** The name of the print job */
    name?: string;
    /** Enable double-sided printing */
    duplex?: boolean;
    /** Print in landscape orientation */
    landscape?: boolean;
    /** Print in grayscale */
    grayscale?: boolean;
    /** Number of copies to print */
    copies?: number;
    /** Printer ID to use */
    printer?: string;
    /** Paper size configuration */
    paper?: PaperSize | string;
}

/**
 * Check options
 */
export interface CheckOptions {
    /** Printer ID to check availability for */
    printer?: string;
}

/**
 * Pick options (iOS only)
 */
export interface PickOptions {
    /** Anchor element bounds for iPad popover */
    bounds?: number[];
}

/**
 * Print result status
 */
export type PrintResult = 'completed' | 'cancelled' | 'failed';

/**
 * Printer plugin manager
 */
export default class PrinterManager {
    /**
     * Check if printing is available and get list of printers.
     * @param callback Callback with availability status and printer list
     * @param options Optional check options
     */
    check(callback: (available: boolean, printers: string[]) => void, options?: CheckOptions): void;

    /**
     * Get the list of supported content types.
     * @param callback Callback with array of supported UTIs
     */
    types(callback: (types: string[]) => void): void;

    /**
     * Pick a printer (iOS only).
     * @param callback Callback with selected printer ID or null
     * @param options Optional pick options
     */
    pick(callback: (printerId: string | null) => void, options?: PickOptions): void;

    /**
     * Print content.
     * @param content The content to print (HTML, file path, or base64)
     * @param options Print options
     * @param callback Optional callback with result: 'completed', 'cancelled', or 'failed'
     */
    print(content: string, options?: PrintOptions, callback?: (result: PrintResult) => void): void;

    /**
     * Print content.
     * @param content The content to print (HTML, file path, or base64)
     * @param callback Callback with result: 'completed', 'cancelled', or 'failed'
     */
    print(content: string, callback: (result: PrintResult) => void): void;

    /**
     * Check if the device is capable of printing.
     * @param callback Callback with capability status
     */
    isAvailable(callback: (capable: boolean) => void): void;
}
