package com.yougi.sample.launchpadusb;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final String ACTION_USB_PERMISSION = "USB_PERMISSION";
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            enableDeviceConnection(device);
                        }
                    } else {
                        printLoggingText("permission denied for device " + device);
                    }
                }
            }
        }
    };

    private UsbManager usbManager;
    private UsbDevice usbDevice;
    private UsbInterface usbInterface;
    private UsbDeviceConnection usbDeviceConnection;
    private UsbEndpoint inEndpoint;
    private UsbEndpoint outEndpoint;


    private StringBuilder loggingText = new StringBuilder();
    private TextView logging;

    private Button catchDeviceBtn;
    private Button sendDataBtn;
    private Button receiveDataBtn;
    private ReceiveDataThread receiveDataThread;
    private PendingIntent permissionUsbIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        logging = findViewById(R.id.main_activity_info_logging);
        logging.setMovementMethod(new ScrollingMovementMethod());

        catchDeviceBtn = findViewById(R.id.main_activity_catch_controller_btn);
        catchDeviceBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
                final Set<String> deviceNameKeys = deviceList.keySet();

                if (deviceNameKeys.size() == 0) {
                    printLoggingText("No Device Detected...");
                    return;
                } else if (deviceNameKeys.size() > 1) {
                    printLoggingText("Too Many Device Detected...");
                    return;
                }

                printLoggingText("Device Detected :");
                final UsbDevice usbDevice = deviceList.get(deviceNameKeys.iterator().next());
                usbManager.requestPermission(usbDevice, permissionUsbIntent);
                printLoggingText(formatUsbDevice(usbDevice));
            }
        });

        findViewById(R.id.main_activity_clear_log).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearLogging();

                receiveDataThread.stopThread();
                receiveDataThread = null;

                usbDevice = null;

                if (usbDeviceConnection != null) {
                    usbDeviceConnection.close();
                }
                usbDeviceConnection = null;

                catchDeviceBtn.setEnabled(true);
                receiveDataBtn.setEnabled(false);
                sendDataBtn.setEnabled(false);
            }
        });

        receiveDataBtn = findViewById(R.id.main_activity_receive_data);
        receiveDataBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                receiveDataBtn.setEnabled(false);
                launchReceiveDataThread();
            }
        });

        sendDataBtn = findViewById(R.id.main_activity_send_data);
        sendDataBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                printLoggingText("Send Data...");
                byte[] sendOut = new byte[]{(byte)240, 0, 32, 41, 9, 124, 5, 72};
                int length = usbDeviceConnection.bulkTransfer(outEndpoint, sendOut, sendOut.length, 100);
                printLoggingText("Send Data... length 1 : " + length);
                sendOut = new byte[]{101, 108, 108, 111, 32, 2, 119, 111};
                length = usbDeviceConnection.bulkTransfer(outEndpoint, sendOut, sendOut.length, 100);
                printLoggingText("Send Data... length 2 : " + length);
                sendOut = new byte[]{114, 108, 100, 33, (byte) 247};
                length = usbDeviceConnection.bulkTransfer(outEndpoint, sendOut, sendOut.length, 100);
                printLoggingText("Send Data... length 3 : " + length);
            }
        });

        receiveDataBtn.setEnabled(false);
        sendDataBtn.setEnabled(false);

        usbManager = (UsbManager) getApplicationContext().getSystemService(Context.USB_SERVICE);

        permissionUsbIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(mUsbReceiver, filter);
    }

    private void printLoggingText(String text) {
        loggingText.append(text).append("\n");
        logging.setText(loggingText);
    }

    private void launchReceiveDataThread() {
        if (usbDevice == null) {
            printLoggingText("Catch Usb Device before send or receive Data...");
            return;
        }

        if (receiveDataThread != null && receiveDataThread.isRunning) {
            printLoggingText("receive Data Thread has already started...");
            return;
        }

        receiveDataThread = new ReceiveDataThread();

        printLoggingText("Start Thread receive Data...");
        receiveDataThread.start();
    }

    private void clearLogging() {
        loggingText.delete(0, loggingText.length() - 1);
        logging.setText(loggingText);
    }

    private String formatUsbDevice(@NonNull UsbDevice device) {
        final String str = device.toString();
        StringBuilder sb = new StringBuilder();

        int nbTab = 0;
        for (int i = 0; i < str.length(); i++) {
            final char charAt = str.charAt(i);

            if (charAt == ']' || charAt == '[') {
                nbTab += charAt == ']' ? -1 : 1;
                int forNbTab = charAt == ']' ? nbTab : nbTab - 1;

                sb.append('\n');
                for (int j = 0; j < forNbTab; j++) {
                    sb.append("    ");
                }
            }

            if (charAt != '\n') {
                sb.append(charAt);
            }

            if (charAt == ',' || charAt == ']' || charAt == '[') {
                sb.append('\n');
                for (int j = 0; j < nbTab; j++) {
                    sb.append("    ");
                }
            }
        }

        return sb.toString();
    }

    private void enableDeviceConnection(UsbDevice device) {
        usbDevice = device;
        usbInterface = device.getInterface(0);

        final UsbEndpoint endpoint0 = usbInterface.getEndpoint(0);
        final UsbEndpoint endpoint1 = usbInterface.getEndpoint(1);
        if (endpoint0.getDirection() == UsbConstants.USB_DIR_IN) {
            inEndpoint = endpoint0;
            outEndpoint = endpoint1;
        } else {
            outEndpoint = endpoint0;
            inEndpoint = endpoint1;
        }

        usbDeviceConnection = usbManager.openDevice(usbDevice);
        usbDeviceConnection.claimInterface(usbInterface, true);

        catchDeviceBtn.setEnabled(false);
        sendDataBtn.setEnabled(true);
        receiveDataBtn.setEnabled(true);
    }

    private class ReceiveDataThread extends Thread {

        /**
         * The refresh interval in milliseconds.
         */
        private final long REFRESH_INTERVAL = 0;

        private boolean isRunning;

        private boolean isInterrupted;

        private int color = 3;

        @Override
        public void run() {
            isRunning = true;
            super.run();
            while (!isInterrupted() && !isInterrupted) {
                try {
                    Thread.sleep(REFRESH_INTERVAL);
                    if (isRunning) {
                        final byte[] recordIn = new byte[inEndpoint.getMaxPacketSize()];
                        final int receivedLength = usbDeviceConnection.bulkTransfer(inEndpoint, recordIn,
                                recordIn.length, 0);

                        if(receivedLength >= 2 && receivedLength%2 == 0){
                            for (int i = 0; i < receivedLength; i+=2) {
                                if(recordIn[i+1] == 127){
                                    final byte[] sendOut = new byte[]{(byte)144, recordIn[i], (byte)color, 0, 0, 0, 0, 0};
                                    usbDeviceConnection.bulkTransfer(outEndpoint, sendOut, sendOut.length, 0);
                                } else if (recordIn[i+1] == 0){
                                    final byte[] sendOut = new byte[]{(byte)144, recordIn[i], 0, 0, 0, 0, 0, 0};
                                    usbDeviceConnection.bulkTransfer(outEndpoint, sendOut, sendOut.length, 0);
                                }
                            }
                        }


                        if (receivedLength > -1) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    printLoggingText("nbData : " + receivedLength + "Data : " + Arrays.toString(recordIn) + " color : " + color);
                                }
                            });
                        }
                    }
                } catch (InterruptedException e) {
                    isInterrupted = true;
                }
            }
        }

        public void setIsRunning(boolean isRunning) {
            this.isRunning = isRunning;
        }

        public void stopThread() {
            setIsRunning(false);
            isInterrupted = true;
            interrupt();
        }
    }
}
