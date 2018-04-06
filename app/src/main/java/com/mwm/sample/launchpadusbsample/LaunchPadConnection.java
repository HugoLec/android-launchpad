package com.mwm.sample.launchpadusbsample;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

public class LaunchPadConnection {

    private static final Byte[][] PAD_MAP =
            {{104, 105, 106, 107, 108, 109, 110, 111, null},
                    {0, 1, 2, 3, 4, 5, 6, 7, 8},
                    {16, 17, 18, 19, 20, 21, 22, 23, 24},
                    {32, 33, 34, 35, 36, 37, 38, 39, 40},
                    {48, 49, 50, 51, 52, 53, 54, 55, 56},
                    {64, 65, 66, 67, 68, 69, 70, 71, 72},
                    {80, 81, 82, 83, 84, 85, 86, 87, 88},
                    {96, 97, 98, 99, 100, 101, 102, 103, 104},
                    {112, 113, 114, 115, 116, 117, 118, 119, 120}};

    private static final byte ID_PAD_TYPE_CONTROL_TOP = (byte) 176;
    private static final byte ID_PAD_TYPE_CONTROL_RIGHT = (byte) 144;
    private static final byte ID_PAD_TYPE_BUTTON = (byte) 144;

    private final LinkedBlockingQueue<byte[]> eventToSend;

    private final SendDataThread sendDataThread;

    private final UsbManager usbManager;
    private final UsbDevice usbDevice;
    private final UsbDeviceConnection usbDeviceConnection;
    private final UsbInterface usbInterface;
    private final UsbEndpoint inEndpoint;
    private final UsbEndpoint outEndpoint;

    LaunchPadConnection(UsbManager usbManager, UsbDevice usbDevice) {
        this.usbDevice = usbDevice;
        this.eventToSend = new LinkedBlockingQueue<>();
        this.usbManager = usbManager;

        this.usbInterface = usbDevice.getInterface(0);

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

        //TODO Start/Stop This thread when connection to launchpad successed


        sendDataThread = new SendDataThread();
        sendDataThread.start();
    }

//    TODO ...............
//    public void releaseConnection(){
//        eventToSend.clear();
//        sendDataThread.stopThread();
//    }

    public String getDeviceName() {
        return usbDevice.getDeviceName() + " - " +
                usbDevice.getManufacturerName() + " - " + usbDevice.getProductName();
    }

    public void enablePadTopControl(ControlTopPad controlTopPad) {
        byte padId = PAD_MAP[controlTopPad.padMapLine][controlTopPad.padMapColumn];
        internalEnablePad(PadType.CONTROL_TOP, padId, (byte) 3);

    }

    public void disablePadTopControl(ControlTopPad controlTopPad) {
        byte padId = PAD_MAP[controlTopPad.padMapLine][controlTopPad.padMapColumn];
        internalDisablePad(PadType.CONTROL_TOP, padId);
    }

    public void enablePadRightControl(ControlRightPad controlRightPad) {
        byte padId = PAD_MAP[controlRightPad.padMapLine][controlRightPad.padMapColumn];
        internalEnablePad(PadType.CONTROL_RIGHT, padId, (byte) 3);
    }

    public void disablePadRightControl(ControlRightPad controlRightPad) {
        byte padId = PAD_MAP[controlRightPad.padMapLine][controlRightPad.padMapColumn];
        internalDisablePad(PadType.CONTROL_RIGHT, padId);
    }

    public void enablePad(int padId) {
        if (padId < 0 || padId > 63) {
            throw new IllegalArgumentException("this pad id isn't supported : " + padId);
        }

        final int padMapLine = 1 + ((int) (padId / 8f));
        final int padMapColumn = padId % 8;
        final byte padMapId = PAD_MAP[padMapLine][padMapColumn];

        //TODO Implement Color
        internalEnablePad(PadType.PAD, padMapId, (byte) 48);
    }

    public void disablePad(int padId) {
        if (padId < 0 || padId > 63) {
            throw new IllegalArgumentException("this pad id isn't supported : " + padId);
        }

        final int padMapLine = 1 + ((int) (padId / 8f));
        final int padMapColumn = padId % 8;
        final byte padMapId = PAD_MAP[padMapLine][padMapColumn];

        internalDisablePad(PadType.PAD, padMapId);
    }

    private void internalEnablePad(PadType type, byte padId, byte color) {
        final byte padTypeIdentifier = type.idPadIdentifier;
        byte[] data = {padTypeIdentifier, padId, color};
        eventToSend.offer(data);
    }

    private void internalDisablePad(PadType type, byte padId) {
        final byte padTypeIdentifier = type.idPadIdentifier;
        byte[] data = {padTypeIdentifier, padId, 0};
        eventToSend.offer(data);
    }

    private class SendDataThread extends Thread {

        /**
         * The refresh interval in milliseconds.
         */
        private final long REFRESH_INTERVAL = 0;

        private boolean isRunning;

        private boolean isInterrupted;

        @Override
        public void run() {
            isRunning = true;
            super.run();
            while (!isInterrupted() && !isInterrupted) {
                try {
                    Thread.sleep(REFRESH_INTERVAL);
                    if (isRunning) {
                        if(!eventToSend.isEmpty()){
                            List<byte[]> dataCollected = new ArrayList<>();
                            eventToSend.drainTo(dataCollected);

                            Log.d("SendDataThread", "sended packed of nb elements : " +  dataCollected.size());
                            for (byte[] bytes : dataCollected) {

                                String val = "";
                                for (int i = 0; i < bytes.length; i++){
                                    val += ((int) bytes[i] < 0 ? 256 + bytes[i] : bytes[i]) + " ";
                                }
                                Log.d("SendDataThread", "sended data : " +  val);

                                usbDeviceConnection.bulkTransfer(outEndpoint, bytes, bytes.length, 0);
                            }
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

    private enum PadType {
        CONTROL_TOP(ID_PAD_TYPE_CONTROL_TOP),
        CONTROL_RIGHT(ID_PAD_TYPE_CONTROL_RIGHT),
        PAD(ID_PAD_TYPE_BUTTON);

        private final byte idPadIdentifier;

        PadType(byte idPadIdentifier) {
            this.idPadIdentifier = idPadIdentifier;
        }
    }

    public enum ControlTopPad {
        ARROW_TOP(0, 0),
        ARROW_BOTTOM(0, 1),
        ARROW_LEFT(0, 2),
        ARROW_RIGHT(0, 3),
        SESSION(0, 4),
        USER_1(0, 5),
        USER_2(0, 6),
        MIXER(0, 7);

        private final int padMapColumn;
        private final int padMapLine;

        ControlTopPad(int padMapLine, int padMapColumn) {
            this.padMapColumn = padMapColumn;
            this.padMapLine = padMapLine;
        }

    }

    public enum ControlRightPad {
        VOL(1, 8),
        PAN(2, 8),
        SND_A(3, 8),
        SND_B(4, 8),
        STOP(5, 8),
        TRK_ON(6, 8),
        SOLO(7, 8),
        ARM(8, 8);


        private final int padMapColumn;
        private final int padMapLine;

        ControlRightPad(int padMapLine, int padMapColumn) {
            this.padMapColumn = padMapColumn;
            this.padMapLine = padMapLine;
        }
    }

}
