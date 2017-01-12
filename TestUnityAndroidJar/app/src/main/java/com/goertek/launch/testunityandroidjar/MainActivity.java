package com.goertek.launch.testunityandroidjar;

import android.os.Bundle;
import android.widget.Toast;

import com.unity3d.player.UnityPlayerActivity;

/*
   若继承 UnityPlayerActivity，需要注释掉 setContentView()
   若不继承 UnityPlayerActivity，则需要 setContentView( new new UnityPlayer(this) )
 */
public class MainActivity extends UnityPlayerActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_main);
    }

    public void showTestToast() {
        Toast.makeText(getApplicationContext(), "This is only for test", Toast.LENGTH_LONG);
    }
}
