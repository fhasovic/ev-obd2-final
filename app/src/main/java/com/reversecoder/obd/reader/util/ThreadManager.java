package com.reversecoder.obd.reader.util;

import android.content.Context;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * @author Md. Rashadul Alam
 */

public abstract class ThreadManager extends Thread {

    public interface ThreadCompletedListener {
        void notifyOnCompleted(final Thread thread, final boolean isSucceeded);
    }

    private boolean isSucceeded = false;
    private boolean isFinished = false;
    private Context mContext;

    private final Set<ThreadCompletedListener> listeners = new CopyOnWriteArraySet<ThreadCompletedListener>();

    public ThreadManager(Context context){
        mContext = context;
    }

    public Context getContext() {
        return mContext;
    }

    public final void addListener(final ThreadCompletedListener listener) {
        listeners.add(listener);
    }

    public final void removeListener(final ThreadCompletedListener listener) {
        listeners.remove(listener);
    }

    private final void notifyListeners() {
        for (ThreadCompletedListener listener : listeners) {
            listener.notifyOnCompleted(this, isSucceeded);
        }
    }

    public boolean isFinished() {
        return isFinished;
    }

    @Override
    public final void run() {
        try {
            isFinished = false;
            doRun();
            isSucceeded = true;
        } catch (Exception ex) {
            isFinished = false;
            isSucceeded = false;
        } finally {
            notifyListeners();
            isFinished = true;
        }
    }

    public abstract void doRun();
}
