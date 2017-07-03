package com.reversecoder.obd.reader.service;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;
import android.widget.Toast;

import com.reversecoder.library.storage.SessionManager;
import com.reversecoder.obd.reader.R;
import com.reversecoder.obd.reader.command.ATA1Command;
import com.reversecoder.obd.reader.command.ATCAF0Command;
import com.reversecoder.obd.reader.command.ATFCSH7E4Command;
import com.reversecoder.obd.reader.command.ATSH7E4Command;
import com.reversecoder.obd.reader.command.SoCCommand;
import com.reversecoder.obd.reader.util.AllConstants;
import com.reversecoder.obd.reader.util.BluetoothManager;
import com.reversecoder.obd2.commands.protocol.EchoOffCommand;
import com.reversecoder.obd2.commands.protocol.LineFeedOffCommand;
import com.reversecoder.obd2.commands.protocol.ObdResetCommand;
import com.reversecoder.obd2.commands.protocol.SelectProtocolCommand;
import com.reversecoder.obd2.commands.protocol.TimeoutCommand;
import com.reversecoder.obd2.enums.ObdProtocols;
import com.reversecoder.obd2.exceptions.UnsupportedCommandException;
import com.reversecoder.obd2.utils.ObdUtil;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ObdReaderService extends AbstractService {

    private static final String TAG = ObdReaderService.class.getName();
    //    ObdConnectionThread obdConnectionThread;
    private BluetoothDevice bluetoothDevice = null;
    private BluetoothSocket bluetoothSocket = null;
    private boolean isObdConnected = false;
    BlockingQueue<ObdCommandJob> jobsQueue = new LinkedBlockingQueue<>();

    @Override
    public void startService() throws IOException {

        Log.d(TAG, "Starting service..");
        // get the remote Bluetooth device
        String remoteDevice = SessionManager.getStringSetting(getContext(), AllConstants.SESSION_KEY_SELECTED_BLUETOOTH_DEVICE, "None");
        Log.d(TAG, "remoteDevice: " + remoteDevice);
        if (remoteDevice == null || remoteDevice.equalsIgnoreCase("None")) {

            Toast.makeText(getContext(), getString(R.string.text_bluetooth_nodevice), Toast.LENGTH_LONG).show();
            Log.d(TAG, "No Bluetooth device has been selected.");
            stopService();
            throw new IOException();

        } else {

            BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
            bluetoothDevice = btAdapter.getRemoteDevice(remoteDevice);

    /*
     * Establish Bluetooth connection
     *
     * Because discovery is a heavyweight procedure for the Bluetooth adapter,
     * this method should always be called before attempting to connect to a
     * remote device with connect(). Discovery is not managed by the Activity,
     * but is run as a system service, so an application should always call
     * cancel discovery even if it did not directly request a discovery, just to
     * be sure. If Bluetooth state is not STATE_ON, this API will return false.
     *
     * see
     * http://developer.android.com/reference/android/bluetooth/BluetoothAdapter
     * .html#cancelDiscovery()
     */
            Log.d(TAG, "Stopping Bluetooth discovery.");
            btAdapter.cancelDiscovery();
            Log.d(TAG, getString(R.string.service_starting));

            try {
                startObdConnection();
            } catch (Exception e) {
                Log.e(
                        TAG,
                        "There was an error while establishing connection. -> "
                                + e.getMessage()
                );

                // in case of failure, stop this service.
                stopService();
                throw new IOException();
            }
        }
    }

    /**
     * Start and configure the connection to the OBD interface.
     * <p/>
     * See http://stackoverflow.com/questions/18657427/ioexception-read-failed-socket-might-closed-bluetooth-on-android-4-3/18786701#18786701
     *
     * @throws IOException
     */
    private void startObdConnection() throws IOException {
        Log.d(TAG, "Starting OBD connection..");
        try {
            bluetoothSocket = BluetoothManager.connect(bluetoothDevice);

            Log.d(TAG, getString(R.string.service_started));
            Toast.makeText(getContext(), getString(R.string.service_started), Toast.LENGTH_LONG).show();
        } catch (Exception e2) {
            Log.e(TAG, "There was an error while establishing Bluetooth connection. Stopping app..", e2);
            stopService();
            throw new IOException();
        }

        queueObdCommands();

        // Let's configure the connection.
//        obdConnectionThread = new ObdConnectionThread(getContext(), bluetoothSocket);
//        obdConnectionThread.addListener(new ThreadManager.ThreadCompletedListener() {
//            @Override
//            public void notifyOnCompleted(Thread thread, boolean isSucceeded) {
//                isObdConnected = isSucceeded;
//            }
//        });
//        obdConnectionThread.start();
    }

    @Override
    public void executeTasks() {
        //now time to execute job
        Log.d(TAG, "Executing queue..");
        while (!Thread.currentThread().isInterrupted()) {
            ObdCommandJob job = null;
            try {
                job = jobsQueue.take();

                if (job != null) {

                    Log.d(TAG, "Taking job[" + job.getId() + "] from queue..");
                    Log.d(TAG, "Taking job[" + job.getCommand().getName() + "] from queue..");

                    if (job.getState().equals(ObdCommandJob.ObdCommandJobState.NEW)) {
                        Log.d(TAG, "Job state is NEW. Run it..");
                        job.setState(ObdCommandJob.ObdCommandJobState.RUNNING);
                        if (bluetoothSocket.isConnected()) {
                            Log.e(TAG, "Socet connection ok, running command");
//                        if (job.getCommand().getName().equalsIgnoreCase("AT S0") ||
//                                job.getCommand().getName().equalsIgnoreCase("AT SP6") ||
//                                job.getCommand().getName().equalsIgnoreCase("AT AT1") ||
//                                job.getCommand().getName().equalsIgnoreCase("AT CAF0") ||
//                                job.getCommand().getName().equalsIgnoreCase("AT SH7E4") ||
//                                job.getCommand().getName().equalsIgnoreCase("AT FCSH7E4") ||
//                                job.getCommand().getName().equalsIgnoreCase("03222002")) {
//                            job.getCommand().runRawCommand(mSocket.getInputStream(), mSocket.getOutputStream());
//                        } else {
//                            job.getCommand().run(mSocket.getInputStream(), mSocket.getOutputStream());
//                        }
                            job.getCommand().runRawCommand(bluetoothSocket.getInputStream(), bluetoothSocket.getOutputStream());
                        } else {
                            job.setState(ObdCommandJob.ObdCommandJobState.EXECUTION_ERROR);
                            Log.e(TAG, "Can't run command on a closed socket.");
                        }
                    } else {
                        // log not new job
                        Log.e(TAG,
                                "Job state was not new, so it shouldn't be in queue. BUG ALERT!");
                    }
                }
            } catch (InterruptedException i) {
                Thread.currentThread().interrupt();
            } catch (UnsupportedCommandException u) {
                if (job != null) {
                    job.setState(ObdCommandJob.ObdCommandJobState.NOT_SUPPORTED);
                }
                Log.d(TAG, "Command not supported. -> " + u.getMessage());
            } catch (IOException io) {
                if (job != null) {
                    if (io.getMessage().contains("Broken pipe"))
                        job.setState(ObdCommandJob.ObdCommandJobState.BROKEN_PIPE);
                    else
                        job.setState(ObdCommandJob.ObdCommandJobState.EXECUTION_ERROR);
                }
                Log.e(TAG, "IO error. -> " + io.getMessage());
            } catch (Exception e) {
                if (job != null) {
                    job.setState(ObdCommandJob.ObdCommandJobState.EXECUTION_ERROR);
                }
                Log.e(TAG, "Failed to run command. -> " + e.getMessage());
            }

//            if (job != null) {
//                Log.d(TAG, "Job is finished.");
//                final ObdCommandJob job2 = job;
//                ((ObdReaderActivity) getContext()).runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        Log.d(TAG, "Sending command result to UI.");
//                        ((ObdReaderActivity) getContext()).stateUpdate(job2);
//                    }
//                });
//            }
        }
    }

    private void queueObdCommands() {
        // execute commands
//        try {
        //Jobs for establishing OBD connection
        ObdCommandJob obdResetJob = new ObdCommandJob(new ObdResetCommand());
        ObdCommandJob obdEchoOffJob = new ObdCommandJob(new EchoOffCommand());
        ObdCommandJob obdLineFeedOffJob = new ObdCommandJob(new LineFeedOffCommand());
        ObdCommandJob obdTimeoutJob = new ObdCommandJob(new TimeoutCommand(100));
        ObdCommandJob obdSelectProtocolJob = new ObdCommandJob(new SelectProtocolCommand(ObdProtocols.ISO_15765_4_CAN));

        //Jobs for getting odometer result
//        ObdCommandJob obdS0Job = new ObdCommandJob(new ATS0Command());
//        ObdCommandJob obdSP6Job = new ObdCommandJob(new ATSP6Command());
        ObdCommandJob obdATA1Job = new ObdCommandJob(new ATA1Command());
        ObdCommandJob obdATCAF0Job = new ObdCommandJob(new ATCAF0Command());
        ObdCommandJob obdATSHJob = new ObdCommandJob(new ATSH7E4Command());
        ObdCommandJob obdATFCSHJob = new ObdCommandJob(new ATFCSH7E4Command());
        ObdCommandJob obdSoCJob = new ObdCommandJob(new SoCCommand());

        //enqueue job
        try {
            obdResetJob.setId((long) ObdUtil.getRandomInteger());
            jobsQueue.put(obdResetJob);
        } catch (InterruptedException e) {
            obdResetJob.setState(ObdCommandJob.ObdCommandJobState.QUEUE_ERROR);
            Log.e(TAG, "Failed to queue job [" + obdResetJob.getCommand().getName() + "].");
        }
        try {
            obdEchoOffJob.setId((long) ObdUtil.getRandomInteger());
            jobsQueue.put(obdEchoOffJob);
        } catch (InterruptedException e) {
            obdEchoOffJob.setState(ObdCommandJob.ObdCommandJobState.QUEUE_ERROR);
            Log.e(TAG, "Failed to queue job [" + obdEchoOffJob.getCommand().getName() + "].");
        }
        try {
            obdLineFeedOffJob.setId((long) ObdUtil.getRandomInteger());
            jobsQueue.put(obdLineFeedOffJob);
        } catch (InterruptedException e) {
            obdLineFeedOffJob.setState(ObdCommandJob.ObdCommandJobState.QUEUE_ERROR);
            Log.e(TAG, "Failed to queue job [" + obdLineFeedOffJob.getCommand().getName() + "].");
        }
        try {
            obdTimeoutJob.setId((long) ObdUtil.getRandomInteger());
            jobsQueue.put(obdTimeoutJob);
        } catch (InterruptedException e) {
            obdTimeoutJob.setState(ObdCommandJob.ObdCommandJobState.QUEUE_ERROR);
            Log.e(TAG, "Failed to queue job [" + obdTimeoutJob.getCommand().getName() + "].");
        }
        try {
            obdSelectProtocolJob.setId((long) ObdUtil.getRandomInteger());
            jobsQueue.put(obdSelectProtocolJob);
        } catch (InterruptedException e) {
            obdSelectProtocolJob.setState(ObdCommandJob.ObdCommandJobState.QUEUE_ERROR);
            Log.e(TAG, "Failed to queue job [" + obdSelectProtocolJob.getCommand().getName() + "].");
        }
        ////////////////////
//        try {
//            obdS0Job.setId((long) ObdUtil.getRandomInteger());
//            jobsQueue.put(obdS0Job);
//        } catch (InterruptedException e) {
//            obdS0Job.setState(ObdCommandJob.ObdCommandJobState.QUEUE_ERROR);
//            Log.e(TAG, "Failed to queue job [" + obdS0Job.getCommand().getName() + "].");
//        }
//        try {
//            obdSP6Job.setId((long) ObdUtil.getRandomInteger());
//            jobsQueue.put(obdSP6Job);
//        } catch (InterruptedException e) {
//            obdSP6Job.setState(ObdCommandJob.ObdCommandJobState.QUEUE_ERROR);
//            Log.e(TAG, "Failed to queue job [" + obdSP6Job.getCommand().getName() + "].");
//        }
        try {
            obdATA1Job.setId((long) ObdUtil.getRandomInteger());
            jobsQueue.put(obdATA1Job);
        } catch (InterruptedException e) {
            obdATA1Job.setState(ObdCommandJob.ObdCommandJobState.QUEUE_ERROR);
            Log.e(TAG, "Failed to queue job [" + obdATA1Job.getCommand().getName() + "].");
        }
        try {
            obdATCAF0Job.setId((long) ObdUtil.getRandomInteger());
            jobsQueue.put(obdATCAF0Job);
        } catch (InterruptedException e) {
            obdATCAF0Job.setState(ObdCommandJob.ObdCommandJobState.QUEUE_ERROR);
            Log.e(TAG, "Failed to queue job [" + obdATCAF0Job.getCommand().getName() + "].");
        }
        try {
            obdATSHJob.setId((long) ObdUtil.getRandomInteger());
            jobsQueue.put(obdATSHJob);
        } catch (InterruptedException e) {
            obdATSHJob.setState(ObdCommandJob.ObdCommandJobState.QUEUE_ERROR);
            Log.e(TAG, "Failed to queue job [" + obdATSHJob.getCommand().getName() + "].");
        }
        try {
            obdATFCSHJob.setId((long) ObdUtil.getRandomInteger());
            jobsQueue.put(obdATFCSHJob);
        } catch (InterruptedException e) {
            obdATFCSHJob.setState(ObdCommandJob.ObdCommandJobState.QUEUE_ERROR);
            Log.e(TAG, "Failed to queue job [" + obdATFCSHJob.getCommand().getName() + "].");
        }
        try {
            obdSoCJob.setId((long) ObdUtil.getRandomInteger());
            jobsQueue.put(obdSoCJob);
        } catch (InterruptedException e) {
            obdSoCJob.setState(ObdCommandJob.ObdCommandJobState.QUEUE_ERROR);
            Log.e(TAG, "Failed to queue job [" + obdSoCJob.getCommand().getName() + "].");
        }
    }

    /**
     * Stop OBD connection and queue processing.
     */
    @Override
    public void stopService() {
        Log.d(TAG, "Stopping service..");

        if (bluetoothSocket != null)
            // close socket
            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }
        isObdConnected = false;
        // kill service
        stopSelf();
    }

    public boolean isObdConnected() {
        return isObdConnected;
    }
}
