package com.yougi.sample.launchpadusb;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.yougi.launchpadusb.ControlRightPad;
import com.yougi.launchpadusb.ControlTopPad;
import com.yougi.launchpadusb.LaunchPadConnection;
import com.yougi.launchpadusb.LaunchpadDriver;
import com.yougi.launchpadusb.PadColor;

import java.util.HashMap;
import java.util.Set;

@SuppressLint("SetTextI18n")
public class MainActivity2 extends AppCompatActivity implements View.OnClickListener, LaunchpadDriver.LaunchPadDriverObserver, LaunchPadConnection.OnReceiveLaunchPadListener {

    private static final String TAG = "MainActivity2";

    private Button connectDeviceBtn;
    private Button releaseDeviceBtn;

    private Button testDeviceBtn;

    private TextView deviceNameTxt;
    private TextView deviceStatusTxt;

    private LaunchpadDriver launchpadDriver;
    private UsbManager usbManager;

    @Nullable
    private LaunchPadConnection launchPadConnection;

    private boolean pendingRequestConnection;

    final Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_2);

        launchpadDriver = LaunchpadDriver.getInstance();
        launchpadDriver.initLaunchpadDriver(getApplicationContext());
        usbManager = (UsbManager) getApplicationContext().getSystemService(Context.USB_SERVICE);

        deviceNameTxt = findViewById(R.id.main_activity_device_name_content);
        deviceStatusTxt = findViewById(R.id.main_activity_device_status_content);

        connectDeviceBtn = findViewById(R.id.main_activity_connect_device);
        connectDeviceBtn.setOnClickListener(this);

        releaseDeviceBtn = findViewById(R.id.main_activity_release_device);
        releaseDeviceBtn.setOnClickListener(this);

        testDeviceBtn = findViewById(R.id.main_activity_send_data);
        testDeviceBtn.setOnClickListener(this);

        deviceNameTxt.setText("N/A");
        deviceStatusTxt.setText("N/A");

        refreshUIState();
    }

    @Override
    protected void onStart() {
        super.onStart();
        launchpadDriver.addLaunchPadDriverObserver(this);

        if(launchPadConnection != null){
            launchPadConnection.enableSendDataProcess();
            launchPadConnection.enableListenerDataProcess();
            launchPadConnection.registerOnReceiveLaunchPadEvents(this);
        }
    }

    @Override
    protected void onStop() {
        launchpadDriver.removeLaunchPadDriverObserver(this);

        if(launchPadConnection != null){
            this.launchPadConnection.unregisterOnReceiveLaunchPadEvents(this);
            launchPadConnection.disableSendDataProcess();
            launchPadConnection.disableListenerDataProcess();
        }

        super.onStop();
    }

    @Override
    public void onClick(View view) {
        final int id = view.getId();
        switch (id) {
            case R.id.main_activity_connect_device:
                clickOnConnectDevice();
                break;
            case R.id.main_activity_release_device:
                clickOnReleaseDevice();
                break;
            case R.id.main_activity_send_data:
                launchTestLaunchpadProcess();
                break;
            default:
                throw new IllegalStateException("This button isn't managed : " + id);
        }

    }

    @Override
    public void onRequestConnectionOnDevice(String deviceId, String deviceName) {
        Log.d(TAG, "onRequestConnectionOnDevice() called with: deviceId = [" + deviceId + "], deviceName = [" + deviceName + "]");
        pendingRequestConnection = true;
        launchPadConnection = null;

        deviceNameTxt.setText(deviceName);
        deviceStatusTxt.setText("Ask Connection...");
        refreshUIState();
    }

    @Override
    public void onConnectionSucceeded(LaunchPadConnection launchPadConnection) {
        Log.d(TAG, "onConnectionSucceeded() called with: launchPadConnection = [" + launchPadConnection + "]");
        pendingRequestConnection = false;
        this.launchPadConnection = launchPadConnection;
        this.launchPadConnection.registerOnReceiveLaunchPadEvents(this);
        launchPadConnection.enableSendDataProcess();
        launchPadConnection.enableListenerDataProcess();

        deviceNameTxt.setText(launchPadConnection.getDeviceName());
        deviceStatusTxt.setText("Connection Succeded");
        refreshUIState();
    }

    @Override
    public void onConnectionFailed(String deviceId, String deviceName) {
        Log.d(TAG, "onConnectionFailed() called with: deviceId = [" + deviceId + "], deviceName = [" + deviceName + "]");
        pendingRequestConnection = false;
        launchPadConnection = null;

        deviceNameTxt.setText(deviceName);
        deviceStatusTxt.setText("Connection Failed...");
        refreshUIState();
    }

    @Override
    public void OnReceiveTopControlEvent(ControlTopPad controlTopPad, boolean isDown) {
        if(launchPadConnection == null){
            return;
        }

        if(isDown)
            launchPadConnection.enablePadTopControl(controlTopPad, PadColor.Red.DISABLE, PadColor.Green.POWER3);
        else
            launchPadConnection.disablePadTopControl(controlTopPad);
    }

    @Override
    public void OnReceiveRightControlEvent(ControlRightPad controlRightPad, boolean isDown) {
        if(launchPadConnection == null){
            return;
        }

        if(isDown)
            launchPadConnection.enablePadRightControl(controlRightPad, PadColor.Red.DISABLE, PadColor.Green.POWER3);
        else
            launchPadConnection.disablePadRightControl(controlRightPad);
    }

    @Override
    public void OnReceiveMainPadEvent(int padId, boolean isDown) {
        if(launchPadConnection == null){
            return;
        }

        if(isDown)
            launchPadConnection.enablePad(padId, PadColor.Red.DISABLE, PadColor.Green.POWER3);
        else
            launchPadConnection.disablePad(padId);
    }

    private void clickOnConnectDevice(){
        if(launchPadConnection != null){
            throw new IllegalStateException("Cannot launch connection process with already pad connected.");
        }

        final HashMap<String, String> devicesDetected = launchpadDriver.getDevicesDetected(usbManager);
        final Set<String> deviceNameKeys = devicesDetected.keySet();

        if (deviceNameKeys.size() == 0) {
            Log.d(TAG, "No Device Detected...");
            deviceNameTxt.setText("No Device Detected...");
            return;
        } else if (deviceNameKeys.size() > 1) {
            Log.d(TAG, "Too Many Device Detected...");
            deviceNameTxt.setText("Too Many Device Detected...");
            return;
        }

        final String key = deviceNameKeys.iterator().next();
        final String deviceDescription = devicesDetected.get(key);
        Log.d(TAG, "Device Detected key -> :" + key + " value -> " + deviceDescription);
        deviceNameTxt.setText(deviceDescription);
        deviceStatusTxt.setText("Not Connected");

        final boolean askConnect = launchpadDriver.askDeviceConnectionAsync(getApplicationContext(), usbManager, key);
        Log.d(TAG, "Ask Connection to : " + key + " sended : " + askConnect);
    }

    private void clickOnReleaseDevice(){
        if(launchPadConnection == null){
            throw new IllegalStateException("Cannot release device without connected device...");
        }

        final String deviceName = launchPadConnection.getDeviceName();
        //launchPadConnection.releaseDevice();
        launchPadConnection = null;

        deviceNameTxt.setText(deviceName);
        deviceStatusTxt.setText("Connection Released");
        refreshUIState();
    }


    private void launchTestLaunchpadProcess() {
        if(launchPadConnection == null){
            throw new IllegalStateException("Cannot launch test process without connected device...");
        }

        testDeviceBtn.setEnabled(false);

        handler.post(new Runnable() {
            private final static int NB_ITERATION = 32;

            private int nbExecuted = 0;

            @Override
            public void run() {
                if(nbExecuted > 0) {
                    launchPadConnection.disablePad((nbExecuted-1) % 8);
                }

                if(nbExecuted >= NB_ITERATION){
                    testDeviceBtn.setEnabled(true);
                    return;
                }

                launchPadConnection.enablePad(nbExecuted%8, PadColor.Red.DISABLE, PadColor.Green.POWER3);

                nbExecuted++;
                //handler.post(this);
                handler.postDelayed(this, 40);
            }
        });

    }

    private void refreshUIState() {
        if(pendingRequestConnection){
            connectDeviceBtn.setEnabled(false);
            releaseDeviceBtn.setEnabled(false);
            testDeviceBtn.setEnabled(false);
        } else if (launchPadConnection == null) {
            connectDeviceBtn.setEnabled(true);
            releaseDeviceBtn.setEnabled(false);
            testDeviceBtn.setEnabled(false);
        } else {
            connectDeviceBtn.setEnabled(false);
            releaseDeviceBtn.setEnabled(true);
            testDeviceBtn.setEnabled(true);

        }
    }
}
