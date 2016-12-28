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

import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;


/**
 * Created by Andre on 12-10-2015.
 */
public class Unity extends ContextWrapper {

    /**
     * Manus return values
     */
    int ERROR = -1;
    int SUCCESS = 0;
    int INVALID_ARGUMENT = 1;
    int OUT_OF_RANGE = 2;
    int DISCONNECTED = 3;

    private Manus.GloveBinder mBinder;
    private Context c;

    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            mBinder = (Manus.GloveBinder) service;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };

    public float[] getData(int iHand) {
        Glove.HAND hand;
        hand = (iHand == 0) ? Glove.HAND.LEFT : Glove.HAND.RIGHT;
        if (mBinder != null) {
            Glove glove = mBinder.getGlove(hand);
            if (glove != null) {
                float data[] = new float[15];
                System.arraycopy(glove.getAcceleration().ToArray(), 0, data, 0, 3);
                System.arraycopy(glove.getEuler().ToArray(), 0, data, 3, 3);
                System.arraycopy(glove.getQuaternion().ToArray(), 0, data, 6, 4);
                System.arraycopy(glove.getFingers(), 0, data, 10, 5);

                return data;
            } else {
                Log.d("MANUS", "Unable to obtain glove.");
                return new float[0];
            }
        } else {
            Log.d("MANUS", "Unable to obtain binder.");
            // returning float[0] as returning null does not play well with Unity's Android helper
            return new float[0];
        }
    }

    public int setHandedness(int iHand, boolean isRightHand) {
        Glove.HAND hand;
        Glove.HAND newValue;
        hand = (iHand == 0) ? Glove.HAND.LEFT : Glove.HAND.RIGHT;
        newValue = (isRightHand) ? Glove.HAND.RIGHT : Glove.HAND.LEFT;
        if (mBinder != null) {
            Glove glove = mBinder.getGlove(hand);
            if (glove != null) {
                if (glove.setHandedness(newValue)) return SUCCESS;
            }
        }
        return ERROR;
    }

    public int setVibration(int iHand,float fPower) {
        Glove.HAND hand;
        hand = (iHand == 0) ? Glove.HAND.LEFT : Glove.HAND.RIGHT;
        if (mBinder != null) {
            Glove glove = mBinder.getGlove(hand);
            if (glove != null) {
                if (glove.setVibration(fPower)) return SUCCESS;
            }
        }
        return ERROR;
    }

    public int calibrate(int iHand, boolean gyro, boolean accel, boolean fingers) {
        Glove.HAND hand;
        hand = (iHand == 0) ? Glove.HAND.LEFT : Glove.HAND.RIGHT;
        if (mBinder != null) {
            Glove glove = mBinder.getGlove(hand);
            if (glove != null) {
                if (glove.calibrate(gyro,accel,fingers)) return SUCCESS;
            }
        }
        return ERROR;
    }

    public Unity(Context context) {
        super(context);
        c = context;
        Intent intent = new Intent(this, Manus.class);
        c.startService(intent);
        c.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }
}


