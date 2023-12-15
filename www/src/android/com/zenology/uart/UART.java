package com.zenology.uart;

import android.util.Log;

import com.google.android.things.pio.PeripheralManager;
import com.google.android.things.pio.UartDevice;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

public class UART extends CordovaPlugin {
    private static final String LIST = "list";
    private static final String OPEN = "open";
    private static final String WRITE = "write";
    private static final String CLOSE = "close";
    private static final String TAG =  UART.class.getName();

    //private CallbackContext callbackContext;
    //private JSONArray requestArgs;
    //private CordovaPlugin plugin;
    private UartDevice mDevice;

    /**
     * Executes the request.
     *
     * This method is called from the WebView thread. To do a non-trivial amount of work, use:
     *     cordova.getThreadPool().execute(runnable);
     *
     * To run on the UI thread, use:
     *     cordova.getActivity().runOnUiThread(runnable);
     *
     * @param action          The action to execute.
     * @param args            The exec() arguments.
     * @param callbackContext The callback context used when calling back into JavaScript.
     * @return                Whether the action was valid.
     *
     */
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        JSONObject arg_object = args.optJSONObject(0);
        if(arg_object == null) {
            return false;
        } else {
            // this.callbackContext = callbackContext;
            // this.requestArgs = args;
            // plugin = this;

            JSONObject opts = arg_object.getJSONObject("opts");

            switch (action) {
                case LIST:
                    list(callbackContext);
                    break;
                case OPEN:
                    open(callbackContext, opts);
                    break;
                case WRITE:
                    write(callbackContext, opts);
                    break;
                case CLOSE:
                    close(callbackContext);
                    break;
                default:
                    return false;
            }

            return true;
        }
    }

    private void list(CallbackContext callbackContext) {
        cordova.getThreadPool().execute(() -> {
            try {
                int port = 3;
                for (int p=3; p<8; p++) {
                    String uartPortPath = "/dev/ttyS" + port; // Example path for a built-in UART port
                    java.io.File uartPort = new java.io.File(uartPortPath);

                    //FileInputStream inputStream = new FileInputStream(uartPort);
                    try (java.io.FileOutputStream outputStream = new java.io.FileOutputStream(uartPort)) {
                        callbackContext.success(uartPortPath);
                    } catch (Exception ex) {

                    }
                }
                callbackContext.error("No serial port found");
            } catch (Exception ex) {
                callbackContext.error((ex.getMessage()));
            }
        });
    }

    private void open(CallbackContext callbackContext, JSONObject obj) {
        cordova.getThreadPool().execute(() -> {
            try {
                String deviceName = obj.optString("device_name");

                PeripheralManager manager = PeripheralManager.getInstance();
                mDevice = manager.openUartDevice(deviceName);

                // Configure the UART port
                mDevice.setBaudrate(obj.optInt("baud_rate", 9200));
                mDevice.setDataSize(obj.optInt("data_size", 8));
                mDevice.setParity(obj.has("parity") ? obj.optInt("parity") : UartDevice.PARITY_NONE);
                mDevice.setStopBits(obj.has("stop_bits") ? obj.getInt("stopBits") : 1);
                mDevice.setHardwareFlowControl(obj.optBoolean("flow_control", false) ? UartDevice.HW_FLOW_CONTROL_AUTO_RTSCTS : UartDevice.HW_FLOW_CONTROL_NONE);
            } catch (IOException | JSONException | RuntimeException ex) {
                callbackContext.error((ex.getMessage()));
            }
        });
    }

    private void write(CallbackContext callbackContext, JSONObject obj) {
        cordova.getThreadPool().execute(() -> {
            try {
                byte[] buffer = obj.optString("text", "").getBytes();

                String uartPortPath = obj.optString("dev", "/dev/ttyS7"); // Example path for a built-in UART port
                java.io.File uartPort = new java.io.File(uartPortPath);

                //FileInputStream inputStream = new FileInputStream(uartPort);
                try (java.io.FileOutputStream outputStream = new java.io.FileOutputStream(uartPort)) {
                    outputStream.write(buffer);
                    callbackContext.success("Write to "+uartPortPath);
                } catch (Exception ex) {
                    callbackContext.error((ex.getMessage()));
                }
                Log.d(TAG, "Wrote to peripheral "+uartPortPath);
            } catch (Exception ex) {
                callbackContext.error((ex.getMessage()));
            }
        });
    }

    private void close(CallbackContext callbackContext) {
        cordova.getThreadPool().execute(() -> {
            if (mDevice != null) {
                try {
                    mDevice.close();
                    mDevice = null;
                } catch (IOException ex) {
                    callbackContext.error((ex.getMessage()));
                }
            }
        });
    }
}