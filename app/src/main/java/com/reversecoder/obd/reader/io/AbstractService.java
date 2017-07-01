package com.reversecoder.obd.reader.io;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

public abstract class AbstractService extends Service implements ServiceState {

    private static final String TAG = AbstractService.class.getName();
    private final IBinder binder = new AbstractServiceBinder();
    private Context mContext;
    private boolean isServiceRunning = false;
    // Run the execute task in a different thread to lighten the UI thread
//    Thread differentThread = new Thread(new Runnable() {
//        @Override
//        public void run() {
//            try {
//                executeTasks();
//            } catch (InterruptedException e) {
//                differentThread.interrupt();
//            }
//        }
//    });

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();

//        differentThread.start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void startAbstractService() throws Exception {
        setServiceRunning(true);
        startService();
    }

    @Override
    public void stopAbstractService() throws Exception {
        stopService();
        setServiceRunning(false);
    }

    public boolean isServiceRunning() {
        return isServiceRunning;
    }

    private void setServiceRunning(boolean isServiceRunning) {
        this.isServiceRunning = isServiceRunning;
    }

    public void setContext(Context context) {
        mContext = context;
    }

    public Context getContext() {
        return mContext;
    }

    abstract public void startService() throws Exception;

    abstract public void stopService() throws Exception;

//    abstract protected void executeTasks() throws InterruptedException;

    public class AbstractServiceBinder extends Binder {
        public AbstractService getService() {
            return AbstractService.this;
        }
    }

//    public interface UpdateTaskStatus {
//        void updateTasks(Object job);
//    }
}
