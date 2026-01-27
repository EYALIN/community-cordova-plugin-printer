/*
 * Licensed under MIT License
 */
package com.community.cordova.printer;

import android.content.Context;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintJob;
import android.print.PrintManager;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.apache.cordova.PluginResult.Status;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static android.content.Context.PRINT_SERVICE;
import static android.os.Build.VERSION.SDK_INT;

public class PrinterPlugin extends CordovaPlugin {

    private static final String LOG_TAG = "PrinterPlugin";

    /**
     * The callback context used when calling back into JavaScript.
     */
    private CallbackContext callbackContext;

    /**
     * Executes the request.
     *
     * @param action   The action to execute.
     * @param args     The exec() arguments.
     * @param callback The callback context used when calling back into JavaScript.
     *
     * @return Returning false results in a "MethodNotFound" error.
     */
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callback) throws JSONException {
        callbackContext = callback;

        try {
            if ("check".equalsIgnoreCase(action)) {
                check(args.optJSONObject(0));
                return true;
            }

            if ("types".equalsIgnoreCase(action)) {
                types();
                return true;
            }

            if ("print".equalsIgnoreCase(action)) {
                print(args.optJSONObject(0));
                return true;
            }

            return false;
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error executing action: " + action, e);
            callbackContext.error("Error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Informs if the device is able to print documents.
     *
     * @param options Options containing printer settings.
     */
    private void check(@Nullable JSONObject options) {
        cordova.getThreadPool().execute(() -> {
            try {
                JSONArray printers = new JSONArray();
                String printerId = options != null ? options.optString("printer", null) : null;
                boolean available = isPrintServiceAvailable();

                JSONObject result = new JSONObject();
                result.put("avail", available);
                result.put("printers", printers);

                sendPluginResult(result);
            } catch (JSONException e) {
                Log.e(LOG_TAG, "Error in check", e);
                callbackContext.error("Error: " + e.getMessage());
            }
        });
    }

    /**
     * Gets a list of the supported content types.
     */
    private void types() {
        cordova.getThreadPool().execute(() -> {
            JSONArray utis = new JSONArray();

            utis.put("application/pdf");
            utis.put("text/html");
            utis.put("text/plain");
            utis.put("image/png");
            utis.put("image/jpeg");
            utis.put("image/gif");

            sendPluginResult(utis);
        });
    }

    /**
     * Sends the provided content to the printer.
     *
     * @param options The settings and content to print.
     */
    private void print(@Nullable JSONObject options) {
        final PrinterManager printerManager = new PrinterManager(cordova.getActivity(), options);

        cordova.getActivity().runOnUiThread(() -> {
            try {
                // IMPORTANT: Must use Activity context, not ApplicationContext for PrintManager.print()
                PrintManager printManager = (PrintManager) cordova.getActivity().getSystemService(PRINT_SERVICE);
                String jobName = printerManager.getJobName();
                PrintAttributes.Builder builder = printerManager.toPrintAttributes();
                PrintDocumentAdapter adapter = printerManager.createPrintAdapter();

                PrintJob printJob = printManager.print(jobName, adapter, builder.build());

                // Monitor print job in background
                cordova.getThreadPool().execute(() -> {
                    waitForPrintJobCompletion(printJob);
                });
            } catch (Exception e) {
                Log.e(LOG_TAG, "Error printing", e);
                sendPluginResult("failed");
            }
        });
    }

    /**
     * Wait for print job completion and send result.
     * Returns: "completed", "cancelled", or "failed"
     */
    private void waitForPrintJobCompletion(@Nullable PrintJob job) {
        if (job == null) {
            Log.e(LOG_TAG, "waitForPrintJobCompletion: job is null");
            sendPluginResult("failed");
            return;
        }

        Log.d(LOG_TAG, "waitForPrintJobCompletion: starting to monitor job " + job.getId());

        while (true) {
            if (job.isCancelled()) {
                Log.d(LOG_TAG, "waitForPrintJobCompletion: job cancelled");
                sendPluginResult("cancelled");
                return;
            }
            if (job.isCompleted()) {
                Log.d(LOG_TAG, "waitForPrintJobCompletion: job completed");
                sendPluginResult("completed");
                return;
            }
            if (job.isFailed()) {
                Log.d(LOG_TAG, "waitForPrintJobCompletion: job failed");
                sendPluginResult("failed");
                return;
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Log.d(LOG_TAG, "waitForPrintJobCompletion: interrupted");
                sendPluginResult("cancelled");
                return;
            }
        }
    }

    /**
     * Gets the print manager service.
     *
     * @return A PrintManager instance.
     */
    private PrintManager getPrintManager() {
        return (PrintManager) getContext().getSystemService(PRINT_SERVICE);
    }

    /**
     * Gets the application context.
     *
     * @return The application context.
     */
    private Context getContext() {
        return cordova.getActivity().getApplicationContext();
    }

    /**
     * Checks if the print service is available.
     *
     * @return true if printing is available
     */
    private boolean isPrintServiceAvailable() {
        return SDK_INT >= 19 && getPrintManager() != null;
    }

    /**
     * Sends a plugin result back to JavaScript.
     *
     * @param result The result.
     */
    private void sendPluginResult(Object result) {
        PluginResult pluginResult;

        if (result == null) {
            pluginResult = new PluginResult(Status.NO_RESULT);
        } else if (result instanceof Boolean) {
            pluginResult = new PluginResult(Status.OK, (Boolean) result);
        } else if (result instanceof JSONObject) {
            pluginResult = new PluginResult(Status.OK, (JSONObject) result);
        } else if (result instanceof JSONArray) {
            pluginResult = new PluginResult(Status.OK, (JSONArray) result);
        } else {
            pluginResult = new PluginResult(Status.OK, result.toString());
        }

        if (callbackContext != null) {
            callbackContext.sendPluginResult(pluginResult);
        }
    }
}
