package com.reversecoder.obd.reader.map.sync;

/**
 * @author Md. Rashadul Alam
 */
public interface WattsOCMSyncTaskListener<T> {
    public void onOCMSyncSuccess(T object);

    public void onOCMSyncFailure(Exception exception);
}
