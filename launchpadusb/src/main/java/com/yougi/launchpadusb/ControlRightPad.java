package com.yougi.launchpadusb;

public enum ControlRightPad {
        VOL(1, 8),
        PAN(2, 8),
        SND_A(3, 8),
        SND_B(4, 8),
        STOP(5, 8),
        TRK_ON(6, 8),
        SOLO(7, 8),
        ARM(8, 8);


        final int padMapColumn;
        final int padMapLine;

        ControlRightPad(int padMapLine, int padMapColumn) {
            this.padMapColumn = padMapColumn;
            this.padMapLine = padMapLine;
        }
    }