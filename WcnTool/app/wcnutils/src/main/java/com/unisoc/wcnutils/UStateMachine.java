package com.unisoc.wcnutils;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class UStateMachine {
    /** Name of the state machine and used as logging tag */
    private String mName;

    /** Message.what value when quitting */
    private static final int SM_QUIT_CMD = -1;

    /** Message.what value when initializing */
    private static final int SM_INIT_CMD = -2;

    /**
     * Convenience constant that maybe returned by processMessage
     * to indicate the message was processed and is not to be
     * processed by parent states
     */
    public static final boolean HANDLED = true;

    /**
     * Convenience constant that maybe returned by processMessage
     * to indicate the message was NOT processed and is to be
     * processed by parent states
     */
    public static final boolean NOT_HANDLED = false;

    private SmHandler mSmHandler;
    private HandlerThread mSmThread;

    private static class SmHandler extends Handler {
        /** true if StateMachine has quit */
        private boolean mHasQuit = false;

        /** The SmHandler object, identifies that message is internal */
        private final Object mSmHandlerObj = new Object();

        /** The current message */
        private Message mMsg;

        /** true if construction of the state machine has not been completed */
        private boolean mIsConstructionCompleted;

        /** Stack used to manage the current hierarchy of states */
        private StateInfo[] mStateStack;

        /** Top of mStateStack */
        private int mStateStackTopIndex = -1;

        /** A temporary stack used to manage the state stack */
        private StateInfo[] mTempStateStack;

        /** The top of the mTempStateStack */
        private int mTempStateStackCount;

        /** State used when state machine is quitting */
        private final QuittingState mQuittingState = new QuittingState();

        /** Reference to the StateMachine */
        private UStateMachine mSm;

        /** Information about a state. Used to maintain the hierarchy.*/
        private static class StateInfo {
            final State state;

            /** The parent of this state, null if there is no parent */
            final StateInfo parentStateInfo;

            /** True when the state has been entered and on the stack */
            // Note that this can be initialized on a different thread than it's used as long
            // as it's only used on one thread. The reason is that it's initialized to false,
            // which is also the default value for a boolean, so if the member is seen uninitialized
            // then it's seen with the default value which is also false.
            boolean active = false;

            StateInfo(final State state, final StateInfo parent) {
                this.state = state;
                this.parentStateInfo = parent;
            }

            /** Convert StateInfo to string */
            @Override
            public String toString() {
                return "state=" + state.getName() + ",active=" + active + ",parent="
                        + ((parentStateInfo == null) ? "null" : parentStateInfo.state.getName());
            }
        }

        /** The map of all of the states in the state machine */
        private final HashMap<State, StateInfo> mStateInfo = new HashMap<>();

        /** The initial state that will process the first message */
        private State mInitialState;

        /** The destination state when transitionTo has been invoked */
        private State mDestState;

        /** The list of deferred messages */
        private final ArrayList<Message> mDeferredMessages = new ArrayList<>();

        /** State entered when a valid quit message is handled. */
        private static class QuittingState extends State {
            @Override
            public boolean processMessage(Message msg) {
                return NOT_HANDLED;
            }
        }

        /**
         * Handle messages sent to the state machine by calling
         * the current state's processMessage. It also handles
         * the enter/exit calls and placing any deferred messages
         * back onto the queue when transitioning to a new state.
         */
        @Override
        public final void handleMessage(Message msg) {
            if (!mHasQuit) {
                // Save the current message
                /* Copy the "msg" to "mMsg" as "msg" will be recycled */
                mMsg = obtainMessage();
                mMsg.copyFrom(msg);
                if (mIsConstructionCompleted || (msg.what == SM_QUIT_CMD)) {
                    // Normal path
                    processMsg(msg);
                } else if (msg.what == SM_INIT_CMD && msg.obj == mSmHandlerObj) {
                    // Initial one time path.
                    mIsConstructionCompleted = true;
                    invokeEnterMethods(0);
                } else {
                    throw new RuntimeException("UStateMachine.handleMessage: "
                            + "The start method not called, received msg: " + msg);
                }
                performTransitions();
            }
        }

        /** Do any transitions */
        private void performTransitions() {
            State destState = mDestState;
            if (destState != null) {
                /** Process the transitions including transitions in the enter/exit methods */
                while (true) {
                    /*
                     * Determine the states to exit and enter and return the
                     * common ancestor state of the enter/exit states. Then
                     * invoke the exit methods then the enter methods.
                     */
                    StateInfo commonStateInfo = setupTempStateStackWithStatesToEnter(destState);
                    invokeExitMethods(commonStateInfo);
                    int stateStackEnteringIndex = moveTempStateStackToStateStack();
                    invokeEnterMethods(stateStackEnteringIndex);

                    /*
                     * Since we have transitioned to a new state we need to have
                     * any deferred messages moved to the front of the message queue
                     * so they will be processed before any other messages in the
                     * message queue.
                     */
                    moveDeferredMessageAtFrontOfQueue();

                    if (destState != mDestState) {
                        // A new mDestState so continue looping
                        destState = mDestState;
                    } else {
                        // No change in mDestState so we're done
                        break;
                    }
                }
                mDestState = null;
            }

            /*
             * After processing all transitions check and
             * see if the last transition was to quit or halt.
             */
            if (destState != null) {
                if (destState == mQuittingState) {
                    /** Call onQuitting to let subclasses cleanup. */
                    mSm.onQuitting();
                    cleanupAfterQuitting();
                }
            }
        }

        /** Cleanup all the static variables and the looper after the SM has been quit. */
        private void cleanupAfterQuitting() {
            if (mSm.mSmThread != null) {
                // If we made the thread then quit looper which stops the thread.
                getLooper().quit();
                mSm.mSmThread = null;
            }

            mSm.mSmHandler = null;
            mSm = null;
            mMsg = null;
            mStateStack = null;
            mTempStateStack = null;
            mStateInfo.clear();
            mInitialState = null;
            mDestState = null;
            mDeferredMessages.clear();
            mHasQuit = true;
        }

        /** Complete the construction of the state machine. */
        private void completeConstruction() {
            /** Determine the maximum depth of the state hierarchy so we can allocate the state stacks. */
            int maxDepth = 0;
            for (StateInfo si : mStateInfo.values()) {
                int depth = 0;
                for (StateInfo i = si; i != null; depth++) {
                    i = i.parentStateInfo;
                }
                if (maxDepth < depth) {
                    maxDepth = depth;
                }
            }

            mStateStack = new StateInfo[maxDepth];
            mTempStateStack = new StateInfo[maxDepth];
            setupInitialStateStack();

            // Sending SM_INIT_CMD message to invoke enter methods asynchronously
            sendMessageAtFrontOfQueue(obtainMessage(SM_INIT_CMD, mSmHandlerObj));
        }

        /**
         * Process the message. If the current state doesn't handle
         * it, call the states parent and so on.
         */
        private void processMsg(Message msg) {
            StateInfo curStateInfo = mStateStack[mStateStackTopIndex];

            if (isQuit(msg)) {
                transitionTo(mQuittingState);
            } else {
                while (!curStateInfo.state.processMessage(msg)) {
                    // Not processed
                    curStateInfo = curStateInfo.parentStateInfo;
                    if (curStateInfo == null) {
                        break;
                    }
                }
            }
        }
        /** Call the exit method for each state from the top of stack up to the common ancestor state. */
        private void invokeExitMethods(StateInfo commonStateInfo) {
            while ((mStateStackTopIndex >= 0)
                    && (mStateStack[mStateStackTopIndex] != commonStateInfo)) {
                State curState = mStateStack[mStateStackTopIndex].state;
                curState.exit();
                mStateStack[mStateStackTopIndex].active = false;
                mStateStackTopIndex -= 1;
            }
        }
        /** Invoke the enter method starting at the entering index to top of state stack */
        private void invokeEnterMethods(int stateStackEnteringIndex) {
            for (int i = stateStackEnteringIndex; i <= mStateStackTopIndex; i++) {
                mStateStack[i].state.enter();
                mStateStack[i].active = true;
            }
        }
        /** Move the deferred message to the front of the message queue. */
        private void moveDeferredMessageAtFrontOfQueue() {
            /*
             * The oldest messages on the deferred list must be at
             * the front of the queue so start at the back, which
             * as the most resent message and end with the oldest
             * messages at the front of the queue.
             */
            for (int i = mDeferredMessages.size() - 1; i >= 0; i--) {
                Message curMsg = mDeferredMessages.get(i);
                sendMessageAtFrontOfQueue(curMsg);
            }
            mDeferredMessages.clear();
        }
        /**
         * Move the contents of the temporary stack to the state stack
         * reversing the order of the items on the temporary stack as
         * they are moved.
         *
         * @return index into mStateStack where entering needs to start
         */
        private int moveTempStateStackToStateStack() {
            int startingIndex = mStateStackTopIndex + 1;
            int i = mTempStateStackCount - 1;
            int j = startingIndex;
            while (i >= 0) {
                mStateStack[j] = mTempStateStack[i];
                j += 1;
                i -= 1;
            }
            mStateStackTopIndex = j - 1;
            return startingIndex;
        }
        /**
         * Set up the mTempStateStack with the states we are going to enter.
         * This is found by searching up the destState's ancestors for a
         * state that is already active i.e. StateInfo.active == true.
         * The destState and all of its inactive parents will be on the
         * TempStateStack as the list of states to enter.
         *
         * @return StateInfo of the common ancestor for the destState and
         * current state or null if there is no common parent.
         */
        private StateInfo setupTempStateStackWithStatesToEnter(State destState) {
            /*
             * Search up the parent list of the destination state for an active
             * state. Use a do while() loop as the destState must always be entered
             * even if it is active. This can happen if we are exiting/entering
             * the current state.
             */
            mTempStateStackCount = 0;
            StateInfo curStateInfo = mStateInfo.get(destState);
            do {
                mTempStateStack[mTempStateStackCount++] = curStateInfo;
                curStateInfo = curStateInfo.parentStateInfo;
            } while ((curStateInfo != null) && !curStateInfo.active);
            return curStateInfo;
        }
        /** Initialize StateStack to mInitialState. */
        private void setupInitialStateStack() {
            StateInfo curStateInfo = mStateInfo.get(mInitialState);
            for (mTempStateStackCount = 0; curStateInfo != null; mTempStateStackCount++) {
                mTempStateStack[mTempStateStackCount] = curStateInfo;
                curStateInfo = curStateInfo.parentStateInfo;
            }
            // Empty the StateStack
            mStateStackTopIndex = -1;
            moveTempStateStackToStateStack();
        }

        private Message getCurrentMessage() {
            return mMsg;
        }

        private State getCurrentState() {
            return mStateStack[mStateStackTopIndex].state;
        }

        /**
         * Add a new state to the state machine. Bottom up addition
         * of states is allowed but the same state may only exist
         * in one hierarchy.
         *
         * @param state the state to add
         * @param parent the parent of state
         * @return stateInfo for this state
         */
        private StateInfo addState(State state, State parent) {
            StateInfo parentStateInfo = null;
            if (parent != null) {
                parentStateInfo = mStateInfo.get(parent);
                if (parentStateInfo == null) {
                    // Recursively add our parent as it's not been added yet.
                    parentStateInfo = addState(parent, null);
                }
            }
            StateInfo stateInfo = mStateInfo.get(state);
            if (stateInfo == null) {
                stateInfo = new StateInfo(state, parentStateInfo);
                mStateInfo.put(state, stateInfo);
            }
            // Validate that we aren't adding the same state in two different hierarchies.
            if ((stateInfo.parentStateInfo != null)
                    && (stateInfo.parentStateInfo != parentStateInfo)) {
                throw new RuntimeException("state already added");
            }
            return stateInfo;
        }
        /**
         * Remove a state from the state machine. Will not remove the state if it is currently
         * active or if it has any children in the hierarchy.
         * @param state the state to remove
         */
        private void removeState(State state) {
            StateInfo stateInfo = mStateInfo.get(state);
            if (stateInfo == null || stateInfo.active) {
                return;
            }
            boolean isParent = mStateInfo.values().stream()
                    .anyMatch(si -> si.parentStateInfo == stateInfo);
            if (isParent) {
                return;
            }
            mStateInfo.remove(state);
        }

        private SmHandler(Looper looper, UStateMachine sm) {
            super(looper);
            mSm = sm;
            addState(mQuittingState, null);
        }

        /** @see UStateMachine#setInitialState(State) */
        private void setInitialState(State initialState) {
            mInitialState = initialState;
        }

        /** @see UStateMachine#transitionTo(State) */
        private void transitionTo(State destState) {
            mDestState = destState;
        }

        /** @see UStateMachine#deferMessage(Message) */
        private void deferMessage(Message msg) {
            /* Copy the "msg" to "newMsg" as "msg" will be recycled */
            Message newMsg = obtainMessage();
            newMsg.copyFrom(msg);
            mDeferredMessages.add(newMsg);
        }

        /** @see UStateMachine#quit() */
        private void quit() {
            sendMessage(obtainMessage(SM_QUIT_CMD, mSmHandlerObj));
        }

        /** @see UStateMachine#quitNow() */
        private void quitNow() {
            sendMessageAtFrontOfQueue(obtainMessage(SM_QUIT_CMD, mSmHandlerObj));
        }

        /** Validate that the message was sent by quit or quitNow. */
        private boolean isQuit(Message msg) {
            return (msg.what == SM_QUIT_CMD) && (msg.obj == mSmHandlerObj);
        }
    }

    /**
     * Initialize.
     *
     * @param looper for this state machine
     * @param name of the state machine
     */
    private void initStateMachine(String name, Looper looper) {
        mName = name;
        mSmHandler = new SmHandler(looper, this);
    }

    /**
     * Constructor creates a StateMachine with its own thread.
     *
     * @param name of the state machine
     */
    protected UStateMachine(String name) {
        mSmThread = new HandlerThread(name);
        mSmThread.start();
        Looper looper = mSmThread.getLooper();
        initStateMachine(name, looper);
    }

    /**
     * Constructor creates a StateMachine using the looper.
     *
     * @param name of the state machine
     */
    protected UStateMachine(String name, Looper looper) {
        initStateMachine(name, looper);
    }

    /**
     * Constructor creates a StateMachine using the handler.
     *
     * @param name of the state machine
     */
    protected UStateMachine(String name, Handler handler) {
        initStateMachine(name, handler.getLooper());
    }

    /**
     * Add a new state to the state machine
     * @param state the state to add
     * @param parent the parent of state
     */
    public final void addState(State state, State parent) {
        mSmHandler.addState(state, parent);
    }

    /**
     * Add a new state to the state machine, parent will be null
     * @param state to add
     */
    public final void addState(State state) {
        mSmHandler.addState(state, null);
    }

    /**
     * Removes a state from the state machine, unless it is currently active or if it has children.
     * @param state state to remove
     */
    public final void removeState(State state) {
        mSmHandler.removeState(state);
    }

    /**
     * Set the initial state. This must be invoked before
     * and messages are sent to the state machine.
     *
     * @param initialState is the state which will receive the first message.
     */
    public final void setInitialState(State initialState) {
        mSmHandler.setInitialState(initialState);
    }

    public final Message getCurrentMessage() {
        // mSmHandler can be null if the state machine has quit.
        SmHandler smh = mSmHandler;
        if (smh == null) return null;
        return smh.getCurrentMessage();
    }

    public final State getCurrentState() {
        // mSmHandler can be null if the state machine has quit.
        SmHandler smh = mSmHandler;
        if (smh == null) return null;
        return smh.getCurrentState();
    }

    /**
     * transition to destination state. Upon returning
     * from processMessage the current state's exit will
     * be executed and upon the next message arriving
     * destState.enter will be invoked.
     *
     * this function can also be called inside the enter function of the
     * previous transition target, but the behavior is undefined when it is
     * called mid-way through a previous transition (for example, calling this
     * in the enter() routine of a intermediate node when the current transition
     * target is one of the nodes descendants).
     *
     * @param destState will be the state that receives the next message.
     */
    public final void transitionTo(State destState) {
        mSmHandler.transitionTo(destState);
    }

    /**
     * Defer this message until next state transition.
     * Upon transitioning all deferred messages will be
     * placed on the queue and reprocessed in the original
     * order. (i.e. The next state the oldest messages will
     * be processed first)
     *
     * @param msg is deferred until the next transition.
     */
    public final void deferMessage(Message msg) {
        mSmHandler.deferMessage(msg);
    }

    /**
     * This will be called once after a quit message that was NOT handled by
     * the derived StateMachine. The StateMachine will stop and any subsequent messages will be
     * ignored. In addition, if this StateMachine created the thread, the thread will
     * be stopped after this method returns.
     */
    protected void onQuitting() {
    }

    protected String getWhatToString(int what) {
        return null;
    }

    public final Handler getHandler() {
        return mSmHandler;
    }

    /**
     * Get a message and set Message.target state machine handler.
     *
     * Note: The handler can be null if the state machine has quit,
     * which means target will be null and may cause a AndroidRuntimeException
     * in MessageQueue#enqueMessage if sent directly or if sent using
     * StateMachine#sendMessage the message will just be ignored.
     *
     * @return  A Message object from the global pool
     */
    public final Message obtainMessage() {
        return Message.obtain(mSmHandler);
    }

    /**
     * Get a message and set Message.target state machine handler, what.
     *
     * Note: The handler can be null if the state machine has quit,
     * which means target will be null and may cause a AndroidRuntimeException
     * in MessageQueue#enqueMessage if sent directly or if sent using
     * StateMachine#sendMessage the message will just be ignored.
     *
     * @param what is the assigned to Message.what.
     * @return  A Message object from the global pool
     */
    public final Message obtainMessage(int what) {
        return Message.obtain(mSmHandler, what);
    }

    /**
     * Enqueue a message to this state machine.
     *
     * Message is ignored if state machine has quit.
     */
    public final void sendMessage(int what) {
        // mSmHandler can be null if the state machine has quit.
        SmHandler smh = mSmHandler;
        if (smh == null) return;
        smh.sendMessage(obtainMessage(what));
    }

    /**
     * Enqueue a message to this state machine.
     *
     * Message is ignored if state machine has quit.
     */
    public final void sendMessage(Message msg) {
        // mSmHandler can be null if the state machine has quit.
        SmHandler smh = mSmHandler;
        if (smh == null) return;
        smh.sendMessage(msg);
    }

    /**
     * Enqueue a message to this state machine after a delay.
     *
     * Message is ignored if state machine has quit.
     */
    public final void sendMessageDelayed(int what, long delayMillis) {
        // mSmHandler can be null if the state machine has quit.
        SmHandler smh = mSmHandler;
        if (smh == null) return;
        smh.sendMessageDelayed(obtainMessage(what), delayMillis);
    }

    /**
     * Enqueue a message to this state machine after a delay.
     *
     * Message is ignored if state machine has quit.
     */
    public final void sendMessageDelayed(Message msg, long delayMillis) {
        // mSmHandler can be null if the state machine has quit.
        SmHandler smh = mSmHandler;
        if (smh == null) return;
        smh.sendMessageDelayed(msg, delayMillis);
    }

    /**
     * Enqueue a message to the front of the queue for this state machine.
     * Protected, may only be called by instances of StateMachine.
     *
     * Message is ignored if state machine has quit.
     */
    protected final void sendMessageAtFrontOfQueue(int what) {
        // mSmHandler can be null if the state machine has quit.
        SmHandler smh = mSmHandler;
        if (smh == null) return;
        smh.sendMessageAtFrontOfQueue(obtainMessage(what));
    }

    /**
     * Enqueue a message to the front of the queue for this state machine.
     * Protected, may only be called by instances of StateMachine.
     *
     * Message is ignored if state machine has quit.
     */
    protected final void sendMessageAtFrontOfQueue(Message msg) {
        // mSmHandler can be null if the state machine has quit.
        SmHandler smh = mSmHandler;
        if (smh == null) return;
        smh.sendMessageAtFrontOfQueue(msg);
    }

    /**
     * Removes a message from the message queue.
     * Protected, may only be called by instances of StateMachine.
     */
    protected final void removeMessages(int what) {
        // mSmHandler can be null if the state machine has quit.
        SmHandler smh = mSmHandler;
        if (smh == null) return;
        smh.removeMessages(what);
    }

    /** Removes a message from the deferred messages queue. */
    protected final void removeDeferredMessages(int what) {
        SmHandler smh = mSmHandler;
        if (smh == null) return;
        Iterator<Message> iterator = smh.mDeferredMessages.iterator();
        while (iterator.hasNext()) {
            Message msg = iterator.next();
            if (msg.what == what) iterator.remove();
        }
    }

    /** Check if there are any pending messages with code 'what' in deferred messages queue. */
    protected final boolean hasDeferredMessages(int what) {
        SmHandler smh = mSmHandler;
        if (smh == null) return false;
        Iterator<Message> iterator = smh.mDeferredMessages.iterator();
        while (iterator.hasNext()) {
            Message msg = iterator.next();
            if (msg.what == what) return true;
        }
        return false;
    }

    /**
     * Check if there are any pending posts of messages with code 'what' in
     * the message queue. This does NOT check messages in deferred message queue.
     */
    protected final boolean hasMessages(int what) {
        SmHandler smh = mSmHandler;
        if (smh == null) return false;
        return smh.hasMessages(what);
    }

    /**
     * Validate that the message was sent by
     * {@link UStateMachine#quit} or {@link UStateMachine#quitNow}.
     */
    protected final boolean isQuit(Message msg) {
        // mSmHandler can be null if the state machine has quit.
        SmHandler smh = mSmHandler;
        if (smh == null) return msg.what == SM_QUIT_CMD;
        return smh.isQuit(msg);
    }

    /** Quit the state machine after all currently queued up messages are processed. */
    public final void quit() {
        // mSmHandler can be null if the state machine is already stopped.
        SmHandler smh = mSmHandler;
        if (smh == null) return;
        smh.quit();
    }

    /** Quit the state machine immediately all currently queued messages will be discarded. */
    public final void quitNow() {
        // mSmHandler can be null if the state machine is already stopped.
        SmHandler smh = mSmHandler;
        if (smh == null) return;
        smh.quitNow();
    }

    public final void start() {
        // mSmHandler can be null if the state machine has quit.
        SmHandler smh = mSmHandler;
        if (smh == null) return;
        // Send the complete construction message
        smh.completeConstruction();
    }

    public final String toString() {
        String state = "null";
        try {
            state = mSmHandler.getCurrentState().getName().toString();
        } catch (NullPointerException | ArrayIndexOutOfBoundsException e) {
            // Will use default(s) initialized above.
        }
        return "name=" + mName + " state=" + state;
    }
}
