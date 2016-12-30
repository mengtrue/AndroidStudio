package com.goertek.hapticble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.util.Log;

import java.util.Iterator;
import java.util.UUID;

/**
 * Created by chaw.meng on 2016/12/29.
 */

public class GtkHaptic extends BluetoothGattCallback {

    private static String TAG = "GoerTek";

    private static final UUID GTK_HAPTIC_SERVICE = GtkUUID16.GtkToUUID(0x00, 0x01);
    private static final UUID GTK_HAPTIC_REPORT  = GtkUUID16.GtkToUUID(0x00, 0x02);
    private static final UUID GTK_HAPTIC_RUMBLE  = GtkUUID16.GtkToUUID(0x00, 0x06);

    private static final UUID CLIENT_CHARACTERISTIC_CONFIG = GtkUUID16.BLEToUUID(0x29, 0x02);

    protected GtkHapticCallback mGtkHapticCallback = null;
    protected BluetoothGatt mGatt = null;
    protected Iterator<BluetoothGattCharacteristic> mConnectIt = null;
    protected int mConnectionState = BluetoothGatt.STATE_DISCONNECTED;

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic report) {
        final int format = BluetoothGattCharacteristic.FORMAT_SINT16;

        Log.i(TAG, "onCharacteristicChanged uuid is " + report.getUuid() + ", GTK_HAPTIC_REPORT is " + GTK_HAPTIC_REPORT);
        // Only callback when the primary input report changed
        if (report.getUuid().equals(GTK_HAPTIC_REPORT)) {
            Log.i(TAG, "GTK_HAPTIC_REPORT is " + GTK_HAPTIC_REPORT);
            mGtkHapticCallback.OnChanged(this);
        }
    }

    @Override
    public void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) {
        super.onConnectionStateChange(gatt, status, newState);
        mConnectionState = newState;

        Log.i(TAG, "onConnectionStateChange status is " + status + ", newState is " + newState);

        if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothGatt.STATE_CONNECTED) {
            if (gatt.getServices().isEmpty()) {
                gatt.discoverServices();
            } else {
                BluetoothGattService service = gatt.getService(GTK_HAPTIC_SERVICE);
                if (service != null) {
                    mConnectIt = service.getCharacteristics().iterator();
                    if (mConnectIt.hasNext())
                        gatt.readCharacteristic(mConnectIt.next());
                }
            }
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        super.onServicesDiscovered(gatt, status);

        Log.i(TAG, "onServicesDiscovered status is " + status);

        if (status == BluetoothGatt.GATT_SUCCESS) {
            // Try to see if this bluetooth device has the Gtk service
            BluetoothGattService service = gatt.getService(GTK_HAPTIC_SERVICE);
            Log.i(TAG, "get GtkHaptic service : " + service);
            if (service != null) {
                mConnectIt = service.getCharacteristics().iterator();
                if (mConnectIt.hasNext())
                    gatt.readCharacteristic(mConnectIt.next());
                mGtkHapticCallback.OnHapticConnected(this, true);
            } else {
                mGtkHapticCallback.OnHapticConnected(this, false);
            }
        }
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        super.onDescriptorWrite(gatt, descriptor, status);

        Log.i(TAG, "onDescriptorWrite status is " + status);

        if (mConnectIt.hasNext())
            gatt.readCharacteristic(mConnectIt.next());
    }

    /**
     * NoW Since Android cannot connect to more than one bluetooth device at the same time,
     * we have to iterate through all the characteristics from the Gtk Haptic service
     */

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic report, int status) {
        super.onCharacteristicRead(gatt, report, status);

        Log.i(TAG, "onCharacteristicRead status is " + status + ", report uuid is " + report.getUuid());

        if (status == BluetoothGatt.GATT_SUCCESS) {
            if (report.getUuid().equals(GTK_HAPTIC_REPORT)) {
                // Enable notification if the reprot supports it
                if ((report.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                    // Enable the notification on the client
                    gatt.setCharacteristicNotification(report, true);

                    //Enable the notification on the server
                    BluetoothGattDescriptor descriptor = report.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG);
                    if (descriptor != null) {
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        gatt.writeDescriptor(descriptor);
                    }
                }
            } else if (mConnectIt.hasNext()) {
                gatt.readCharacteristic(mConnectIt.next());
            }
        }
    }

    protected GtkHaptic(Context con, BluetoothDevice dev, GtkHapticCallback callback) {
        mGatt = dev.connectGatt(con, true, this);
        mGtkHapticCallback = callback;
    }

    protected void close() {
        mGatt.close();;
    }

    public boolean isConnected() {
        return mConnectionState == BluetoothGatt.STATE_CONNECTED;
    }

    /**
     * set the output power of the vibration motor
     * @param fPower The power of the vibration motor ranging from 0 to 1 (ex 0.5 = %50 power)
     * @return       True if it succeeded
     */
    public boolean setVibration(float fPower) {
        Log.i(TAG, "setVibration");
        final int format = BluetoothGattCharacteristic.FORMAT_UINT16;
        BluetoothGattService service = mGatt.getService(GTK_HAPTIC_SERVICE);
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(GTK_HAPTIC_RUMBLE);

        // clipping
        if (fPower < 0.0) fPower = 0.0f;
        if (fPower > 1.0) fPower = 1.0f;
        int iPower = (int)(fPower * (float)0xFFFF);

        characteristic.setValue(iPower, format, 0);
        return mGatt.writeCharacteristic(characteristic);
    }
}
