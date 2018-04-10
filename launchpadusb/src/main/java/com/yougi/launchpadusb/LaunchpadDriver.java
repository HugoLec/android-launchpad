package com.yougi.launchpadusb;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;

public final class LaunchpadDriver {

    private static final String TAG = "LaunchpadDriver";

    private static final String ACTION_USB_PERMISSION = "com.yougi.launchpadusb.USB_PERMISSION";

    private static LaunchpadDriver INSTANCE;

    private ArrayList<LaunchPadDriverObserver> launchPadDriverObservers;

    public static LaunchpadDriver getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new LaunchpadDriver();
        }

        return INSTANCE;
    }

    private final BroadcastReceiver usbAccessPermissionReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            Log.i(TAG, "Permission GRANTED for device : " + device);
                            UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
                            LaunchPadConnection launchPadConnection = new LaunchPadConnection(usbManager, device);
                            notifyOnConnectionSuccessed(launchPadConnection);
                        }
                    } else {
                        Log.w(TAG, "Permission DENIED for device : " + device);
                        notifyOnConnectionFailed(device.getDeviceName(), device.getDeviceName() + " - " +
                                device.getManufacturerName() + " - " + device.getProductName());
                    }
                }
            }
        }
    };

    private LaunchpadDriver() {
        launchPadDriverObservers = new ArrayList<>();
    }

    public void initLaunchpadDriver(Context context){
        if (context == null) {
            throw new IllegalArgumentException("the context in argument Cannot be null");
        }

        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        context.registerReceiver(usbAccessPermissionReceiver, filter);
    }

    public void releaseLaunchpadDriver(Context context){
        if (context == null) {
            throw new IllegalArgumentException("the context in argument Cannot be null");
        }

        context.unregisterReceiver(usbAccessPermissionReceiver);
    }

    public HashMap<String, String> getDevicesDetected(UsbManager usbManager) {
        if (usbManager == null) {
            throw new IllegalArgumentException("the usbManager in argument Cannot be null");
        }

        final HashMap<String, String> devices = new HashMap<>();
        final HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();

        if (deviceList.size() == 0) {
            // return empty list
            return devices;
        }


        for (String key : deviceList.keySet()) {
            final UsbDevice usbDevice = deviceList.get(key);
            if (isSupportedLaunchPad(usbDevice)) {
                devices.put(key, usbDevice.getDeviceName() + " - " +
                        usbDevice.getManufacturerName() + " - " + usbDevice.getProductName());
            }
        }

        return devices;
    }

    public boolean askDeviceConnectionAsync(Context context, UsbManager usbManager, String id) {
        if (context == null) {
            throw new IllegalArgumentException("the context in argument Cannot be null");
        }

        if (usbManager == null) {
            throw new IllegalArgumentException("the usbManager in argument Cannot be null");
        }

        final HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
        if (!deviceList.containsKey(id)) {
            return false;
        }

        final UsbDevice usbDevice = deviceList.get(id);

        notifyOnRequestConnectionOnDevice(id, usbDevice.getDeviceName() + " - " +
                usbDevice.getManufacturerName() + " - " + usbDevice.getProductName());

        PendingIntent permissionUsbIntent = PendingIntent.getBroadcast(context, 0,
                new Intent(ACTION_USB_PERMISSION), 0);
        usbManager.requestPermission(usbDevice, permissionUsbIntent);
        return true;
    }

    public boolean addLaunchPadDriverObserver(LaunchPadDriverObserver observer){
        if(observer == null){
            return false;
        }

        return launchPadDriverObservers.add(observer);
    }

    public boolean removeLaunchPadDriverObserver(LaunchPadDriverObserver observer){
        return launchPadDriverObservers.remove(observer);
    }

    private boolean isSupportedLaunchPad(UsbDevice usbDevice) {
        return usbDevice != null
                && usbDevice.getVendorId() == 4661
                && usbDevice.getProductId() == 14
                && usbDevice.getDeviceClass() == 255
                && usbDevice.getDeviceSubclass() == 0
                && usbDevice.getDeviceProtocol() == 255;
    }

    private void notifyOnRequestConnectionOnDevice(String deviceId, String deviceName){
        for (LaunchPadDriverObserver launchPadDriverObserver : launchPadDriverObservers) {
            launchPadDriverObserver.onRequestConnectionOnDevice(deviceId, deviceName);
        }
    }

    private void notifyOnConnectionSuccessed(LaunchPadConnection launchPadConnection){
        for (LaunchPadDriverObserver launchPadDriverObserver : launchPadDriverObservers) {
            launchPadDriverObserver.onConnectionSucceeded(launchPadConnection);
        }
    }

    private void notifyOnConnectionFailed(String deviceId, String deviceName){
        for (LaunchPadDriverObserver launchPadDriverObserver : launchPadDriverObservers) {
            launchPadDriverObserver.onConnectionFailed(deviceId, deviceName);
        }
    }

    public interface LaunchPadDriverObserver {
        void onRequestConnectionOnDevice(String deviceId, String deviceName);
        void onConnectionSucceeded(LaunchPadConnection launchPadConnection);
        void onConnectionFailed(String deviceId, String deviceName);
    }

}
