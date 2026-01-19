/*
 * Copyright (c) 2024 Community Cordova Plugin Authors
 * Licensed under the MIT License
 *
 * Browser platform implementation for Printer plugin
 */

var Printer = {

    check: function(success, error, args) {
        var result = {
            avail: typeof window.print === 'function',
            printers: []
        };
        success(result);
    },

    types: function(success, error, args) {
        success(['text/html']);
    },

    pick: function(success, error, args) {
        success(null);
    },

    print: function(success, error, args) {
        var options = args[0] || {};
        var content = options.content;

        if (!content) {
            success(false);
            return;
        }

        try {
            var printWindow = window.open('', '_blank', 'width=800,height=600');

            if (printWindow) {
                // Check if content is HTML
                if (content.indexOf('<') !== -1) {
                    printWindow.document.write(content);
                } else {
                    printWindow.document.write('<pre>' + content + '</pre>');
                }

                printWindow.document.close();
                printWindow.focus();

                setTimeout(function() {
                    printWindow.print();
                    printWindow.close();
                    success(true);
                }, 250);
            } else {
                // Popup blocked, try iframe approach
                var iframe = document.createElement('iframe');
                iframe.style.display = 'none';
                document.body.appendChild(iframe);

                var doc = iframe.contentWindow.document;
                doc.open();
                doc.write(content);
                doc.close();

                iframe.contentWindow.focus();
                iframe.contentWindow.print();

                setTimeout(function() {
                    document.body.removeChild(iframe);
                    success(true);
                }, 1000);
            }
        } catch (e) {
            console.error('Print error:', e);
            success(false);
        }
    }
};

require('cordova/exec/proxy').add('Printer', Printer);
