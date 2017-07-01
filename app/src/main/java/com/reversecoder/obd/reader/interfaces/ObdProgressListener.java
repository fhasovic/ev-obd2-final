package com.reversecoder.obd.reader.interfaces;

import com.reversecoder.obd.reader.service.ObdCommandJob;

public interface ObdProgressListener {

    void stateUpdate(final ObdCommandJob job);

}