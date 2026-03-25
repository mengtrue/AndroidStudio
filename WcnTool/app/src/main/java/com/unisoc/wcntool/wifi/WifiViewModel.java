package com.unisoc.wcntool.wifi;

import android.net.wifi.WifiManager;

import androidx.lifecycle.ViewModel;

import com.unisoc.wcntool.WcnInjector;

public class WifiViewModel extends ViewModel {
    private WifiManager mWifiManager;

    public WifiViewModel() {
        mWifiManager = WcnInjector.getInstance().getWifiManager();
    }
}