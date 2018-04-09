package com.nmsl.smartmouse;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.UUID;


public class BluetoothService {

    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private BluetoothAdapter bluetoothAdapter;

    private int state;

    private ConnectThread connectThread;
    private ConnectedThread connectedThread;

    private Activity activity;
    private Handler handler;

    private static final int STATE_NONE = 0; // we're doing nothing
    private static final int STATE_LISTEN = 1; // now listening for incoming connections
    private static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    private static final int STATE_CONNECTED = 3; // now connected to a remote device


    public BluetoothService(Activity inputActivity, Handler inputHandler) {
        activity = inputActivity;
        handler = inputHandler;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public boolean getDeviceState(){
        if (bluetoothAdapter == null) {
            return false;
        } else {
            return true;
        }
    }

    public void enableBluetooth() {
        if (bluetoothAdapter.isEnabled()){
            Log.d("Check if Bluetooth is enabled", "Bluetooth enabled");
            scanDevice();
        } else {
            Intent i = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(i, 33);
        }
    }

    public void scanDevice() {
        Intent serverIntent = new Intent(activity, DeviceListActivity.class);
        activity.startActivityForResult(serverIntent, 34);
    }

    public void getDeviceInfo(Intent data) {
        String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);

        connect(device);
    }

    private synchronized void setState(int inputState) {
        state = inputState;
    }

    private synchronized int getState() {
        return state;
    }

    private synchronized void start() {
        if (connectThread == null) {

        } else {
            connectThread.cancel();
            connectThread = null;
        }

        if (connectedThread == null) {

        } else {
            connectedThread.cancel();
            connectedThread = null;
        }
    }

    public synchronized void connect(BluetoothDevice device) {
        if (state == STATE_CONNECTING) {
            if (connectThread == null) {

            } else {
                connectThread.cancel();
                connectThread = null;
            }
        }

        if (connectedThread == null) {

        } else {
            connectedThread.cancel();
            connectedThread = null;
        }

        connectThread = new ConnectThread(device);
        connectThread.start();
        setState(STATE_CONNECTING);
    }

    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        if (connectThread == null) {

        } else {
            connectThread.cancel();
            connectThread = null;
        }

        if (connectedThread == null) {

        } else {
            connectedThread.cancel();
            connectedThread = null;
        }

        connectedThread = new ConnectedThread(socket);
        connectedThread.start();

        setState(STATE_CONNECTED);
    }

    public synchronized void stop() {
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }
        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        setState(STATE_NONE);
    }

    public void write(byte[] out) {
        ConnectedThread r;
        synchronized (this) {
            if (state != STATE_CONNECTED)
                return;
            r = connectedThread;
        }
    }

    private void connectionFailed() {
        setState(STATE_LISTEN);
    }

    private void connectionLost() {
        setState(STATE_LISTEN);
    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device)
            throws IOException {
        if(Build.VERSION.SDK_INT >= 10){
            try {
                final Method m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", new Class[] { UUID.class });
                return (BluetoothSocket) m.invoke(device, MY_UUID);
            } catch (Exception e) {
                Log.e("HEHE", "Could not create Insecure RFComm Connection",e);
            }
        }
        return  device.createRfcommSocketToServiceRecord(MY_UUID);
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket bluetoothSocket;
        private final BluetoothDevice bluetoothDevice;

        public ConnectThread(BluetoothDevice device) {
            bluetoothDevice = device;
            BluetoothSocket tmp = null;

            try{
                Log.d("TMP6", "TMP6");
                tmp = createBluetoothSocket(device);
                Log.d("TMP7", "TMP7");
            } catch (IOException e) {
                e.printStackTrace();
            }
            bluetoothSocket = tmp;
        }

        public void run() {
            setName("ConnectThread");

            bluetoothAdapter.cancelDiscovery();

            try {
                Log.d("TMP4", "TMP4");
                bluetoothSocket.connect();
                Log.d("TMP5", "TMP5");
            } catch (IOException e) {
                connectionFailed();
                try {
                    bluetoothSocket.close();
                } catch (IOException s) {
                    s.printStackTrace();
                }
                BluetoothService.this.start();
                return;
            }

            synchronized (BluetoothService.this) {
                connectedThread = null;
            }

            connected(bluetoothSocket, bluetoothDevice);
        }

        public void cancel() {
            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket bluetoothSocket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        public ConnectedThread(BluetoothSocket socket) {
            bluetoothSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                Log.d("TMP8", "TMP8");
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
                Log.d("TMP9", "TMP9");
            } catch (IOException e) {
                e.printStackTrace();
            }

            inputStream = tmpIn;
            outputStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;
            Log.d("TMP", "TMP");

            while (true) {
                try {
                    Log.d("TMP2", "TMP2");
                    outputStream.write("HELLO".getBytes());
                    bytes = inputStream.read(buffer);
                    Log.d("TMP3", "TMP3");
                } catch (IOException e) {
                    e.printStackTrace();
                    connectionLost();
                    break;
                }
            }
        }

        public void write(byte[] buffer) {
            try{
                outputStream.write(buffer);
            } catch (IOException e){
                e.printStackTrace();
            }
        }

        public void cancel() {
            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
