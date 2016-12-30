package com.goertek.hapticble;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by chaw.meng on 2016/12/29.
 */

public class GtkBLE extends Service {

    private static String TAG = "GoerTek";

    // Binder given to clients
    private final IBinder mBinder = new GtkHapticBinder();

    // The bluetooth adapter
    private BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();

    // List of pending Haptics
    private volatile List<GtkHaptic> mPendingHaptics = new LinkedList<>();

    // List of detected Haptic
    private volatile GtkHaptic mHaptic = null;

    // Device connection iterator
    private Iterator<BluetoothDevice> mConnectIt = null;

    // List of event listeners
    private ArrayList<OnHapticChangedListener> mOnHapticChangedListeners = new ArrayList<>();

    // Connect to a BluetoothDevice
    private void connect(BluetoothDevice dev) {
        Log.d(TAG, "exist BT device, name is " + dev.getName() + ", address is " + dev.getAddress()
                //+ ", uuid is " + dev.getUuids().toString()
                + ", bondState is " + dev.getBondState()
                + ", type is " + dev.getType()
                + ", bluetoothClass is " + dev.getBluetoothClass());
        // Reconnect to this device if a Haptic has already been added for it
        if (mHaptic != null) {
            if (mHaptic.mGatt.getDevice().getAddress().equals(dev.getAddress())) {
                mHaptic.mGatt.connect();
                return;
            }
        }

        GtkHaptic haptic = new GtkHaptic(this, dev, mGtkHapticCallback);
        Log.d(TAG, "haptic is " + haptic + ", is connected ? " + haptic.isConnected());
        if (haptic.isConnected())
            mPendingHaptics.add(haptic);
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.i(TAG, "intent action : " + action);

            if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                final BluetoothDevice dev = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                final int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE,
                        BluetoothDevice.BOND_NONE);
                if (state == BluetoothDevice.BOND_BONDED) {
                    connect(dev);
                }
            } else if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE,
                        BluetoothAdapter.ERROR);

                // Connect to all bonded devices
                if (state == BluetoothAdapter.STATE_ON && mAdapter != null) {
                    mConnectIt = mAdapter.getBondedDevices().iterator();
                    while (mConnectIt.hasNext())
                        connect(mConnectIt.next());
                }
            }
        }
    };

    private GtkHapticCallback mGtkHapticCallback = new GtkHapticCallback() {
        @Override
        public void OnHapticConnected(GtkHaptic haptic, boolean serviceFound) {
            Log.i(TAG, "OnHapticConnected, serviceFound is " + serviceFound);
            if (serviceFound) {
                if (mHaptic == null) {
                    mHaptic = haptic;
                } else {
                    haptic.close();
                }
            } else {
                haptic.close();
            }

            mPendingHaptics.remove(haptic);

            if (mConnectIt.hasNext()) {
                connect(mConnectIt.next());
            }
        }

        @Override
        public void OnChanged(GtkHaptic haptic) {
            Log.i(TAG, "OnChanged haptic is " + haptic);
            for (OnHapticChangedListener listener : mOnHapticChangedListeners) {
                listener.OnHapticChanged(haptic);
            }
        }
    };

    public interface OnHapticChangedListener {
        void OnHapticChanged(GtkHaptic haptic);
    }

    /**
     * Class used for the Client Binder
     * This service always runs in the same process as its clients.
     * We don't need to deal with IPC
     */

    public class GtkHapticBinder extends Binder {
        public GtkHaptic getGtkHaptic() {
            Log.d(TAG, "mHaptic is " + mHaptic);
            if (mHaptic != null)
                return mHaptic;

            return null;
        }

        /**
         * Add a listener that will be called when the Haptic state is changed
         * @param listener The listener that will be called when the Haptic state changed
         */
        public void addOnHapticChangedListener(OnHapticChangedListener listener) {
            if (!mOnHapticChangedListeners.contains(listener)) {
                mOnHapticChangedListeners.add(listener);
            }
        }

        /**
         * Remove a listener for Haptic state changes
         * @param listener The listener for Haptic state changes.
         */
        public void removeOnHapticChangedListener(OnHapticChangedListener listener) {
            mOnHapticChangedListeners.remove(listener);
        }
    }

    @Override
    public void onCreate() {
        // Use this check to determine whether BLE is supported on the device
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            throw new UnsupportedOperationException();
        }

        // Initializes a Bluetooth adapter.
        // For API level 18 and above, get a reference to Bluetooth through BluetoothManager
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);

        // Retrieve the Bluetooth adapter
        if (bluetoothManager != null) {
            mAdapter = bluetoothManager.getAdapter();
            Log.d(TAG, "BT is on ? " + mAdapter.isEnabled());
        }

        // Connect to all bonded devices
        if (mAdapter != null && mAdapter.isEnabled()) {
            mConnectIt = mAdapter.getBondedDevices().iterator();
            while (mConnectIt.hasNext()) {
                connect(mConnectIt.next());
            }
        }

        // Register for broadcasts
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mReceiver, filter);
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mReceiver);

    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
}
