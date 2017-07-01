//package com.reversecoder.obd.reader.thread;
//
//import android.bluetooth.BluetoothSocket;
//import android.util.Log;
//
//import com.reversecoder.obd2.commands.protocol.EchoOffCommand;
//import com.reversecoder.obd2.commands.protocol.LineFeedOffCommand;
//import com.reversecoder.obd2.commands.protocol.ObdRawCommand;
//import com.reversecoder.obd2.commands.protocol.ObdResetCommand;
//import com.reversecoder.obd2.commands.protocol.SelectProtocolCommand;
//import com.reversecoder.obd2.commands.protocol.TimeoutCommand;
//import com.reversecoder.obd2.enums.ObdProtocols;
//
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.OutputStream;
//
//public class ObdSingleCommandThread extends Thread {
//
//    private final InputStream mmInStream;
//    private final OutputStream mmOutStream;
//    private BluetoothSocket mSocket;
//    private ObdRawCommand obdRawCommand;
//    String mCommand;
//    private static String TAG = ObdSingleCommandThread.class.getSimpleName();
//
//    public ObdSingleCommandThread(BluetoothSocket socket, String command) {
//        mSocket = socket;
//        mCommand = command;
//        InputStream tmpIn = null;
//        OutputStream tmpOut = null;
//
//        try {
//            tmpIn = socket.getInputStream();
//            tmpOut = socket.getOutputStream();
//        } catch (IOException e) {
//            Log.v("e", "e");
//        }
//
//        mmInStream = tmpIn;
//        mmOutStream = tmpOut;
//
//        obdRawCommand = new ObdRawCommand(command);
//    }
//
//    public void run() {
//        byte[] buffer = new byte[1024];  // buffer store for the stream
//        int bytes; // bytes returned from read()
//
//        setObdSettings();
//
//        try {
//            while (!Thread.currentThread().isInterrupted()) {
//                Log.d(TAG, "Inside while");
//
//                try {
//                    obdRawCommand.run(mmInStream, mmOutStream);
//                    obdRawCommand.getFormattedResult();
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            Log.d(TAG, "inside single command thread catch before while");
//        }
//    }
//
//    private void setObdSettings() { // execute commands
//        try {
//            new ObdResetCommand().run(mmInStream, mmOutStream);
//            new EchoOffCommand().run(mmInStream, mmOutStream);
//            new LineFeedOffCommand().run(mmInStream, mmOutStream);
//            new TimeoutCommand(100).run(mmInStream, mmOutStream);
//            new SelectProtocolCommand(ObdProtocols.ISO_15765_4_CAN).run(mmInStream, mmOutStream);
//
//        } catch (Exception e) {
//            Log.v("OBDcmds", "e");
//            // handle errors
//        }
//    }
//
//    public void cancel() {
//        try {
//            mSocket.close();
//        } catch (IOException e) {
//        }
//    }
//}