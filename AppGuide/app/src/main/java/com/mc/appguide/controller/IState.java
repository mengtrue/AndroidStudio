package com.mc.appguide.controller;

import android.os.Message;

public interface IState {
    // called when a state is entered
    void enter(Enum event);

    // called when a state is exited
    void exit(Enum event);

    // called when a message to be processed by the stateMachine
    boolean processMessage(Message msg);

    // name of state
    String getName();
}
