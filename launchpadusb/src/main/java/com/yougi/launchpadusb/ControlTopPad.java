package com.yougi.launchpadusb;

public enum ControlTopPad {
    ARROW_TOP(0, 0),
    ARROW_BOTTOM(0, 1),
    ARROW_LEFT(0, 2),
    ARROW_RIGHT(0, 3),
    SESSION(0, 4),
    USER_1(0, 5),
    USER_2(0, 6),
    MIXER(0, 7);

    final int padMapColumn;
    final int padMapLine;

    ControlTopPad(int padMapLine, int padMapColumn) {
        this.padMapColumn = padMapColumn;
        this.padMapLine = padMapLine;
    }

}