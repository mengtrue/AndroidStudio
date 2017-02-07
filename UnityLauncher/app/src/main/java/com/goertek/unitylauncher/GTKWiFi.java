package com.goertek.unitylauncher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.text.TextUtils;
import android.util.Log;

import com.unity3d.player.UnityPlayer;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Created by chaw.meng on 2017/2/6.
 */

public class GTKWiFi {
    private static final String TAG = "GTKWiFi";

    private Context mContext;
    private WifiManager mWifiManager;
    private IntentFilter mfilter;
    private static boolean disconnectReceived = true;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            handEvent(intent);
        }
    };

    private static final int DISABLE_AUTH_FAILURE = 3;

    public GTKWiFi(Context context) {
        mContext = context;
        mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);

        mfilter = new IntentFilter();
        mfilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        mfilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        mfilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        mfilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        mContext.registerReceiver(mReceiver, mfilter);
    }

    public String getWifiState()
    {
        String wifiState;
        int state = mWifiManager.getWifiState();
        switch (state) {
            case 0:
                wifiState = "DISABLING";
                break;
            case 1:
                wifiState = "DISABLED";
                break;
            case 2:
                wifiState = "ENABLING";
                break;
            case 3:
                wifiState = "ENABLED";
                break;
            case 4:
                wifiState = "UNKNOWN";
                break;
            default:
                wifiState = "UNKNOWN";
                break;
        }
        return wifiState;
    }

    public void GTKEnableWifi(boolean enable) {
        Log.i(TAG, "GTK Enable WiFi " + enable);
        mWifiManager.setWifiEnabled(enable);
    }

    public void GTKScanWifi() {
        Log.i(TAG, "GTK Scan Wifi");
        if (mWifiManager.isWifiEnabled())
            mWifiManager.startScan();
    }

    public void GTKGetWifiScanResults() {
        List<ScanResult> results = mWifiManager.getScanResults();
        if (results.isEmpty()) {
            UnityPlayer.UnitySendMessage("Main Camera", "WiFiScanResult", "null");
            return;
        }

        List<ScanResult> wifiList = new ArrayList<ScanResult>();

        // delete same SSID APs
        for (ScanResult result : results) {
            boolean same = false;
            if (!TextUtils.isEmpty(result.SSID)
                    && !result.capabilities.contains("WEP")
                    && !result.capabilities.contains("EAP")) {
                for (ScanResult ap : wifiList) {
                    if (ap.SSID.equals(result.SSID)) {
                        same = true;
                        break;
                    }
                }
                if (!same)
                    wifiList.add(result);
            }
        }

        // sort by RSSI
        Collections.sort(wifiList, new Comparator<ScanResult>() {
            @Override
            public int compare(ScanResult lhs, ScanResult rhs) {
                return rhs.level - lhs.level;
            }
        });

        // check WiFi AccessPoint saved or not
        List<WifiConfiguration> wifiConfigs = mWifiManager.getConfiguredNetworks();
        ConnectivityManager conn = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = conn.getActiveNetworkInfo();
        WifiInfo wifiInfo = null;
        String ssid = null;
        if (activeNetworkInfo != null) {
            if (activeNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                wifiInfo = mWifiManager.getConnectionInfo();
                ssid = wifiInfo.getSSID().replace("\"", "");
            }
        }
        try {
            JSONArray resultArray = new JSONArray();
            for (int i = 0; i < wifiList.size(); i++) {
                ScanResult result = wifiList.get(i);
                JSONObject apObject = new JSONObject();
                apObject.put("SSID", result.SSID.replace("\"", ""));
                apObject.put("singalStrength",
                        Integer.toString(WifiManager.calculateSignalLevel(result.level, 4)));
                if (result.capabilities.contains("WPA"))
                    apObject.put("security", result.capabilities);
                else
                    apObject.put("security", "none");

                boolean saved = false;
                if (wifiConfigs != null) {
                    for (WifiConfiguration config : wifiConfigs) {
                        if (result.SSID.replace("\"", "")
                                .equals(config.SSID.replace("\"", ""))) {
                            apObject.put("saved", "true");
                            saved = true;
                            break;
                        }
                    }
                }
                if (!saved)
                    apObject.put("saved", "false");

                if (ssid != null && !ssid.isEmpty()
                        && result.SSID.replace("\"", "").equals(ssid))
                    apObject.put("connected", "true");
                else
                    apObject.put("connected", "false");

                resultArray.put(apObject);
            }
            UnityPlayer.UnitySendMessage("AndroidEvents", "receiveAndroidEvent",
                    Util.WIFI_SCAN_RESULT_LIST + resultArray.toString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        for (ScanResult result : wifiList)
            Log.d(TAG, result.SSID + ", RSSI: " + result.level);
    }

    public void forget(String ssid) {
        WifiConfiguration wifiConfig = getWifiConfig(ssid);
        if (wifiConfig != null) {
            mWifiManager.disconnect();
            mWifiManager.disableNetwork(wifiConfig.networkId);
            mWifiManager.removeNetwork(wifiConfig.networkId);
            mWifiManager.saveConfiguration();
        }
    }

    private WifiConfiguration getWifiConfig(String ssid) {
        WifiConfiguration wifiConfig = null;
        List<WifiConfiguration> wifiConfigs = mWifiManager.getConfiguredNetworks();
        for (WifiConfiguration config : wifiConfigs) {
            if (config.SSID.equals("\"" + ssid + "\"")) {
                wifiConfig = config;
                break;
            }
        }
        return wifiConfig;
    }

    private void disconnect() {
        ConnectivityManager conn = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = conn.getActiveNetworkInfo();
        if (activeNetworkInfo != null) {
            if (activeNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                if (activeNetworkInfo.isConnected())
                    mWifiManager.disconnect();
            }
        }
    }

    public void connectWiFi(String ssid, String pwd, String security) {
        // check SSID saved or not when pwd is empty
        if (pwd.isEmpty()) {
            List<WifiConfiguration> wifiConfigs = mWifiManager.getConfiguredNetworks();
            for (WifiConfiguration config : wifiConfigs) {
                if (config.SSID.replace("\"", "").equals(ssid)) {
                    disconnect();
                    mWifiManager.enableNetwork(config.networkId, true);
                    return;
                }
            }
        }
        if (!security.equals("none") && pwd.isEmpty())
            return;

        WifiConfiguration config = new WifiConfiguration();
        config.SSID = "\"" + ssid + "\"";

        // check same SSID config exist, if exist, first remove
        WifiConfiguration tmpConfig = getWifiConfig(ssid);
        if (tmpConfig != null)
            mWifiManager.removeNetwork(tmpConfig.networkId);

        if (security.equals("none"))
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        else if (security.equals("WPA")) {
            config.preSharedKey = "\"" + pwd + "\"";
            config.status = WifiConfiguration.Status.ENABLED;
            config.allowedGroupCiphers.set(WifiConfiguration.AuthAlgorithm.OPEN);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            config.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
            config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
        } else if (security.equals("WEP")) {
            config.wepKeys[0] = "\"" + pwd + "\"";
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.wepTxKeyIndex = 0;
        }

        int networkId = mWifiManager.addNetwork(config);
        if (networkId < 0) {
            UnityPlayer.UnitySendMessage("Main Camera", "connectSSID", "connect failed for networkId");
            return;
        }

        disconnect();
        mWifiManager.enableNetwork(networkId, true);
        disconnectReceived = false;
    }

    private void handEvent(Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "receive intent : " + action);
        if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action)) {
            GTKGetWifiScanResults();
        } else if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
            NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            if (networkInfo == null)
                return;
            WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
            String ssid = wifiInfo.getSSID().replace("\"", "");
            if (NetworkInfo.DetailedState.CONNECTED == networkInfo.getDetailedState()) {
                UnityPlayer.UnitySendMessage("AndroidEvents", "receiveAndroidEvent",
                        Util.WIFI_CONNECTED + ssid);
                disconnectReceived = false;
            } else if (NetworkInfo.DetailedState.AUTHENTICATING == networkInfo.getDetailedState()) {
                UnityPlayer.UnitySendMessage("AndroidEvents", "receiveAndroidEvent",
                        Util.WIFI_AUTHENTICATING + ssid);
            } else if (NetworkInfo.DetailedState.OBTAINING_IPADDR == networkInfo.getDetailedState()) {
                UnityPlayer.UnitySendMessage("AndroidEvents", "receiveAndroidEvent",
                        Util.WIFI_OBTAINING_IPADDR + ssid);
            }
        } else if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {
            int state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);
            Log.d(TAG, "WiFi State is " + state);
            switch (state) {
                case WifiManager.WIFI_STATE_DISABLED:
                    UnityPlayer.UnitySendMessage("AndroidEvents", "receiveAndroidEvent",
                            Util.WIFI_STATE + "disabled");
                    break;
                case WifiManager.WIFI_STATE_DISABLING:
                    UnityPlayer.UnitySendMessage("Main Camera", "wifiState", "disabling");
                    break;
                case WifiManager.WIFI_STATE_ENABLED:
                    GTKScanWifi();
                    UnityPlayer.UnitySendMessage("AndroidEvents", "receiveAndroidEvent",
                            Util.WIFI_STATE + "enabled");
                    break;
                case WifiManager.WIFI_STATE_ENABLING:
                    UnityPlayer.UnitySendMessage("Main Camera", "wifiState", "enabling");
                    break;
                case WifiManager.WIFI_STATE_UNKNOWN:
                    UnityPlayer.UnitySendMessage("Main Camera", "wifiState", "unknown");
                    break;
            }
        } else if (WifiManager.SUPPLICANT_STATE_CHANGED_ACTION.equals(action)) {
            SupplicantState state = intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE);
            Log.d(TAG, "WiFi Supplicant state is " + state.toString());
            if (state == SupplicantState.DISCONNECTED) {
                List<WifiConfiguration> wifiConfigs = mWifiManager.getConfiguredNetworks();
                if (wifiConfigs == null || disconnectReceived == true)
                    return;
                WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
                String ssid = wifiInfo.getSSID().replace("\"", "");
                int disabledReason = -1;
                for (WifiConfiguration config : wifiConfigs) {
                    if (config.SSID.replace("\"", "").equals(ssid)) {
                        try {
                            Field field = WifiConfiguration.class.getDeclaredField("disableReason");
                            field.setAccessible(true);
                            disabledReason = field.getInt(config);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    }
                }
                Log.d(TAG, "WIFI Disconnected, disableReason = " + disabledReason);
                if (disabledReason == DISABLE_AUTH_FAILURE)
                    UnityPlayer.UnitySendMessage("AndroidEvents", "receiveAndroidEvent",
                            Util.WIFI_CONN_PASS_ERROR + ssid);
                else
                    UnityPlayer.UnitySendMessage("AndroidEvents", "receiveAndroidEvent",
                            Util.WIFI_CONN_FAILED + ssid);
                disconnectReceived = true;
            }
        }
    }
}
