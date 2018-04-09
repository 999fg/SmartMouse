package com.nmsl.smartmouse;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;



public class MainActivity extends AppCompatActivity{

    Accelerometer accelerometer;
    TextView accX, accY, accZ;
    Button connectButton;
    private BluetoothService bluetoothService;

    private final int REQUEST_BLUETOOTH_ENABLE = 100;

    ConnectedTask mConnectedTask = null;
    static BluetoothAdapter mBluetoothAdapter;
    private String mConnectedDeviceName = null;
    static boolean isConnectionError = false;
    private static final String TAG = "BluetoothClient";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        accelerometer = new Accelerometer();


        Log.d( TAG, "Initalizing Bluetooth adapter...");

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            showErrorDialog("This device does not have Bluetooth module.");
            return;
        }

        if (!mBluetoothAdapter.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, REQUEST_BLUETOOTH_ENABLE);
        }
        else {
            Log.d(TAG, "Initialization successful.");

            showPairedDevicesListDialog();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        accelerometer.registerListener();
    }

    @Override
    protected void onPause() {
        super.onPause();
        accelerometer.unregisterListener();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if ( mConnectedTask != null ) {

            mConnectedTask.cancel(true);
        }
    }

    private class Accelerometer implements SensorEventListener {
        final SensorManager mSensorManager;
        final Sensor mAccelerometer;

        Accelerometer () {
            mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            accX = (TextView) findViewById(R.id.accX);
            accY = (TextView) findViewById(R.id.accY);
            accZ = (TextView) findViewById(R.id.accZ);
        }

        void registerListener() {
            mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }

        void unregisterListener() {
            mSensorManager.unregisterListener(this);
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            accX.setText("accX: "+ event.values[0]);
            accY.setText("accY: "+ event.values[1]);
            accZ.setText("accZ: "+ event.values[2]);
            Log.d("ACCELEROMETER", event.values+"");
            sendMessage(event.values[0]+","+event.values[1]+","+event.values[2]);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // Do nothing here.
        }
    }

    private class ConnectTask extends AsyncTask<Void, Void, Boolean> {

        private BluetoothSocket mBluetoothSocket = null;
        private BluetoothDevice mBluetoothDevice = null;

        ConnectTask(BluetoothDevice bluetoothDevice) {
            mBluetoothDevice = bluetoothDevice;
            mConnectedDeviceName = bluetoothDevice.getName();

            //SPP
            UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

            try {
                mBluetoothSocket = mBluetoothDevice.createRfcommSocketToServiceRecord(uuid);
                Log.d( TAG, "create socket for "+mConnectedDeviceName);

            } catch (IOException e) {
                Log.e( TAG, "socket create failed " + e.getMessage());
            }
        }


        @Override
        protected Boolean doInBackground(Void... params) {

            // Always cancel discovery because it will slow down a connection
            mBluetoothAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mBluetoothSocket.connect();
            } catch (IOException e) {
                // Close the socket
                try {
                    mBluetoothSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() " +
                            " socket during connection failure", e2);
                }

                return false;
            }

            return true;
        }


        @Override
        protected void onPostExecute(Boolean isSucess) {

            if ( isSucess ) {
                connected(mBluetoothSocket);
            }
            else{

                isConnectionError = true;
                Log.d( TAG,  "Unable to connect device");
                showErrorDialog("Unable to connect device");
            }
        }
    }


    public void connected( BluetoothSocket socket ) {
        mConnectedTask = new ConnectedTask(socket);
        mConnectedTask.execute();
    }



    private class ConnectedTask extends AsyncTask<Void, String, Boolean> {

        private InputStream mInputStream = null;
        private OutputStream mOutputStream = null;
        private BluetoothSocket mBluetoothSocket = null;

        ConnectedTask(BluetoothSocket socket){

            mBluetoothSocket = socket;
            try {
                mInputStream = mBluetoothSocket.getInputStream();
                mOutputStream = mBluetoothSocket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "socket not created", e );
            }

            Log.d( TAG, "connected to "+mConnectedDeviceName);
        }


        @Override
        protected Boolean doInBackground(Void... params) {

            byte [] readBuffer = new byte[1024];
            int readBufferPosition = 0;


            while (true) {

                if ( isCancelled() ) return false;

                try {

                    int bytesAvailable = mInputStream.available();

                    if(bytesAvailable > 0) {

                        byte[] packetBytes = new byte[bytesAvailable];

                        mInputStream.read(packetBytes);

                        for(int i=0;i<bytesAvailable;i++) {

                            byte b = packetBytes[i];
                            if(b == '\n')
                            {
                                byte[] encodedBytes = new byte[readBufferPosition];
                                System.arraycopy(readBuffer, 0, encodedBytes, 0,
                                        encodedBytes.length);
                                String recvMessage = new String(encodedBytes, "UTF-8");

                                readBufferPosition = 0;

                                Log.d(TAG, "recv message: " + recvMessage);
                                publishProgress(recvMessage);
                            }
                            else
                            {
                                readBuffer[readBufferPosition++] = b;
                            }
                        }
                    }
                } catch (IOException e) {

                    Log.e(TAG, "disconnected", e);
                    return false;
                }
            }

        }

        @Override
        protected void onProgressUpdate(String... recvMessage) {
            Log.d(TAG, mConnectedDeviceName + ": " + recvMessage[0]);
        }

        @Override
        protected void onPostExecute(Boolean isSucess) {
            super.onPostExecute(isSucess);

            if ( !isSucess ) {


                closeSocket();
                Log.d(TAG, "Device connection was lost");
                isConnectionError = true;
                showErrorDialog("Device connection was lost");
            }
        }

        @Override
        protected void onCancelled(Boolean aBoolean) {
            super.onCancelled(aBoolean);

            closeSocket();
        }

        void closeSocket(){

            try {

                mBluetoothSocket.close();
                Log.d(TAG, "close socket()");

            } catch (IOException e2) {

                Log.e(TAG, "unable to close() " +
                        " socket during connection failure", e2);
            }
        }

        void write(String msg){

            msg += "\n";

            try {
                mOutputStream.write(msg.getBytes());
                mOutputStream.flush();
            } catch (IOException e) {
                Log.e(TAG, "Exception during send", e );
            }
        }
    }


    public void showPairedDevicesListDialog()
    {
        Set<BluetoothDevice> devices = mBluetoothAdapter.getBondedDevices();
        final BluetoothDevice[] pairedDevices = devices.toArray(new BluetoothDevice[0]);

        if ( pairedDevices.length == 0 ){
            showQuitDialog( "No devices have been paired.\n"
                    +"You must pair it with another device.");
            return;
        }

        String[] items;
        items = new String[pairedDevices.length];
        for (int i=0;i<pairedDevices.length;i++) {
            items[i] = pairedDevices[i].getName();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select device");
        builder.setCancelable(false);
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();

                ConnectTask task = new ConnectTask(pairedDevices[which]);
                task.execute();
            }
        });
        builder.create().show();
    }



    public void showErrorDialog(String message)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Quit");
        builder.setCancelable(false);
        builder.setMessage(message);
        builder.setPositiveButton("OK",  new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                if ( isConnectionError  ) {
                    isConnectionError = false;
                    finish();
                }
            }
        });
        builder.create().show();
    }


    public void showQuitDialog(String message)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Quit");
        builder.setCancelable(false);
        builder.setMessage(message);
        builder.setPositiveButton("OK",  new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                finish();
            }
        });
        builder.create().show();
    }

    void sendMessage(String msg){

        if ( mConnectedTask != null ) {
            mConnectedTask.write(msg);
            Log.d(TAG, "send message: " + msg);
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if(requestCode == REQUEST_BLUETOOTH_ENABLE){
            if (resultCode == RESULT_OK){
                //BlueTooth is now Enabled
                showPairedDevicesListDialog();
            }
            if(resultCode == RESULT_CANCELED){
                showQuitDialog( "You need to enable bluetooth");
            }
        }
    }
}
