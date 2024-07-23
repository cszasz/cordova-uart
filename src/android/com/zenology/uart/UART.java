package com.zenology.uart;

import android.Manifest;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.google.android.things.pio.PeripheralManager;
import com.google.android.things.pio.UartDevice;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
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
                    String uartPortPath = "/dev/ttyS" + p; // Example path for a built-in UART port
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

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    public static List<byte[]> splitByCRLF(byte[] data) {
        List<byte[]> result = new ArrayList<>();

        int start = 0;

        for (int i = 0; i < data.length - 1; i++) {
            // Check for the \r\n sequence
            if (data[i] == '\r' && data[i + 1] == '\n') {
                // Extract the segment
                int length = i - start;
                byte[] segment = new byte[length];
                System.arraycopy(data, start, segment, 0, length);

                // Add the segment to the result list
                result.add(segment);

                // Skip the \r\n
                i++;
                start = i + 1;
            }
        }

        // Add the last segment if there's any remaining data
        if (start < data.length) {
            int length = data.length - start;
            byte[] segment = new byte[length];
            System.arraycopy(data, start, segment, 0, length);
            result.add(segment);
        }

        return result;
    }

    private void write(CallbackContext callbackContext, JSONObject obj) {
        cordova.getThreadPool().execute(() -> {
            try {
                String buff = obj.optString("text", "");
                byte[] buffer = obj.optString("text", "").getBytes();

                String uartPortPath = obj.optString("dev", "/dev/ttyS7"); // Example path for a built-in UART port
                java.io.File uartPort = new java.io.File(uartPortPath);
                try {
                    //FileInputStream inputStream = new FileInputStream(uartPort);
                    for (byte[] bytes : splitByCRLF(buffer)) {
                        try (java.io.FileOutputStream outputStream = new java.io.FileOutputStream(uartPort)) {
                            outputStream.write(bytes);
                            outputStream.write(0x0a);
                            outputStream.flush();
                            //Thread.sleep(2000);
                        }
                    }
                   //Thread.sleep(15000);
                   callbackContext.success("Write to "+uartPortPath+" text:"+buff+" -> "+new String(buffer)+" -> "+bytesToHex(buffer));
                } catch (Exception ex) {
                    callbackContext.error((ex.getMessage()+" text:"+buff+" -> "+new String(buffer)+" -> "+bytesToHex(buffer)));
                }
                Log.d(TAG, "Wrote to peripheral "+uartPortPath);
            } catch (Exception ex) {
                callbackContext.error((ex.getMessage()));
            }
        });
    }
    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
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