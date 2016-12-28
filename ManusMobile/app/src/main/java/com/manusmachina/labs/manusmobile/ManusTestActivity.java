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

package com.manusmachina.labs.manusmobile;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.TabHost;
import android.widget.TextView;

import com.manusmachina.labs.manussdk.*;


public class ManusTestActivity extends ActionBarActivity implements Manus.OnGloveChangedListener, TabHost.OnTabChangeListener {
    /**
     * The serialization (saved instance state) Bundle key representing the
     * current dropdown position.
     */
    private static final String STATE_SELECTED_NAVIGATION_ITEM = "selected_navigation_item";

    private long timestamp = System.currentTimeMillis();
    private ManusTestActivity mScope = this;
    private Manus.GloveBinder mBinder = null;
    private Glove.HAND mSelectedGlove = Glove.HAND.LEFT;
    private Menu mMenu = null;

    private class vibrationListener implements SeekBar.OnSeekBarChangeListener {
        public void onProgressChanged(SeekBar seekBar, int progress,
                                      boolean fromUser) {
            TextView textview = (TextView)findViewById(R.id.textView6);
            textview.setText("Vibration: " + progress + "%");
            mBinder.getGlove(mSelectedGlove).setVibration((float)progress/100.0f);
        }

        public void onStartTrackingTouch(SeekBar seekBar) {}

        public void onStopTrackingTouch(SeekBar seekBar) {}
    }


    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            mBinder = (Manus.GloveBinder) service;
            mBinder.addOnGloveChangedListener(mScope);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manus_test);

        // Bind to Manus service
        Intent intent = new Intent(this, Manus.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        // Setup tabs
        TabHost tabs = (TabHost)findViewById(R.id.tabHost);
        tabs.setup();
        TabHost.TabSpec leftTab = tabs.newTabSpec("left");
        leftTab.setIndicator("Left Glove");
        leftTab.setContent(R.id.left);
        tabs.addTab(leftTab);
        TabHost.TabSpec rightTab = tabs.newTabSpec("right");
        rightTab.setIndicator("Right Glove");
        rightTab.setContent(R.id.right);
        tabs.addTab(rightTab);
        tabs.setOnTabChangedListener(this);

        // Setup Vibration test

        SeekBar vibration =(SeekBar) findViewById(R.id.vibration);
        vibration.setOnSeekBarChangeListener(new vibrationListener());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBinder = null;
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);

        savedInstanceState.putSerializable(STATE_SELECTED_NAVIGATION_ITEM, mSelectedGlove);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        // Restore the previously serialized current dropdown position.
        if (savedInstanceState.containsKey(STATE_SELECTED_NAVIGATION_ITEM)) {
            mSelectedGlove = (Glove.HAND)savedInstanceState.getSerializable(STATE_SELECTED_NAVIGATION_ITEM);
            if(mSelectedGlove == Glove.HAND.LEFT)
                ((TabHost)findViewById(R.id.tabHost)).setCurrentTab(0);
            if(mSelectedGlove == Glove.HAND.RIGHT)
                ((TabHost)findViewById(R.id.tabHost)).setCurrentTab(1);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_manus_test, menu);
        mMenu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.calibrate_imu) {
            mBinder.getGlove(mSelectedGlove).calibrate(true, true, false);
            return true;
        } else if (id == R.id.calibrate_fingers) {
            mBinder.getGlove(mSelectedGlove).calibrate(false, false, true);
            ProgressDialog.show(this, "Finger Calibration", "Open and close the hands to calibrate.", true, true, new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialogInterface) {
                    mBinder.getGlove(mSelectedGlove).calibrate(false, false, false);
                }
            });
            return true;
        } else if (id == R.id.calibrate_hand) {
            Glove glove = mBinder.getGlove(mSelectedGlove);
            if(glove != null)
                glove.setHandedness(!item.isChecked() ? Glove.HAND.RIGHT : Glove.HAND.LEFT);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void OnGloveChanged(Glove.HAND hand, Glove glove) {
        if (glove.getHandedness() != mSelectedGlove)
            return;

        Glove.Vector euler = glove.getEuler();
        Glove.Vector degrees = euler.ToDegrees();
        float[] fingers = glove.getFingers();

        SeekBar yaw = (SeekBar)findViewById(R.id.yaw);
        SeekBar pitch = (SeekBar)findViewById(R.id.pitch);
        SeekBar roll = (SeekBar)findViewById(R.id.roll);
        SeekBar[] fingerBars = {
                (SeekBar)findViewById(R.id.thumb),
                (SeekBar)findViewById(R.id.index),
                (SeekBar)findViewById(R.id.middle),
                (SeekBar)findViewById(R.id.ring),
                (SeekBar)findViewById(R.id.pinky)
        };
        SeekBar interval = (SeekBar)findViewById(R.id.interval);

        roll.setProgress((int)degrees.x + 180);
        pitch.setProgress((int)degrees.y + 90);
        yaw.setProgress((int) degrees.z + 180);
        for (int i = 0; i < 5; i++)
            fingerBars[i].setProgress((int)(fingers[i] * 255.0f));
        interval.setProgress((int)(System.currentTimeMillis() - timestamp));

        if(mMenu != null)
            mMenu.findItem(R.id.calibrate_hand).setChecked(glove.getHandedness() == Glove.HAND.RIGHT);

        timestamp = System.currentTimeMillis();
    }

    @Override
    public void onTabChanged(String s) {
        mSelectedGlove = (s == "right") ? Glove.HAND.RIGHT : Glove.HAND.LEFT;
    }
}
