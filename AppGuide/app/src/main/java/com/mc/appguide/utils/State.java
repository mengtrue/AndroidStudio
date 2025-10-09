package com.mc.appguide.utils;

import android.os.Message;

public class State implements IState {
    @Override
    public void enter(Enum event) {}

    @Override
    public void exit(Enum event) {}

    @Override
    public boolean processMessage(Message msg) {
        return false;
    }

    @Override
    public String getName() { return ""; }
}
