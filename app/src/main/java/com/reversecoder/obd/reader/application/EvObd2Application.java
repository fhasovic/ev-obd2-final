package com.reversecoder.obd.reader.application;

import android.app.Application;

import com.reversecoder.sqlite.LitePal;

public class EvObd2Application extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        LitePal.initialize(this);
    }
}
