package com.reversecoder.obd.reader.service;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;
import android.widget.Toast;

import com.reversecoder.library.storage.SessionManager;
import com.reversecoder.obd.reader.R;
import com.reversecoder.obd.reader.thread.ObdConnectionThread;
import com.reversecoder.obd.reader.util.AllConstants;
import com.reversecoder.obd.reader.util.BluetoothManager;
import com.reversecoder.obd.reader.util.ThreadManager;

import java.io.IOException;

public class ObdReaderService extends AbstractService {

    private static final String TAG = ObdReaderService.class.getName();
    ObdConnectionThread obdConnectionThread;
    private BluetoothDevice bluetoothDevice = null;
    private BluetoothSocket bluetoothSocket = null;
    private boolean isObdConnected = false;

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

        // Let's configure the connection.
        obdConnectionThread = new ObdConnectionThread(getContext(), bluetoothSocket);
        obdConnectionThread.addListener(new ThreadManager.ThreadCompletedListener() {
            @Override
            public void notifyOnCompleted(Thread thread, boolean isSucceeded) {
                isObdConnected = isSucceeded;
            }
        });
        obdConnectionThread.start();
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
