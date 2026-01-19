/*
 * Copyright (c) 2024 Community Cordova Plugin Authors
 * Licensed under the MIT License
 *
 * Based on cordova-plugin-printer by appPlant GmbH
 */

var exec = require('cordova/exec');

/**
 * The Printer plugin provides the ability to print content.
 */
var Printer = {

    /**
     * Check if printing is available and get list of printers.
     *
     * @param {Function} callback Callback function with (available, printers) parameters.
     * @param {Object} [options] Optional settings.
     */
    check: function(callback, options) {
        var fn = function(result) {
            if (callback) {
                callback(result.avail, result.printers || []);
            }
        };

        exec(fn, null, 'Printer', 'check', [options || {}]);
    },

    /**
     * Get the list of supported content types.
     *
     * @param {Function} callback Callback function with (types) parameter.
     */
    types: function(callback) {
        exec(callback, null, 'Printer', 'types', []);
    },

    /**
     * Pick a printer (iOS only).
     *
     * @param {Function} callback Callback function with (printerId) parameter.
     * @param {Object} [options] Optional settings.
     */
    pick: function(callback, options) {
        exec(callback, null, 'Printer', 'pick', [options || {}]);
    },

    /**
     * Print content.
     *
     * @param {String} content The content to print (HTML, file path, base64).
     * @param {Object} [options] Print options.
     * @param {Function} [callback] Callback function with (success) parameter.
     */
    print: function(content, options, callback) {
        // Handle optional parameters
        if (typeof options === 'function') {
            callback = options;
            options = {};
        }

        options = options || {};
        options.content = content;

        var fn = function(success) {
            if (callback) {
                callback(success);
            }
        };

        exec(fn, fn, 'Printer', 'print', [options]);
    },

    /**
     * Check if the device is capable of printing.
     *
     * @param {Function} callback Callback function with (capable) parameter.
     */
    isAvailable: function(callback) {
        this.check(function(available) {
            if (callback) {
                callback(available);
            }
        });
    }
};

module.exports = Printer;
