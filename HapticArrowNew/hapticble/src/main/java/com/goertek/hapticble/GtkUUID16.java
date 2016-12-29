package com.goertek.hapticble;

import java.util.UUID;

/**
 * Created by chaw.meng on 2016/12/29.
 */

public class GtkUUID16 {
    private static final UUID BLE_BASE = new UUID(0x1000L, 0x800000805F9B34FBL);
    private static final UUID GTK_BASE = new UUID(0x1BC500000200ECA1L, 0xE41120FAC04AFA8FL);

    public static UUID BLEToUUID(byte mostSigBits, byte leastSigBits) {
        long sigBits = (mostSigBits << 8) | leastSigBits;
        return new UUID(BLE_BASE.getMostSignificantBits() | (sigBits << 32),
                BLE_BASE.getLeastSignificantBits());
    }

    public static UUID BLEToUUID(int mostSigBits, int leastSigBits) {
        // Because Java doesn't allow you to declare literals as bytes.
        return BLEToUUID((byte) mostSigBits, (byte) leastSigBits);
    }

    public static UUID GtkToUUID(byte mostSigBits, byte leastSigBits) {
        long sigBits = (mostSigBits << 8) | leastSigBits;
        return  new UUID(GTK_BASE.getMostSignificantBits() | (sigBits << 32),
                GTK_BASE.getLeastSignificantBits());
    }

    public static UUID GtkToUUID(int mostSigBits, int leastSigBits) {
        // Because Java doesn't allow you to declare literals as bytes.
        return GtkToUUID((byte) mostSigBits, (byte) leastSigBits);
    }
}
