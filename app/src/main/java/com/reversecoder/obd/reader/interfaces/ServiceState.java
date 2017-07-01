package com.reversecoder.obd.reader.interfaces;

public interface ServiceState {
    
    public void startAbstractService() throws Exception;

    public void stopAbstractService() throws Exception;
}