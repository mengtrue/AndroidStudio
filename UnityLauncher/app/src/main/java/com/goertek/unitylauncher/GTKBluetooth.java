package com.goertek.unitylauncher;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothInputDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.text.TextUtils;
import android.util.Log;

import com.unity3d.player.UnityPlayer;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Created by chaw.meng on 2017/2/7.
 */

public class GTKBluetooth {
    private static final String TAG = "GTKBT";

    private Context mContext;
    private IntentFilter mFilter;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            handEvent(intent);
        }
    };

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice bluetoothDevice;
    private BluetoothInputDevice bluetoothInputDevice;

    private int devicePairType;
    private final int BT_PROFILE_INPUT_DEVICE = 4;

    public GTKBluetooth(Context context) {
        mContext = context;
        mFilter =  new IntentFilter();
        mFilter.addAction(BluetoothDevice.ACTION_FOUND);
        mFilter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
        mFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        mFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        mFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        mFilter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
        mContext.registerReceiver(mReceiver, mFilter);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothDevice = null;
        bluetoothInputDevice = null;
    }

    private int GTKGetBTState() {
        int state = BluetoothAdapter.STATE_OFF;
        if (bluetoothAdapter != null)
            state = bluetoothAdapter.getState();
        return state;
    }

    private int GTKGetBTConnState() {
        Method method = null;
        int connState = 0;
        try {
            method = Class.forName("android.bluetooth.BluetoothAdapter")
                    .getMethod("getConnectionState");
            connState = (Integer) method.invoke(bluetoothAdapter);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return connState;
    }

    public void GTKEnableBT(boolean enable) {
        if (bluetoothAdapter != null) {
            if (enable) {
                if (!bluetoothAdapter.isEnabled()) {
                    if (!bluetoothAdapter.enable()) {
                        Log.e(TAG, "GTK Bluetooth enable fail");
                        return;
                    }
                }
            } else {
                if (bluetoothAdapter.isDiscovering())
                    bluetoothAdapter.cancelDiscovery();
                if (bluetoothInputDevice != null && bluetoothDevice != null) {
                    bluetoothInputDevice.disconnect(bluetoothDevice);
                    bluetoothAdapter.closeProfileProxy(BT_PROFILE_INPUT_DEVICE, bluetoothInputDevice);
                    bluetoothInputDevice = null;
                }
                if (!bluetoothAdapter.disable())
                    Log.e(TAG, "GTK Bluetooth disable fail");
            }
        }
    }

    private void send() {
        Log.d(TAG, "GTK BT sendDiscoveryBroadcast");
        if (bluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 3000);
            mContext.startActivity(intent);
        }
    }

    public void GTKScanBT() {
        if (bluetoothAdapter == null)
            return;
        if (bluetoothAdapter.isDiscovering())
            bluetoothAdapter.cancelDiscovery();
        bluetoothAdapter.startDiscovery();

        send();

        if (GTKGetBTConnState() == BluetoothAdapter.STATE_CONNECTED)
            bluetoothAdapter.getProfileProxy(mContext, new remoteDeviceServiceListener(), BT_PROFILE_INPUT_DEVICE);
    }

    public void GTKBTPair(String pinStr) {
        if (pinStr == null)
            return;
        Log.d(TAG, "GTK BT pairType is " + devicePairType);
        switch (devicePairType) {
            case BluetoothDevice.PAIRING_VARIANT_PIN:
                Method method = null;
                byte[] pin = null;
                try {
                    method = Class.forName("android.bluetooth.BluetoothDevice")
                            .getMethod("convertPinToBytes", new Class[] {String.class});
                    pin = (byte[]) method.invoke(bluetoothDevice, pinStr);
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }

                boolean pinSet = bluetoothDevice.setPin(pin);
                Log.d(TAG, "GTK Bluetooth pin : " + pin + ", pinSet : " + pinSet);
                break;
            case BluetoothDevice.PAIRING_VARIANT_PASSKEY_CONFIRMATION:
                if (pinStr.equals("true")) {
                    bluetoothDevice.setPairingConfirmation(true);
                } else if (pinStr.equals("false")) {
                    bluetoothDevice.setPairingConfirmation(false);
                }
                break;
        }
    }

    public void GTKBTUnPair() {
        if (bluetoothDevice == null)
            return;
        Method method = null;
        try {
            method = Class.forName("android.bluetooth.BluetoothDevice")
                    .getMethod("removeBond");
            method.invoke(bluetoothDevice);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    public void GTKBTConnRemoteDevice(String addr) {
        boolean valid = BluetoothAdapter.checkBluetoothAddress(addr);
        if (bluetoothAdapter == null || valid == false)
            return;
        if (bluetoothAdapter.isDiscovering())
            bluetoothAdapter.cancelDiscovery();
        boolean paired = false;
        bluetoothDevice = bluetoothAdapter.getRemoteDevice(addr);

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                Log.d(TAG, "GTK BT pairedDevice address is " + device.getAddress()
                + ", to connect address is " + addr);
                if (addr.equals(device.getAddress())) {
                    paired = true;
                    break;
                }
            }
        }
        if (!paired)
            bluetoothDevice.createBond();
        if (paired) {
            boolean connProxy = bluetoothAdapter.getProfileProxy(mContext,
                    new remoteDeviceServiceListener(), BT_PROFILE_INPUT_DEVICE);
            Log.d(TAG, "GTK BT connProxy is " + connProxy);
        }
    }

    public void GTKBTUnregisterReceiver() {
        try {
            mContext.unregisterReceiver(mReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handEvent(Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "GTK BT receive : " + action);
        if (BluetoothDevice.ACTION_FOUND.equals(action)) {
            BluetoothDevice device = intent.getParcelableExtra(bluetoothDevice.EXTRA_DEVICE);
            int deviceClass = device.getBluetoothClass().getMajorDeviceClass();
            Log.d(TAG, "GTK BT Found name is " + device.getName() + ", addr is " + device.getAddress());
            if (!TextUtils.isEmpty(device.getName())
                    && !TextUtils.isEmpty(device.getAddress())) {
                JSONObject btDeviceInfo = new JSONObject();
                try {
                    btDeviceInfo.put("Name", device.getName());
                    btDeviceInfo.put("Address", device.getAddress());
                    btDeviceInfo.put("Status", "0");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                UnityPlayer.UnitySendMessage("AndroidEvents", "receiveAndroidEvent",
                        Util.BLUETOOTH_REMOTE_DEVICE_INFO + btDeviceInfo.toString());
            }
        } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
            UnityPlayer.UnitySendMessage("AndroidEvents", "receiveAndroidEvent",
                    Util.BLUETOOTH_DISCOVERY_FINISHED + "finished");
        } else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                    BluetoothAdapter.STATE_OFF);
            if (state == BluetoothAdapter.STATE_OFF) {
                UnityPlayer.UnitySendMessage("AndroidEvents", "receiveAndroidEvent",
                        Util.BLUETOOTH_STATE + "disable");
            } else if (state == BluetoothAdapter.STATE_ON) {
                UnityPlayer.UnitySendMessage("AndroidEvents", "receiveAndroidEvent",
                        Util.BLUETOOTH_STATE + "enable");
            }
        } else if (BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED.equals(action)) {
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            int connState = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE,
                    BluetoothAdapter.STATE_DISCONNECTED);
            Log.d(TAG, "GTK BT conn state changed, name is " + device.getName() + ", address is "
                       + device.getAddress() + ", new state is " + connState);
            if (connState == BluetoothAdapter.STATE_DISCONNECTED) {
                UnityPlayer.UnitySendMessage("AndroidEvents", "receiveAndroidEvent",
                        Util.BLUETOOTH_CONN_DISCONNECTED + device.getAddress());
            } else if (connState == BluetoothAdapter.STATE_CONNECTED) {
                UnityPlayer.UnitySendMessage("AndroidEvents", "receiveAndroidEvent",
                        Util.BLUETOOTH_CONN_CONNECTED + device.getAddress());
            }
        } else if (BluetoothDevice.ACTION_PAIRING_REQUEST.equals(action)) {
            bluetoothDevice= intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            devicePairType = intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_VARIANT,
                    BluetoothDevice.ERROR);
            Log.d(TAG, "GTK BT pair request device name is " + bluetoothDevice.getName()
                       + ", address is " + bluetoothDevice.getAddress() + ", pairType is " + devicePairType);
            switch (devicePairType) {
                case BluetoothDevice.PAIRING_VARIANT_PIN:
                    UnityPlayer.UnitySendMessage("AndroidEvents", "receiveAndroidEvent",
                            Util.BLUETOOTH_PAIR_WITH_PIN + "nokey");
                    break;
                case BluetoothDevice.PAIRING_VARIANT_PASSKEY_CONFIRMATION:
                    int pinKey = intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_KEY,
                            BluetoothDevice.ERROR);
                    if (pinKey == BluetoothDevice.ERROR) {
                        Log.e(TAG, "GTK BT Invalid pair confirmation passkey received");
                        return;
                    }
                    String pairKey = String.format(Locale.US, "%06d", pinKey);
                    UnityPlayer.UnitySendMessage("AndroidEvents", "receiveAndroidEvent",
                            Util.BLUETOOTH_PAIR_CONFIRM + pairKey);
                    Log.d(TAG, "GTK BT pair confirm key is " + pairKey);
                    break;
            }
        } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE,
                    BluetoothDevice.BOND_NONE);
            switch (bondState) {
                case BluetoothDevice.BOND_NONE:
                    UnityPlayer.UnitySendMessage("AndroidEvents", "receiveAndroidEvent",
                            Util.BLUETOOTH_BOND_NONE + device.getAddress());
                    break;
                case BluetoothDevice.BOND_BONDING:
                    UnityPlayer.UnitySendMessage("AndroidEvents", "receiveAndroidEvent",
                            Util.BLUETOOTH_BOND_BONDING + device.getAddress());
                    break;
                case BluetoothDevice.BOND_BONDED:
                    UnityPlayer.UnitySendMessage("AndroidEvents", "receiveAndroidEvent",
                            Util.BLUETOOTH_BOND_BONDED + device.getAddress());
                    bluetoothAdapter.getProfileProxy(mContext, new remoteDeviceServiceListener(),
                            BT_PROFILE_INPUT_DEVICE);
                    break;
            }
            Log.d(TAG, "GTK BT bond state changed, name is " + device.getName() + ", address is "
                       + device.getAddress() + ", bondState is " + bondState);
        }
    }

    private final class remoteDeviceServiceListener implements BluetoothProfile.ServiceListener {
        boolean connected = false;

        @Override
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            List<BluetoothDevice> bluetoothDevices = proxy.getConnectedDevices();
            if (!bluetoothDevices.isEmpty()) {
                for (BluetoothDevice device : bluetoothDevices) {
                    if (bluetoothDevice != null) {
                        if (device.getAddress().equals(bluetoothDevice.getAddress())) {
                            if (profile == BT_PROFILE_INPUT_DEVICE) {
                                bluetoothInputDevice = (BluetoothInputDevice) proxy;
                                connected = bluetoothInputDevice.connect(bluetoothDevice);
                                Log.d(TAG, "GTK BT input device connected : " + connected);
                            }
                        }
                    } else {
                        Log.i(TAG, "GTK BT connect device is " + device.getName());
                    }

                    JSONObject btDeviceInfo = new JSONObject();
                    try {
                        btDeviceInfo.put("Name", device.getName());
                        btDeviceInfo.put("Address", device.getAddress());
                        btDeviceInfo.put("Status", "Connected");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    UnityPlayer.UnitySendMessage("AndroidEvents", "receiveAndroidEvent",
                            Util.BLUETOOTH_REMOTE_DEVICE_INFO + btDeviceInfo.toString());
                }
            } else {
                if (profile == BT_PROFILE_INPUT_DEVICE) {
                    bluetoothInputDevice = (BluetoothInputDevice) proxy;
                    connected = bluetoothInputDevice.connect(bluetoothDevice);
                }
            }
        }

        @Override
        public void onServiceDisconnected(int profile) {

        }
    };
}
