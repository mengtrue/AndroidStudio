package com.unisoc.wcnutils;

import android.os.Message;

public class State {
    protected State() {
    }

    public void enter() {
    }

    public void exit() {
    }

    public boolean processMessage(Message msg) {
        return false;
    }

    public String getName() {
        String name = getClass().getName();
        int lastDollar = name.lastIndexOf('$');
        return name.substring(lastDollar + 1);
    }
}
