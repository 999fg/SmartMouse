package com.nmsl.smartmouse;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
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
    Gyroscope gyroscope;
    LinearAccelerometer linearAccelerometer;
    TextView accX, accY, accZ;
    Button connectButton;
    private BluetoothService bluetoothService;

    private final int REQUEST_BLUETOOTH_ENABLE = 100;

    ConnectedTask mConnectedTask = null;
    static BluetoothAdapter mBluetoothAdapter;
    private String mConnectedDeviceName = null;
    static boolean isConnectionError = false;
    private static final String TAG = "BluetoothClient";
    FileOutputStream output = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //accelerometer = new Accelerometer();
        linearAccelerometer = new LinearAccelerometer();
        //gyroscope = new Gyroscope();

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


        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        35);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }

        if (isExternalStorageWritable()) {
            try {
                output = new FileOutputStream(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/out.csv", false);
            } catch (FileNotFoundException e){
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        //accelerometer.registerListener();
        //gyroscope.registerListener();
        linearAccelerometer.registerListener();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //accelerometer.unregisterListener();
        //gyroscope.unregisterListener();
        linearAccelerometer.unregisterListener();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if ( mConnectedTask != null ) {

            mConnectedTask.cancel(true);
        }
    }

    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }


    private class Accelerometer implements SensorEventListener {
            final SensorManager mSensorManager;
            final Sensor mAccelerometer;
            private final float NOISE;
            float[] lastValues = new float[3];
            float[] deltaValues = new float[3];
            boolean initialized;

            Accelerometer () {
                mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
                mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                accX = (TextView) findViewById(R.id.accX);
                accY = (TextView) findViewById(R.id.accY);
                accZ = (TextView) findViewById(R.id.accZ);
                lastValues[0] = 0;
                lastValues[1] = 0;
                lastValues[2] = 0;
                deltaValues[0] = 0;
                deltaValues[1] = 0;
                deltaValues[2] = 0;
                initialized = false;
                NOISE = (float) 2.0;
            }

            void registerListener() {
                mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
            }

            void unregisterListener() {
                mSensorManager.unregisterListener(this);
            }

            @Override
            public void onSensorChanged(SensorEvent event) {

                if (!initialized) {
                    lastValues[0] = event.values[0];
                    lastValues[1] = event.values[1];
                    lastValues[2] = event.values[2];
                    initialized = true;
                } else {
                    accX.setText("accX: " + event.values[0]);
                    accY.setText("accY: " + event.values[1]);
                    accZ.setText("accZ: " + event.values[2]);
                    deltaValues[0] = lastValues[0] - event.values[0];
                    deltaValues[1] = lastValues[1] - event.values[1];
                    deltaValues[2] = lastValues[2] - event.values[2];
                    if (Math.abs(deltaValues[0]) < NOISE) deltaValues[0] = (float) 0.0;
                    if (Math.abs(deltaValues[1]) < NOISE) deltaValues[1] = (float) 0.0;
                    if (Math.abs(deltaValues[2]) < NOISE) deltaValues[2] = (float) 0.0;
                    lastValues[0] = event.values[0];
                    lastValues[1] = event.values[1];
                    lastValues[2] = event.values[2];
                    Date date = new Date();
                    //Log.i("SMARTMOUSE", deltaValues[0]+","+deltaValues[1]+","+deltaValues[2]+","+ new Timestamp(event.timestamp));
                    //sendMessage(deltaValues[0]+","+deltaValues[1]+","+deltaValues[2]+","+new Date().getTime());
                    Log.i("SMARTMOUSE", deltaValues[0] + "," + deltaValues[1] + "," + deltaValues[2] + "," + new Date().getTime());
                    //Log.i("SMARTMOUSE", eventValues[0]+","+deltaValues[1]+","+deltaValues[2]+","+ new Timestamp(date.getTime()));
                    try {
                        output.write((deltaValues[0] + "," + deltaValues[1] + "," + deltaValues[2] + "," + new Date().getTime()+"\n").getBytes());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
                // Do nothing here.
            }
    }

    private class LinearAccelerometer implements SensorEventListener {
        final SensorManager mSensorManager;
        final Sensor mLinearAccelerometer;
        private final float NOISE;
        float[] lastValues = new float[3];
        float[] deltaValues = new float[3];
        boolean initialized;

        LinearAccelerometer () {
            mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            mLinearAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
            //accX = (TextView) findViewById(R.id.accX);
            //accY = (TextView) findViewById(R.id.accY);
            //accZ = (TextView) findViewById(R.id.accZ);
            lastValues[0] = 0;
            lastValues[1] = 0;
            lastValues[2] = 0;
            deltaValues[0] = 0;
            deltaValues[1] = 0;
            deltaValues[2] = 0;
            initialized = false;
            NOISE = (float) 0.0;
        }

        void registerListener() {
            mSensorManager.registerListener(this, mLinearAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        }

        void unregisterListener() {
            mSensorManager.unregisterListener(this);
        }

        @Override
        public void onSensorChanged(SensorEvent event) {

            if (!initialized) {
                lastValues[0] = event.values[0];
                lastValues[1] = event.values[1];
                lastValues[2] = event.values[2];
                initialized = true;
            } else {
                //accX.setText("accX: " + event.values[0]);
                //accY.setText("accY: " + event.values[1]);
                //accZ.setText("accZ: " + event.values[2]);
                /*
                deltaValues[0] = lastValues[0] - event.values[0];
                deltaValues[1] = lastValues[1] - event.values[1];
                deltaValues[2] = lastValues[2] - event.values[2];
                if (Math.abs(deltaValues[0]) < NOISE) deltaValues[0] = (float) 0.0;
                if (Math.abs(deltaValues[1]) < NOISE) deltaValues[1] = (float) 0.0;
                if (Math.abs(deltaValues[2]) < NOISE) deltaValues[2] = (float) 0.0;
                lastValues[0] = event.values[0];
                lastValues[1] = event.values[1];
                lastValues[2] = event.values[2];
                */
                Date date = new Date();
                //Log.i("SMARTMOUSE", deltaValues[0]+","+deltaValues[1]+","+deltaValues[2]+","+ new Timestamp(event.timestamp));
                //sendMessage(deltaValues[0]+","+deltaValues[1]+","+deltaValues[2]+","+new Date().getTime());
                Log.i("SMARTMOUSE", event.values[0] + "," + event.values[1] + "," + event.values[2] + "," + new Date().getTime());
                //Log.i("SMARTMOUSE", eventValues[0]+","+deltaValues[1]+","+deltaValues[2]+","+ new Timestamp(date.getTime()));
                try {
                    output.write((event.values[0] + "," + event.values[1] + "," + event.values[2] + "," + new Date().getTime()+"\n").getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // Do nothing here.
        }
    }

    private class Gyroscope implements SensorEventListener {
        final SensorManager mSensorManager;
        final Sensor mGyroscope;
        float[] gyro = new float[3];
        float[] deltaRotationVector = new float[4];
        private static final float NS2S = 1.0f / 1000000000.0f;
        float omegaMagnitude;
        float timestamp;
        float[] deltaRotationMatrix = new float[9];

        Gyroscope () {
            mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        }

        void registerListener() {
            mSensorManager.registerListener(this, mGyroscope, SensorManager.SENSOR_DELAY_FASTEST);
        }

        void unregisterListener() {
            mSensorManager.unregisterListener(this);
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (timestamp != 0) {
                final float dT = (event.timestamp - timestamp) * NS2S;
                gyro[0] = event.values[0];
                gyro[1] = event.values[1];
                gyro[2] = event.values[2];
                omegaMagnitude = (float) Math.sqrt(gyro[0] * gyro[0] + gyro[1] * gyro[1] + gyro[2] * gyro[2]);
                if (true) {
                    gyro[0] /= omegaMagnitude;
                    gyro[1] /= omegaMagnitude;
                    gyro[2] /= omegaMagnitude;
                }
                deltaRotationVector[0] = (float) Math.sin(omegaMagnitude * dT / 2.0f) * gyro[0];
                deltaRotationVector[1] = (float) Math.sin(omegaMagnitude * dT / 2.0f) * gyro[1];
                deltaRotationVector[2] = (float) Math.sin(omegaMagnitude * dT / 2.0f) * gyro[2];
                deltaRotationVector[3] = (float) Math.cos(omegaMagnitude * dT / 2.0f);
            }
            timestamp = event.timestamp;
            SensorManager.getRotationMatrixFromVector(deltaRotationMatrix, deltaRotationVector);
            for(int i =0; i<deltaRotationMatrix.length; i++){
                Log.i("ROTATION"+i, deltaRotationMatrix[i]+"");
            }
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

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 35: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }
}
