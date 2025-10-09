package com.mc.appguide.utils;

import android.database.DataSetObservable;
import android.database.DataSetObserver;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import java.util.HashMap;
import java.util.Map;

public class StateMachine implements Handler.Callback{
    private IState currentState;
    private final Map<IState, Map<Enum, IState>> transitions = new HashMap<>();
    private final Handler mHandler;

    private final DataSetObservable mObservable = new DataSetObservable();
    private DataSetObserver mObserver;

    public StateMachine() {
        this(null);
    }

    public StateMachine(String threadName) {
        HandlerThread stateMachineThread = new HandlerThread(
                threadName != null ? threadName : StateMachine.class.getSimpleName());
        stateMachineThread.start();
        mHandler = new Handler(stateMachineThread.getLooper(), this);
    }

    public void registerStateObserver(DataSetObserver observer) {
        mObserver = observer;
        mObservable.registerObserver(mObserver);
    }

    public void start(IState initialState, Enum event) {
        updateCurrentState(initialState, event);
    }

    public void addTransition(IState srcState, Enum event, IState desState) {
        transitions.computeIfAbsent(srcState, k->new HashMap<>())
                .put(event, desState);
    }

    public void sendMessage(int what) {
        if (mHandler != null) {
            mHandler.sendMessage(Message.obtain(mHandler, what));
        }
    }

    public void sendMessageDelayed(int what, int arg1, long delayMillis) {
        if (mHandler != null) {
            mHandler.sendMessageDelayed(Message.obtain(mHandler,
                    what, arg1, 0), delayMillis);
        }
    }


    public boolean transitionState(Enum event) {
        Map<Enum, IState> eventMap = transitions.get(currentState);
        if (eventMap != null && eventMap.containsKey(event)) {
            currentState.exit(event);
            updateCurrentState(eventMap.get(event), event);
            return true;
        }
        return false;
    }

    public IState getCurrentState() { return currentState;}

    public void quit() {
        mObservable.unregisterObserver(mObserver);
    }

    private void updateCurrentState(IState state, Enum event) {
        currentState = state;
        currentState.enter(event);
        mObservable.notifyChanged();
    }

    @Override
    public boolean handleMessage(Message msg) {
        if (currentState != null) {
            return currentState.processMessage(msg);
        }
        return false;
    }
}
