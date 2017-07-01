package com.reversecoder.obd.reader.io;

interface ServiceState {
    
    public void startAbstractService() throws Exception;

    public void stopAbstractService() throws Exception;
}