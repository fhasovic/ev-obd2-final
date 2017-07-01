package com.reversecoder.obd.reader.io;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.reversecoder.obd2.commands.protocol.EchoOffCommand;
import com.reversecoder.obd2.commands.protocol.LineFeedOffCommand;
import com.reversecoder.obd2.commands.protocol.ObdResetCommand;
import com.reversecoder.obd2.commands.protocol.SelectProtocolCommand;
import com.reversecoder.obd2.commands.protocol.TimeoutCommand;
import com.reversecoder.obd2.enums.ObdProtocols;
import com.reversecoder.obd2.exceptions.UnsupportedCommandException;
import com.reversecoder.obd.reader.R;
import com.reversecoder.obd.reader.activity.ConfigActivity;
import com.reversecoder.obd.reader.activity.DashboardActivity;
import com.reversecoder.obd2.utils.ObdUtil;

import java.io.File;
import java.io.IOException;

/**
 * This service is primarily responsible for establishing and maintaining a
 * permanent connection between the device where the application runs and a more
 * OBD Bluetooth interface.
 * <p/>
 * Secondarily, it will serve as a repository of ObdCommandJobs and at the same
 * time the application state-machine.
 */
public class DashboardService extends AbstractGatewayService {

    private static final String TAG = DashboardService.class.getName();

//    SharedPreferences prefs;

    private BluetoothDevice dev = null;
    private BluetoothSocket sock = null;

    public void startService() throws IOException {
        Log.d(TAG, "Starting service..");

        // get the remote Bluetooth device
        final String remoteDevice = prefs.getString(ConfigActivity.BLUETOOTH_LIST_KEY, null);
        Log.d(TAG, "remoteDevice: " + remoteDevice);
        if (remoteDevice == null || "".equals(remoteDevice)) {
            Toast.makeText(ctx, getString(R.string.text_bluetooth_nodevice), Toast.LENGTH_LONG).show();

            // log error
            Log.e(TAG, "No Bluetooth device has been selected.");

            // TODO kill this service gracefully
            stopService();
            throw new IOException();
        } else {

            final BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
            dev = btAdapter.getRemoteDevice(remoteDevice);


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

            showNotification(getString(R.string.notification_action), getString(R.string.service_starting), R.drawable.ic_btcar, true, true, false);

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
            showNotification(getString(R.string.notification_action), getString(R.string.service_started), R.drawable.ic_btcar, true, true, false);
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
        isRunning = true;
        try {
            sock = BluetoothManager.connect(dev);
        } catch (Exception e2) {
            Log.e(TAG, "There was an error while establishing Bluetooth connection. Stopping app..", e2);
            stopService();
            throw new IOException();
        }

        // Let's configure the connection.
        Log.d(TAG, "Queueing jobs for connection configuration..");
        queueJob(new ObdCommandJob(new ObdResetCommand()));

        //Below is to give the adapter enough time to reset before sending the commands, otherwise the first startup commands could be ignored.
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        queueJob(new ObdCommandJob(new EchoOffCommand()));

    /*
     * Will send second-time based on tests.
     *
     * TODO this can be done w/o having to queue jobs by just issuing
     * command.run(), command.getResult() and validate the result.
     */
        queueJob(new ObdCommandJob(new EchoOffCommand()));
        queueJob(new ObdCommandJob(new LineFeedOffCommand()));
        queueJob(new ObdCommandJob(new TimeoutCommand(62)));

        // Get protocol from preferences
        final String protocol = prefs.getString(ConfigActivity.PROTOCOLS_LIST_KEY, "AUTO");
        queueJob(new ObdCommandJob(new SelectProtocolCommand(ObdProtocols.valueOf(protocol))));

        // Job for returning dummy data
//        queueJob(new ObdCommandJob(new AmbientAirTemperatureCommand()));

        queueCounter = 0L;
        Log.d(TAG, "Initialization jobs queued.");


    }

    /**
     * This method will add a job to the queue while setting its ID to the
     * internal queue counter.
     *
     * @param job the job to queue.
     */
    @Override
    public void queueJob(ObdCommandJob job) {
        // This is a good place to enforce the imperial units option
        job.getCommand().useImperialUnits(prefs.getBoolean(ConfigActivity.IMPERIAL_UNITS_KEY, false));

        // Now we can pass it along
        super.queueJob(job);
    }

    /**
     * Runs the queue until the service is stopped
     */
    @Override
    protected void executeQueue() throws InterruptedException {
        Log.d(TAG, "****************************************************");
        Log.d(TAG, "Executing queue..");
        while (!Thread.currentThread().isInterrupted()) {
            Log.d(TAG, "------------------------------------------");
            ObdCommandJob job = null;
            try {
                job = jobsQueue.take();

                // log job
                Log.d(TAG, "Taking job[" + job.getId() + "] from queue..");
                Log.d(TAG, "Taking job[" + job.getCommand().getName() + "] from queue..");

                if (job.getState().equals(ObdCommandJob.ObdCommandJobState.NEW)) {
                    Log.d(TAG, "Job state is NEW. Run it..");
                    job.setState(ObdCommandJob.ObdCommandJobState.RUNNING);
                    if (sock.isConnected()) {
                        Log.e(TAG, "Socet connection ok, running command");
                        job.getCommand().run(sock.getInputStream(), sock.getOutputStream());
                    } else {
                        job.setState(ObdCommandJob.ObdCommandJobState.EXECUTION_ERROR);
                        Log.e(TAG, "Can't run command on a closed socket.");
                    }
                } else
                    // log not new job
                    Log.e(TAG,
                            "Job state was not new, so it shouldn't be in queue. BUG ALERT!");
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

            if (job != null) {
                Log.d(TAG, "Job is finished.");
                final ObdCommandJob job2 = job;
                ((DashboardActivity) ctx).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "Sending command result to UI.");
                        ((DashboardActivity) ctx).stateUpdate(job2);
                    }
                });
            }
            Log.d(TAG, "------------------------------------------");
        }
        Log.d(TAG, "****************************************************");
    }

    @Override
    public void executeSingleCommand(ObdCommandJob commandJob, final ObdProgressListener obdProgressListener) throws InterruptedException {
        Log.d(TAG, "****************************************************");
        Log.d(TAG, "Executing single command");
        ObdCommandJob job = null;
        try {
            job = commandJob;

            // log job
            Log.d(TAG, "Taking job[" + job.getId() + "] for executing");

            if (job.getState().equals(ObdCommandJob.ObdCommandJobState.NEW)) {
                Log.d(TAG, "Job state is NEW. Run it..");
                job.setState(ObdCommandJob.ObdCommandJobState.RUNNING);
                if (sock.isConnected()) {
                    job.getCommand().run(sock.getInputStream(), sock.getOutputStream());
                } else {
                    job.setState(ObdCommandJob.ObdCommandJobState.EXECUTION_ERROR);
                    Log.e(TAG, "Can't run command on a closed socket.");
                }
            } else
                // log not new job
                Log.e(TAG,
                        "Job state was not new, so it shouldn't be in queue. BUG ALERT!");
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

        if (job != null) {
            final ObdCommandJob job2 = job;
            obdProgressListener.stateUpdate(job2);
//            ((DashboardActivity) ctx).runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    obdProgressListener.stateUpdate(job2);
//                }
//            });
            Log.d(TAG, "****************************************************");
        }
    }

    /**
     * Stop OBD connection and queue processing.
     */
    public void stopService() {
        Log.d(TAG, "Stopping service..");

        notificationManager.cancel(NOTIFICATION_ID);
        jobsQueue.clear();
        isRunning = false;

        if (sock != null)
            // close socket
            try {
                sock.close();
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }

        // kill service
        stopSelf();
    }

    public boolean isRunning() {
        return isRunning;
    }

    public static void saveLogcatToFile(Context context, String devemail, String ccemail) {
        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        emailIntent.setType("text/plain");
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{devemail});
        emailIntent.putExtra(Intent.EXTRA_CC, new String[]{ccemail});
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "[EV-OBD2_"+context.getString(R.string.app_version_name)+"] Debug log of "+ObdUtil.convertMilliSecondToTime(System.currentTimeMillis(),"yyyy.MM.dd 'at' hh:mm:ss a"));

        StringBuilder sb = new StringBuilder();
        sb.append("\nManufacturer: ").append(Build.MANUFACTURER);
        sb.append("\nModel: ").append(Build.MODEL);
        sb.append("\nAndroid version: ").append(Build.VERSION.RELEASE);

        emailIntent.putExtra(Intent.EXTRA_TEXT, sb.toString());

        String fileName = "[EV-OBD2_"+context.getString(R.string.app_version_name)+"]_" + ObdUtil.convertMilliSecondToTime(System.currentTimeMillis(),"yyyy.MM.dd'_'hh.mm.ss'_'a") + ".txt";
        File sdCard = Environment.getExternalStorageDirectory();
        File dir = new File(sdCard.getAbsolutePath() + File.separator + "EV-OBD2");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        if (dir.exists()) {
            File outputFile = new File(dir, fileName);
            Uri uri = Uri.fromFile(outputFile);
            emailIntent.putExtra(Intent.EXTRA_STREAM, uri);

            Log.d(TAG, "Going to send logcat from " + outputFile);
            //emailIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(Intent.createChooser(emailIntent, "Pick an Email provider").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));

            try {
                @SuppressWarnings("unused")
                Process process = Runtime.getRuntime().exec("logcat -f " + outputFile.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
