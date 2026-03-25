package com.unisoc.wcntool;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.net.wifi.WifiManager;

public class WcnInjector {
    private static WcnInjector sInjector;
    private WifiManager mWifiManager;
    private BluetoothAdapter mBtAdapter;

    public static WcnInjector getInstance() {
        return sInjector;
    }

    public WcnInjector(Context context) {
        if (sInjector != null) return;
        mWifiManager = context.getSystemService(WifiManager.class);
        BluetoothManager bluetoothManager = context.getSystemService(BluetoothManager.class);
        if (bluetoothManager != null) mBtAdapter = bluetoothManager.getAdapter();
        sInjector = this;
    }

    public WifiManager getWifiManager() { return mWifiManager; }
    public BluetoothAdapter getBtAdapter() { return mBtAdapter; }

}
