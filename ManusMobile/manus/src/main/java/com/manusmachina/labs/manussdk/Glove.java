/**
 * Copyright (C) 2015 Manus Machina
 *
 * This file is part of the Manus SDK.
 *
 * Manus SDK is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Manus SDK is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Manus SDK. If not, see <http://www.gnu.org/licenses/>.
 */

package com.manusmachina.labs.manussdk;

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
 * Created by Armada on 8-4-2015.
 */
public class Glove extends BluetoothGattCallback {
    // Left or Right glove enum
    public enum HAND {
        LEFT,
        RIGHT,
    }

    public class Quaternion {
        public float w, x, y, z;

        public Quaternion() {
            this(1, 0, 0, 0);
        }

        public Quaternion(float w, float x, float y, float z) {
            this.w = w;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public Quaternion(float[] array) {
            this.w = array[0];
            this.x = array[1];
            this.y = array[2];
            this.z = array[3];
        }

        public float[] ToArray() {
            return new float[]{w, x, y, z};
        }
    }

    public class Vector {
        public float x, y, z;

        public Vector() {
            this(0, 0, 0);
        }

        public Vector(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public Vector(float[] array) {
            this.x = array[0];
            this.y = array[1];
            this.z = array[2];
        }

        public Vector ToDegrees() {
            return new Vector((float) (x * 180.0 / Math.PI), (float) (y * 180.0 / Math.PI), (float) (z * 180.0 / Math.PI));
        }

        public float[] ToArray() {
            return new float[]{x, y, z};
        }
    }

    private static final UUID MANUS_GLOVE_SERVICE = UUID16.ManusToUUID(0x00, 0x01);
    private static final UUID MANUS_GLOVE_REPORT = UUID16.ManusToUUID(0x00, 0x02);
    private static final UUID MANUS_GLOVE_COMPASS = UUID16.ManusToUUID(0x00, 0x03);
    private static final UUID MANUS_GLOVE_FLAGS = UUID16.ManusToUUID(0x00, 0x04);
    private static final UUID MANUS_GLOVE_CALIB = UUID16.ManusToUUID(0x00, 0x05);
    private static final UUID MANUS_GLOVE_RUMBLE = UUID16.ManusToUUID(0x00, 0x06);

    private static final UUID CLIENT_CHARACTERISTIC_CONFIG = UUID16.BLEToUUID(0x29, 0x02);

    private static final float ACCEL_DIVISOR = 16384.0f;
    private static final float QUAT_DIVISOR = 16384.0f;
    private static final float FINGER_DIVISOR = 255.0f;

    protected static final int VENDOR_ID = 0x0220;
    protected static final int PRODUCT_ID = 0x0001;

    // flag for handedness (0 = left, 1 = right)
    private static final byte GLOVE_FLAGS_HANDEDNESS = 0x1;
    private static final byte GLOVE_FLAGS_CAL_GYRO = 0x2;
    private static final byte GLOVE_FLAGS_CAL_ACCEL = 0x4;
    private static final byte GLOVE_FLAGS_CAL_FINGERS = 0x8;

    // Output variables, volatile to ensure synchronisation
    private volatile byte mFlags = 0;
    private volatile float[] mQuat = new float[]{1, 0, 0, 0};
    private volatile short[] mAccel = new short[]{0, 0, 0};
    private volatile float[] mFingers = new float[]{0, 0, 0, 0, 0};

    protected GloveCallback mGloveCallback = null;
    protected BluetoothGatt mGatt = null;
    protected Iterator<BluetoothGattCharacteristic> mConnectIt = null;
    protected int mConnectionState = BluetoothGatt.STATE_DISCONNECTED;

    int value = -1;
    int current = -1;

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic report) {
        final int format = BluetoothGattCharacteristic.FORMAT_SINT16;

        // Only callback when the primary input report changed
        if (report.getUuid().equals(MANUS_GLOVE_REPORT)) {
            mQuat = new float[]{
                    report.getIntValue(format, 0) / QUAT_DIVISOR,
                    report.getIntValue(format, 2) / QUAT_DIVISOR,
                    report.getIntValue(format, 4) / QUAT_DIVISOR,
                    report.getIntValue(format, 6) / QUAT_DIVISOR
            };

            mAccel = new short[]{
                    report.getIntValue(format, 8).shortValue(),
                    report.getIntValue(format, 10).shortValue(),
                    report.getIntValue(format, 12).shortValue()
            };

            float[] fingers = new float[5];
            for (int i = 0; i < fingers.length; i++) {
                int finger = (mFlags & GLOVE_FLAGS_HANDEDNESS) != 0 ? i : 4 - i;
                fingers[finger] = report.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 14 + i) / FINGER_DIVISOR;
            }
            mFingers = fingers;

            //Log.d("MENG", "value is " + value + ", current is " + current);
            if (value == -1 && current == -1)
                value = report.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 19);
            else
                current = report.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 19);

            Log.d("MENG", "receive : " + current);

            if (current == 0) {
                if (value != 255)
                    Log.e("ERROR", "miss " + (255 - value));
            } else if (value != -1) {
                int miss = current - value;
                if (miss != 1)
                    Log.e("ERROR", "miss " + miss);
            }

            if (current != -1)
                value = current;

        }

        if (report.getUuid().equals(MANUS_GLOVE_REPORT))
            mGloveCallback.OnChanged(this);
    }

    @Override
    public void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) {
        super.onConnectionStateChange(gatt, status, newState);
        mConnectionState = newState;

        if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothGatt.STATE_CONNECTED) {
            if (gatt.getServices().isEmpty()) {
                gatt.discoverServices();
            } else {
                BluetoothGattService service = gatt.getService(MANUS_GLOVE_SERVICE);
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

        if (status == BluetoothGatt.GATT_SUCCESS) {
            // Try to see if this bluetooth device has the manus service
            BluetoothGattService service = gatt.getService(MANUS_GLOVE_SERVICE);
            if (service != null) {
                mConnectIt = service.getCharacteristics().iterator();
                if (mConnectIt.hasNext())
                    gatt.readCharacteristic(mConnectIt.next());
            } else {
                mGloveCallback.OnGloveConnected(this, false);
            }
        }
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        super.onDescriptorWrite(gatt, descriptor, status);

        if (mConnectIt.hasNext())
            gatt.readCharacteristic(mConnectIt.next());
    }

    /*
    Since Android cannot connect to more then one bluetooth device at the same time,
    we have to iterate trough all the characteristics from the Manus service
     */
    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic report, int status) {
        super.onCharacteristicRead(gatt, report, status);

        if (status == BluetoothGatt.GATT_SUCCESS) {
            if (report.getUuid().equals(MANUS_GLOVE_REPORT)) {
                // Enable notification if the report supports it
                if ((report.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                    // Enable the notification on the client
                    gatt.setCharacteristicNotification(report, true);

                    // Enable the notification on the server
                    BluetoothGattDescriptor descriptor = report.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG);
                    if (descriptor != null) {
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        gatt.writeDescriptor(descriptor);
                    }
                }
            } else if (report.getUuid().equals(MANUS_GLOVE_FLAGS)) {
                mFlags = report.getValue()[0];
                // if all characteristics are found, notify the list
                mGloveCallback.OnGloveConnected(this, true);

                if (mConnectIt.hasNext())
                    gatt.readCharacteristic(mConnectIt.next());
            } else if (mConnectIt.hasNext()) {
                gatt.readCharacteristic(mConnectIt.next());
            }
        }
    }

    protected Glove(Context con, BluetoothDevice dev, GloveCallback callback) {
        mGatt = dev.connectGatt(con, true, this);
        mGloveCallback = callback;
    }

    protected void close() {
        mGatt.close();
    }

    public boolean isConnected() {
        return mConnectionState == BluetoothGatt.STATE_CONNECTED;
    }

    public HAND getHandedness() {
        if ((mFlags & GLOVE_FLAGS_HANDEDNESS) == 0)
            return HAND.LEFT;
        else
            return HAND.RIGHT;
    }

    /**
     *  Returns the Finger values ranging from 0 to 1, where 0 is straight and 1 is bend.
     *
     *  @return Array of values of the fingers.
     */
    public float[] getFingers() {
        return mFingers;
    }

    /**
     *  Returns the Quaternion as Yaw, Pitch and Roll angles
     *  relative to the Earth's gravity.
     *
     *  @return The Quaternion of the glove.
     */

    public Quaternion getQuaternion() {
        return new Quaternion(mQuat);
    }

    /**
     *  Returns the Euler angles.
     *
     *  @return Euler angles of the glove.
     */
    public Vector getEuler() {
        final Quaternion q = new Quaternion(mQuat);

        return new Vector(
                // roll: (tilt left/right, about X axis)
                (float) Math.atan2(2 * (q.w * q.x + q.y * q.z), 1 - 2 * (q.x * q.x + q.y * q.y)),
                // pitch: (nose up/down, about Y axis)
                (float) Math.asin(2 * (q.w * q.y - q.z * q.x)),
                // yaw: (about Z axis)
                (float) Math.atan2(2 * (q.w * q.z + q.x * q.y), 1 - 2 * (q.y * q.y + q.z * q.z))
        );
    }

    /**
     *  Returns the Acceleration as a vector independent from
     *  the Earth's gravity.
     *
     *  @return Linear acceleration of the glove.
     */
    public Vector getAcceleration() {
        return new Vector(
                mAccel[0] / ACCEL_DIVISOR,
                mAccel[1] / ACCEL_DIVISOR,
                mAccel[2] / ACCEL_DIVISOR
        );
    }

    /**
     *  This reconfigures the glove for a different hand.
     *
     *  WARNING This function overwrites factory settings on the
     *  glove, it should only be called if the user requested it.
     *
     *  @param hand  Set the glove as a right or left hand.
     *  @return True if it succeeded
     */
    public boolean setHandedness(HAND hand) {
        final int format = BluetoothGattCharacteristic.FORMAT_UINT8;
        BluetoothGattService service = mGatt.getService(MANUS_GLOVE_SERVICE);
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(MANUS_GLOVE_FLAGS);

        if (hand == HAND.RIGHT)
            mFlags |= GLOVE_FLAGS_HANDEDNESS;
        else
            mFlags &= ~GLOVE_FLAGS_HANDEDNESS;

        characteristic.setValue(mFlags, format, 0);
        return mGatt.writeCharacteristic(characteristic);
    }

    /**
     *
     *  This sets the output power of the vibration motor.
     *
     *
     *  @param fPower The power of the vibration motor ranging from 0 to 1 (ex. 0.5 = 50% power).
     *               *  @return True if it succeeded
     */
    public boolean setVibration(float fPower) {
        final int format = BluetoothGattCharacteristic.FORMAT_UINT16;
        BluetoothGattService service = mGatt.getService(MANUS_GLOVE_SERVICE);
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(MANUS_GLOVE_RUMBLE);

        // clipping
        if (fPower < 0.0) fPower = 0.0f;
        if (fPower > 1.0) fPower = 1.0f;
        int iPower = (int)(fPower * (float)0xFFFF);

        characteristic.setValue(iPower, format, 0);
        return mGatt.writeCharacteristic(characteristic);
    }


    /** \brief Calibrate the IMU on the glove.
     *
     *  This will run a self-test of the IMU and recalibrate it.
     *  The glove should be placed on a stable flat surface during
     *  recalibration.
     *
     *  WARNING This function overwrites factory settings on the
     *  glove, it should only be called if the user requested it.
     *
     *  @param gyro      Calibrate the gyroscope.
     *  @param accel     Calibrate the accelerometer.
     *  @param fingers   True to start Calibrate the fingers, False to stop.
     *  @return True if it succeeded
     */
    public boolean calibrate(boolean gyro, boolean accel, boolean fingers) {
        final int format = BluetoothGattCharacteristic.FORMAT_UINT8;
        BluetoothGattService service = mGatt.getService(MANUS_GLOVE_SERVICE);
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(MANUS_GLOVE_FLAGS);

        byte flags = mFlags;
        if (gyro)
            flags |= GLOVE_FLAGS_CAL_GYRO;
        else
            flags &= ~GLOVE_FLAGS_CAL_GYRO;

        if (accel)
            flags |= GLOVE_FLAGS_CAL_ACCEL;
        else
            flags &= ~GLOVE_FLAGS_CAL_ACCEL;

        if (fingers)
            flags |= GLOVE_FLAGS_CAL_FINGERS;
        else
            flags &= ~GLOVE_FLAGS_CAL_FINGERS;

        characteristic.setValue(flags, format, 0);
        return mGatt.writeCharacteristic(characteristic);
    }
}
