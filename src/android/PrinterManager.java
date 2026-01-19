/*
 * Licensed under MIT License
 */
package com.community.cordova.printer;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintAttributes.MediaSize;
import android.print.PrintAttributes.Resolution;
import android.print.PrintAttributes.Margins;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.util.Base64;
import android.util.Log;

import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Manager class for handling print operations.
 */
public class PrinterManager {

    private static final String LOG_TAG = "PrinterManager";

    private final Activity activity;
    private final JSONObject options;

    /**
     * Constructor.
     *
     * @param activity The activity context.
     * @param options  Print options from JavaScript.
     */
    public PrinterManager(@NonNull Activity activity, @Nullable JSONObject options) {
        this.activity = activity;
        this.options = options != null ? options : new JSONObject();
    }

    /**
     * Gets the content to print.
     *
     * @return The content string.
     */
    @Nullable
    public String getContent() {
        return options.optString("content", null);
    }

    /**
     * Gets the job name.
     *
     * @return The job name.
     */
    @NonNull
    public String getJobName() {
        return options.optString("name", "Print Job");
    }

    /**
     * Gets the printer ID.
     *
     * @return The printer ID or null.
     */
    @Nullable
    public String getPrinterId() {
        return options.optString("printer", null);
    }

    /**
     * Checks if duplex printing is requested.
     *
     * @return true if duplex.
     */
    public boolean isDuplex() {
        return options.optBoolean("duplex", false);
    }

    /**
     * Checks if landscape orientation is requested.
     *
     * @return true if landscape.
     */
    public boolean isLandscape() {
        return options.optBoolean("landscape", false);
    }

    /**
     * Checks if grayscale is requested.
     *
     * @return true if grayscale.
     */
    public boolean isGrayscale() {
        return options.optBoolean("grayscale", false);
    }

    /**
     * Gets the number of copies.
     *
     * @return The number of copies.
     */
    public int getCopies() {
        return options.optInt("copies", 1);
    }

    /**
     * Converts options to PrintAttributes.Builder.
     *
     * @return The PrintAttributes.Builder.
     */
    @NonNull
    public PrintAttributes.Builder toPrintAttributes() {
        PrintAttributes.Builder builder = new PrintAttributes.Builder();

        // Set media size
        MediaSize mediaSize = getMediaSize();
        if (mediaSize != null) {
            if (isLandscape()) {
                mediaSize = mediaSize.asLandscape();
            } else {
                mediaSize = mediaSize.asPortrait();
            }
            builder.setMediaSize(mediaSize);
        }

        // Set color mode
        if (isGrayscale()) {
            builder.setColorMode(PrintAttributes.COLOR_MODE_MONOCHROME);
        } else {
            builder.setColorMode(PrintAttributes.COLOR_MODE_COLOR);
        }

        // Set duplex mode (API 23+)
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            if (isDuplex()) {
                builder.setDuplexMode(PrintAttributes.DUPLEX_MODE_LONG_EDGE);
            } else {
                builder.setDuplexMode(PrintAttributes.DUPLEX_MODE_NONE);
            }
        }

        // Set resolution
        builder.setResolution(new Resolution("default", "Default", 300, 300));

        // Set margins
        builder.setMinMargins(Margins.NO_MARGINS);

        return builder;
    }

    /**
     * Gets the media size based on options.
     *
     * @return The MediaSize or null.
     */
    @Nullable
    private MediaSize getMediaSize() {
        String paperName = options.optString("paper", null);

        if (paperName != null) {
            switch (paperName.toUpperCase()) {
                case "A3":
                    return MediaSize.ISO_A3;
                case "A4":
                    return MediaSize.ISO_A4;
                case "A5":
                    return MediaSize.ISO_A5;
                case "A6":
                    return MediaSize.ISO_A6;
                case "LETTER":
                    return MediaSize.NA_LETTER;
                case "LEGAL":
                    return MediaSize.NA_LEGAL;
                case "TABLOID":
                    return MediaSize.NA_TABLOID;
            }
        }

        return MediaSize.NA_LETTER;
    }

    /**
     * Creates a print document adapter for the content.
     *
     * @return The print document adapter.
     */
    @NonNull
    public PrintDocumentAdapter createPrintAdapter() {
        return new PrintDocumentAdapter() {
            @Override
            public void onLayout(PrintAttributes oldAttributes, PrintAttributes newAttributes,
                                 CancellationSignal cancellationSignal,
                                 LayoutResultCallback callback, Bundle extras) {

                if (cancellationSignal.isCanceled()) {
                    callback.onLayoutCancelled();
                    return;
                }

                PrintDocumentInfo.Builder builder = new PrintDocumentInfo.Builder(getJobName());
                builder.setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT);
                builder.setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN);

                PrintDocumentInfo info = builder.build();
                callback.onLayoutFinished(info, true);
            }

            @Override
            public void onWrite(PageRange[] pages, ParcelFileDescriptor destination,
                                CancellationSignal cancellationSignal,
                                WriteResultCallback callback) {

                if (cancellationSignal.isCanceled()) {
                    callback.onWriteCancelled();
                    return;
                }

                String content = getContent();

                if (content != null && (content.startsWith("file://") || content.endsWith(".pdf"))) {
                    String path = content.startsWith("file://") ? content.substring(7) : content;
                    writePdfFile(path, destination, callback);
                } else {
                    callback.onWriteFailed("Unsupported content type");
                }
            }
        };
    }

    /**
     * Writes a PDF file to the print destination.
     */
    private void writePdfFile(String path, ParcelFileDescriptor destination,
                              PrintDocumentAdapter.WriteResultCallback callback) {
        InputStream input = null;
        OutputStream output = null;

        try {
            input = openInputStream(path);
            output = new FileOutputStream(destination.getFileDescriptor());

            byte[] buf = new byte[1024];
            int bytesRead;

            while ((bytesRead = input.read(buf)) > 0) {
                output.write(buf, 0, bytesRead);
            }

            callback.onWriteFinished(new PageRange[]{PageRange.ALL_PAGES});
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error writing PDF", e);
            callback.onWriteFailed(e.getMessage());
        } finally {
            try {
                if (input != null) input.close();
                if (output != null) output.close();
            } catch (IOException ignored) {
                // Ignore close errors
            }
        }
    }

    /**
     * Opens an input stream for the specified path.
     *
     * @param path The file path or URL.
     * @return The input stream.
     * @throws IOException If the stream cannot be opened.
     */
    @NonNull
    private InputStream openInputStream(@NonNull String path) throws IOException {
        Context context = activity.getApplicationContext();

        if (path.startsWith("base64:")) {
            String base64 = path.substring(7);
            int commaIndex = base64.indexOf(',');
            if (commaIndex > 0) {
                base64 = base64.substring(commaIndex + 1);
            }
            byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
            return new ByteArrayInputStream(bytes);
        }

        if (path.startsWith("file:///android_asset/")) {
            String assetPath = path.replace("file:///android_asset/", "");
            return context.getAssets().open(assetPath);
        }

        if (path.startsWith("content://")) {
            return context.getContentResolver().openInputStream(android.net.Uri.parse(path));
        }

        if (path.startsWith("http://") || path.startsWith("https://")) {
            URL url = new URL(path);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setDoInput(true);
            connection.connect();
            return connection.getInputStream();
        }

        // Try as a file path
        return new FileInputStream(new File(path));
    }
}
