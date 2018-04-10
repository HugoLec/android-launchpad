package com.yougi.launchpadusb;

public final class PadColor {

    private PadColor(){
        // static access
    }

    public enum Red {
        DISABLE((byte)0),
        POWER1((byte)1),
        POWER2((byte)2),
        POWER3((byte)3);

        final byte colorId;

        Red(byte colorId) {
            this.colorId = colorId;
        }
    }

    public enum Green {
        DISABLE((byte)0),
        POWER1((byte)16),
        POWER2((byte)32),
        POWER3((byte)48);

        final byte colorId;

        Green(byte colorId) {
            this.colorId = colorId;
        }
    }

}
