package com.goertek.hapticarrow;


import android.app.Activity;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.Display;
import android.view.Window;
import android.view.WindowManager;


public class MainActivity extends Activity {

    public static MainActivity instance;
    public static Vibrator vibrator;  //haptic
//    public static  Haptics haptics;
//    com.android.server.HapticsService

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;

        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);  //haptic
//        haptics = (Haptics) getSystemService(HAPTICS_SERVICE);


        //设置全屏
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        Display display = getWindowManager().getDefaultDisplay();
        //显示自定义的SurfaceView视图
        setContentView(new MySurfaceView(this, display.getHeight(), display.getWidth()));

    }
}
