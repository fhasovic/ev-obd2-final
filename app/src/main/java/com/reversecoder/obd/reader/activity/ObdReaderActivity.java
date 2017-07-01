package com.reversecoder.obd.reader.activity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;

import com.reversecoder.library.event.OnSingleClickListener;
import com.reversecoder.library.storage.SessionManager;
import com.reversecoder.obd.reader.R;
import com.reversecoder.obd.reader.service.AbstractService;
import com.reversecoder.obd.reader.service.ObdCommandJob;
import com.reversecoder.obd.reader.interfaces.ObdProgressListener;
import com.reversecoder.obd.reader.service.ObdReaderService;
import com.reversecoder.obd.reader.util.AppUtils;
import com.reversecoder.obd.reader.util.BroadcastReceiverManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.reversecoder.obd.reader.util.AllConstants.SESSION_KEY_SELECTED_BLUETOOTH_DEVICE;

public class ObdReaderActivity extends AppCompatActivity implements ObdProgressListener {

    Button btnConnectBluetooth, btnSendObdCommand, btnConnectObd, btnClearObdLog, btnSendEmail;
    Spinner spinnerObdDevices;
    EditText edtObdCommand;
    LinearLayout llResponseList;
    BluetoothAdapter btAdapter;
    private static final int REQUEST_ENABLE_BT = 1234;
    BroadcastReceiverManager receiverManager;
    ArrayAdapter<String> spinnerBluetoothAdapter;
    private boolean isServiceBound = false;
    private ObdReaderService service;
    private PowerManager.WakeLock wakeLock = null;
    private PowerManager powerManager;
    LayoutInflater layoutInflater;
    public Map<String, String> commandResult = new HashMap<String, String>();
    private boolean isDebuggable = true;
    private boolean lastObdConnectedStatus = false;
    private static final String TAG = ObdReaderActivity.class.getSimpleName();

    /********************
     * Activity methods *
     ********************/

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_obd_reader);

        initObd2ReaderUI();

        initObd2ReaderAction();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
//            if (resultCode == Activity.RESULT_OK) {
//            }
            setBluetoothStatus();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onResume() {
        super.onResume();

        setBluetoothStatus();

        registerBluetoothBroadcast();

        acquireWakeLock();
    }

    @Override
    protected void onPause() {
        super.onPause();

        unregisterBluetoothBroadcast();

        releaseWakeLockIfHeld();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        doUnbindService();

        disConnectBluetooth();

        unregisterBluetoothBroadcast();

        releaseWakeLockIfHeld();
    }

    /*******************************
     * Activity's view and actions *
     *******************************/

    private void initObd2ReaderUI() {
        btnConnectBluetooth = (Button) findViewById(R.id.btn_connect_bluetooth);
        btnSendObdCommand = (Button) findViewById(R.id.btn_send_obd_command);
        btnConnectObd = (Button) findViewById(R.id.btn_connect_obd);
        btnClearObdLog = (Button) findViewById(R.id.btn_clear_obd_log);
        btnSendEmail = (Button) findViewById(R.id.btn_send_email);
        spinnerObdDevices = (Spinner) findViewById(R.id.spinner_obd_device);
        edtObdCommand = (EditText) findViewById(R.id.edt_obd_command);
        llResponseList = (LinearLayout) findViewById(R.id.ll_response_list);

        layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        receiverManager = BroadcastReceiverManager.init(ObdReaderActivity.this);
        powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "ObdReader");
        setBluetoothStatus();
    }

    private void initObd2ReaderAction() {
        btnConnectBluetooth.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                connectBluetooth();
            }
        });

        btnConnectObd.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                if (service != null && service.isServiceRunning()) {
                    disconnectObd();
                } else {
                    connectObd();
                }
            }
        });

        btnSendObdCommand.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View view) {
                if (service != null && service.isServiceRunning() && service.isObdConnected()) {

                }
            }
        });

        btnClearObdLog.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View view) {
                llResponseList.removeAllViews();
            }
        });

        btnSendEmail.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View view) {
                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case DialogInterface.BUTTON_POSITIVE:
                                AppUtils.saveLogcatToFile(getApplicationContext(), "rashed.droid@gmail.com", "mdhayatunnabi@yahoo.com");
                                break;

                            case DialogInterface.BUTTON_NEGATIVE:
                                //No button clicked
                                break;
                        }
                    }
                };
                AlertDialog.Builder builder = new AlertDialog.Builder(ObdReaderActivity.this);
                builder.setMessage("Where there issues?\nThen please send us the logs.\nSend Logs?").setPositiveButton("Yes", dialogClickListener)
                        .setNegativeButton("No", dialogClickListener).show();
            }
        });

        spinnerObdDevices.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String item = parent.getItemAtPosition(position).toString();
//                Toast.makeText(parent.getContext(), "Selected: " + item, Toast.LENGTH_SHORT).show();

                if (!item.equalsIgnoreCase("None")) {
                    SessionManager.setStringSetting(ObdReaderActivity.this, SESSION_KEY_SELECTED_BLUETOOTH_DEVICE, item.split(">>")[1]);
                } else {
                    SessionManager.setStringSetting(ObdReaderActivity.this, SESSION_KEY_SELECTED_BLUETOOTH_DEVICE, "None");
                }

                setSpinnerStatus();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setSpinnerStatus() {
        String selectedItem = SessionManager.getStringSetting(ObdReaderActivity.this, SESSION_KEY_SELECTED_BLUETOOTH_DEVICE);
        if (selectedItem != null && selectedItem.equalsIgnoreCase("None")) {
            btnConnectObd.setEnabled(false);
            edtObdCommand.setEnabled(false);
            btnSendObdCommand.setEnabled(false);
        } else {
            if (service != null && service.isServiceRunning() && service.isObdConnected()) {
                btnConnectObd.setEnabled(false);
                edtObdCommand.setEnabled(true);
                btnSendObdCommand.setEnabled(true);
            } else {
                btnConnectObd.setEnabled(true);
                edtObdCommand.setEnabled(false);
                btnSendObdCommand.setEnabled(false);
            }
        }
    }

    private void sendLogToDeveloper() {
        if (isDebuggable && lastObdConnectedStatus) {
            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case DialogInterface.BUTTON_POSITIVE:
                            AppUtils.saveLogcatToFile(getApplicationContext(), "rashed.droid@gmail.com", "mdhayatunnabi@yahoo.com");
                            break;

                        case DialogInterface.BUTTON_NEGATIVE:
                            //No button clicked
                            break;
                    }
                }
            };
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Where there issues?\nThen please send us the logs.\nSend Logs?").setPositiveButton("Yes", dialogClickListener)
                    .setNegativeButton("No", dialogClickListener).show();
        }
    }

    /*************************
     * Methods for bluetooth *
     *************************/

    private void setBluetoothStatus() {
        if (btAdapter == null) {
            btAdapter = BluetoothAdapter.getDefaultAdapter();
        }

        if (btAdapter != null && btAdapter.isEnabled()) {
            btnConnectBluetooth.setText("Enabled");
            btnConnectBluetooth.setEnabled(false);
            spinnerObdDevices.setEnabled(true);
            edtObdCommand.setEnabled(true);
            btnSendObdCommand.setEnabled(true);
            btnConnectObd.setEnabled(true);

            // set spinner
            ArrayList<String> categories = new ArrayList<String>();
            Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
            if (pairedDevices.size() > 0) {
                for (BluetoothDevice device : pairedDevices) {
                    categories.add(device.getName() + ">>" + device.getAddress());
                }
                String selectedItem = SessionManager.getStringSetting(ObdReaderActivity.this, SESSION_KEY_SELECTED_BLUETOOTH_DEVICE);
                if (spinnerObdDevices.getAdapter() == null) {
                    //did this for keep spinner default selected empty
                    categories.add(0, "None");

                    spinnerBluetoothAdapter = new ArrayAdapter<String>(ObdReaderActivity.this, android.R.layout.simple_spinner_item, categories);
                    spinnerBluetoothAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerObdDevices.setAdapter(spinnerBluetoothAdapter);
                }
            }
        } else {
            btnConnectBluetooth.setText("Enable Now");
            btnConnectBluetooth.setEnabled(true);
            spinnerObdDevices.setEnabled(false);
            edtObdCommand.setEnabled(false);
            btnSendObdCommand.setEnabled(false);
            btnConnectObd.setEnabled(false);
        }
    }

    private void connectBluetooth() {
        if (btAdapter == null) {
            btAdapter = BluetoothAdapter.getDefaultAdapter();
        }

        if (btAdapter != null && !btAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    private void disConnectBluetooth() {
        if (btAdapter == null) {
            btAdapter = BluetoothAdapter.getDefaultAdapter();
        }

        if (btAdapter != null && btAdapter.isEnabled()) {
            btAdapter.disable();
        }
    }

    /*******************
     * Methods for obd *
     *******************/

    private void connectObd() {
        Log.d(TAG, "Starting obd connection..");
        llResponseList.removeAllViews();

        doBindService();

        setObdStatus();

        acquireWakeLock();
    }

    private void disconnectObd() {
        Log.d(TAG, "Stopping obd connection..");

        doUnbindService();

        releaseWakeLockIfHeld();

        sendLogToDeveloper();
    }

    @Override
    public void stateUpdate(final ObdCommandJob job) {
        final String cmdName = job.getCommand().getName();
        String cmdResult = "";
//        View rowView = layoutInflater.inflate(R.layout.row_obd2_response, llResponseList, true);
//        TextView tvCommand = (TextView) rowView.findViewById(R.id.tv_command);
//        TextView tvResponse = (TextView) rowView.findViewById(R.id.tv_response);

        String commandName = "";
        String commandResponse = "";
        if (job.getState().equals(ObdCommandJob.ObdCommandJobState.EXECUTION_ERROR)) {
            cmdResult = job.getCommand().getResult();
            if (cmdResult != null && isServiceBound) {

                Log.d(TAG, cmdName);
                Log.d(TAG, cmdResult.toLowerCase());
//                tvCommand.setText(cmdName);
//                tvResponse.setText(cmdResult.toLowerCase());
            }
        } else if (job.getState().equals(ObdCommandJob.ObdCommandJobState.BROKEN_PIPE)) {
            if (isServiceBound) {

                Log.d(TAG, cmdName);
                Log.d(TAG, getString(R.string.status_obd_broken_pipe) + "Disconnecting OBD");
//                tvCommand.setText(cmdName);
//                tvResponse.setText(getString(R.string.status_obd_broken_pipe) + "Disconnecting OBD");
                disconnectObd();
            }
        } else if (job.getState().equals(ObdCommandJob.ObdCommandJobState.NOT_SUPPORTED)) {

            Log.d(TAG, cmdName);
            Log.d(TAG, getString(R.string.status_obd_no_support));
//            tvCommand.setText(cmdName);
//            tvResponse.setText(getString(R.string.status_obd_no_support));
        } else {
            cmdResult = job.getCommand().getFormattedResult();
            if (isServiceBound) {
                Log.d(TAG, cmdName);
                Log.d(TAG, cmdResult);
//                tvCommand.setText(cmdName);
//                tvResponse.setText(cmdResult);
            }
        }

        commandResult.put(cmdName.toUpperCase(), cmdResult);
    }

    private void setObdStatus() {

        if (service != null && service.isServiceRunning() && service.isObdConnected()) {
            btnConnectObd.setText("Connected");
            btnSendObdCommand.setEnabled(true);
            edtObdCommand.setEnabled(true);
            lastObdConnectedStatus = true;
        } else {
            btnConnectObd.setText("Connect Now");
            btnSendObdCommand.setEnabled(false);
            edtObdCommand.setEnabled(false);
            lastObdConnectedStatus = false;
        }
    }

    /**************************
     * Methods for obd sercie *
     **************************/

    private void doBindService() {
        if (!isServiceBound) {
            Log.d(TAG, "Binding OBD service..");
            if (btAdapter != null && btAdapter.isEnabled()) {
                Intent serviceIntent = new Intent(this, ObdReaderService.class);
                bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
            }
        }
    }

    private void doUnbindService() {
        if (isServiceBound) {
            if (service.isServiceRunning()) {
                try {
                    service.stopService();
                } catch (Exception ioe) {
                }
            }
            Log.d(TAG, "Unbinding OBD service..");
            unbindService(serviceConnection);
            isServiceBound = false;
            setObdStatus();
            setBluetoothStatus();
        }
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            Log.d(TAG, className.toString() + " service is bound");
            isServiceBound = true;
            service = (ObdReaderService) ((AbstractService.AbstractServiceBinder) binder).getService();
            service.setContext(ObdReaderActivity.this);
            Log.d(TAG, "Starting live data");
            try {
                service.startService();
            } catch (Exception ioe) {
                Log.e(TAG, "Failure Starting live data");
                doUnbindService();
            }
            setObdStatus();
        }

        @Override
        protected Object clone() throws CloneNotSupportedException {
            return super.clone();
        }

        // This method is *only* called when the connection to the service is lost unexpectedly
        // and *not* when the client unbinds (http://developer.android.com/guide/components/bound-services.html)
        // So the isServiceBound attribute should also be set to false when we unbind from the service.
        @Override
        public void onServiceDisconnected(ComponentName className) {
            Log.d(TAG, className.toString() + " service is unbound");
            isServiceBound = false;
        }
    };


    /*************************
     * Methods for wake lock *
     *************************/

    private void releaseWakeLockIfHeld() {

        if (powerManager == null) {
            powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        }

        if (wakeLock == null) {
            wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "ObdReader");
        }

        if (wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

    private void acquireWakeLock() {

        if (powerManager == null) {
            powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        }

        if (wakeLock == null) {
            wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "ObdReader");
        }

        if (!wakeLock.isHeld()) {
            wakeLock.acquire();
        }
    }

    /***********************************
     * Methods for bluetooth broadcast *
     ***********************************/

    //Run on UI
    private Runnable sendBluetoothStatusUpdatesToUI = new Runnable() {
        public void run() {
            setBluetoothStatus();
        }
    };

    private final BroadcastReceiver bluetoothBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
//                if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_OFF) {
//                    // Bluetooth is disconnected, do handling here
//                }
                new Handler().postDelayed(sendBluetoothStatusUpdatesToUI, 10);
            }

        }

    };

    // unregister bluetooth broadcast receiver
    private void unregisterBluetoothBroadcast() {
        if (receiverManager.isReceiverRegistered(bluetoothBroadcastReceiver)) {
            receiverManager.unregisterReceiver(bluetoothBroadcastReceiver);
        }
    }

    // unregister bluetooth broadcast receiver
    private void registerBluetoothBroadcast() {
        if (!receiverManager.isReceiverRegistered(bluetoothBroadcastReceiver)) {
            receiverManager.registerReceiver(bluetoothBroadcastReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        }
    }
}
