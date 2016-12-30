package com.goertek.hapticarrow;


import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Log;
import android.view.Display;
import android.view.Window;
import android.view.WindowManager;

import com.goertek.hapticble.*;

public class MainActivity extends Activity implements GtkBLE.OnHapticChangedListener {

    public static MainActivity instance;
    public static Vibrator vibrator;  //haptic
//    public static  Haptics haptics;
//    com.android.server.HapticsService

    private static String TAG_Gtk = "GoerTek";
    private GtkBLE.GtkHapticBinder mBinder = null;

    /**
     * Define callbacks for service binding, passed to bindService()
     */
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            //bound to LocalService, cast the IBinder and get LocalService instance
            mBinder = (GtkBLE.GtkHapticBinder) service;
            mBinder.addOnHapticChangedListener(instance);
            Log.d(TAG_Gtk, "GtkBLE service connected");

            if (mBinder.getGtkHaptic() == null) {
                Log.d(TAG_Gtk, "no haptic");
                Intent intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
                startActivity(intent);
            } else {
                showSurfaceView();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.e(TAG_Gtk, "GtkBLE service disconnected");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;

        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);  //haptic
//        haptics = (Haptics) getSystemService(HAPTICS_SERVICE);

        // Bind to GtkBLE service
        Intent intent = new Intent(this, GtkBLE.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

    }

    @Override
    public void OnHapticChanged(GtkHaptic haptic) {
        Log.i(TAG_Gtk, "OnHapticChanged");
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.d(TAG_Gtk, "onResume, mBinder = " + mBinder);

        if (mBinder == null)
            return;
        if (mBinder.getGtkHaptic() == null) {
            Log.d(TAG_Gtk, "no haptic");
            Intent intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
            startActivity(intent);
        } else {
            showSurfaceView();
        }
    }

    void showSurfaceView() {
        //设置全屏
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        Display display = getWindowManager().getDefaultDisplay();
        //显示自定义的SurfaceView视图
        setContentView(new MySurfaceView(this, display.getHeight(), display.getWidth(), mBinder));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBinder = null;
    }
}
