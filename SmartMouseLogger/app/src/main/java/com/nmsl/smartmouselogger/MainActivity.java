package com.nmsl.smartmouselogger;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import java.util.Date;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;



public class MainActivity extends AppCompatActivity{

    LinearAccelerometer linearAccelerometer;
    Button recordStartButton;
    Button recordStopButton;

    FileOutputStream output = null;
    Boolean isWriting = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recordStartButton = (Button) findViewById(R.id.recordStartButton);
        recordStopButton = (Button) findViewById(R.id.recordStopButton);

        if (isExternalStorageWritable()) {
            try {
                output = new FileOutputStream(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/outlog.csv", false);
                output.write(("AccX" + "," + "AccY" + "," + "AccZ" + ","
                        + "eventValuesWithoutNoiseX" + "," + "eventValuesWithoutNoiseY" + "," + "eventValuesWithoutNoiseZ" + ","
                        + "VelocityX" + "," + "VelocityY" + "," + "VelocityZ" + ","
                        + "DisplacementX" + "," + "DisplacementY" + "," + "DisplacementZ" + ","
                        + "Timestamp" + "\n").getBytes());
                isWriting = true;
            } catch (FileNotFoundException e){
                e.printStackTrace();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }


        //accelerometer = new Accelerometer();
        linearAccelerometer = new LinearAccelerometer();
        //gyroscope = new Gyroscope();


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
/*
        recordStartButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                if (isExternalStorageWritable()) {
                    try {
                        output = new FileOutputStream(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/outlog.csv", false);
                        output.write(("AccX" + "," + "AccY" + "," + "AccZ" + ","
                                + "VelocityX" + "," + "VelocityY" + "," + "VelocityZ" + ","
                                + "DisplacementX" + "," + "DisplacementY" + "," + "DisplacementZ" + ","
                                + "Timestamp" + "\n").getBytes());
                        isWriting = true;
                    } catch (FileNotFoundException e){
                        e.printStackTrace();
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                }
            }
        });
*/
        recordStopButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (isWriting) {
                    try {
                        output.close();
                        Log.i("LIAAO", "the file closed");
                        isWriting = false;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
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
    }

    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    private class LinearAccelerometer implements SensorEventListener {
        final SensorManager mSensorManager;
        final Sensor mLinearAccelerometer;
        float[] lastValues = new float[3];
        float[] deltaValues = new float[3];
        float[] velocity = new float[3];
        float[] displacement = new float[3];
        float[] eventValuesWithoutNoise = new float[3];
        float z_initial_displacement = 0;
        long last_timestamp = 0;
        long curr_timestamp = 0;
        long time_interval = 0;
        boolean initialized;

        LinearAccelerometer () {
            mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            mLinearAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
            //accX = (TextView) findViewById(R.id.accX);
            //accY = (TextView) findViewById(R.id.accY);
            //accZ = (TextView) findViewById(R.id.accZ);
            /*
            lastValues[0] = 0;
            lastValues[1] = 0;
            lastValues[2] = 0;
            deltaValues[0] = 0;
            deltaValues[1] = 0;
            deltaValues[2] = 0;
            initialized = false;
            NOISE = (float) 0.0;
            */
            initialized = false;
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
                last_timestamp = new Date().getTime();
                velocity[0] = 0;
                velocity[1] = 0;
                velocity[2] = 0;
                //lastValues[0] = event.values[0];
                //lastValues[1] = event.values[1];
                //lastValues[2] = event.values[2];
                initialized = true;
            } else {
                //accX.setText("accX: " + event.values[0]);
                //accY.setText("accY: " + event.values[1]);
                //accZ.setText("accZ: " + event.values[2]);

                curr_timestamp = new Date().getTime();
                time_interval = curr_timestamp - last_timestamp;
                //Log.i("LIAAO", time_interval + "," + velocity[0] * time_interval + "," + event.values[0] * time_interval * time_interval / 2);
                eventValuesWithoutNoise[0] = event.values[0];
                eventValuesWithoutNoise[1] = event.values[1];
                eventValuesWithoutNoise[2] = event.values[2];
                if (Math.abs(event.values[0]) < 0.5) {
                    eventValuesWithoutNoise[0] = 0;
                }
                if (Math.abs(event.values[1]) < 0.5) {
                    eventValuesWithoutNoise[1] = 0;
                }
                if (Math.abs(event.values[2]) < 0.5) {
                    eventValuesWithoutNoise[2] = 0;
                }

                //displacement[0] = velocity[0] * time_interval + event.values[0] * time_interval * time_interval / 2;
                //displacement[1] = velocity[1] * time_interval + event.values[1] * time_interval * time_interval / 2;
                //displacement[2] = velocity[2] * time_interval + event.values[2] * time_interval * time_interval / 2;

                displacement[0] = velocity[0] * time_interval + eventValuesWithoutNoise[0] * time_interval * time_interval / 2;
                displacement[1] = velocity[1] * time_interval + eventValuesWithoutNoise[1] * time_interval * time_interval / 2;
                displacement[2] = velocity[2] * time_interval + eventValuesWithoutNoise[2] * time_interval * time_interval / 2;


                /*
                if (z_initial_displacement == 0 & displacement[2] == 0){
                    displacement[2] = velocity[2] * time_interval + event.values[2] * time_interval * time_interval / 2;
                    z_initial_displacement = displacement[2];
                } else {
                    displacement[2] = velocity[2] * time_interval + event.values[2] * time_interval * time_interval / 2;
                }
                */
                velocity[0] = velocity[0] + eventValuesWithoutNoise[0] * time_interval; //TODO: Check the unit, it is m/s^2!
                velocity[1] = velocity[1] + eventValuesWithoutNoise[1] * time_interval; //TODO: Check the unit, it is m/s^2!
                velocity[2] = velocity[2] + eventValuesWithoutNoise[2] * time_interval; //TODO: Check the unit, it is m/s^2!


                //deltaValues[0] = lastValues[0] - event.values[0];
                //deltaValues[1] = lastValues[1] - event.values[1];
                //deltaValues[2] = lastValues[2] - event.values[2];
                //if (Math.abs(deltaValues[0]) < NOISE) deltaValues[0] = (float) 0.0;
                //if (Math.abs(deltaValues[1]) < NOISE) deltaValues[1] = (float) 0.0;
                //if (Math.abs(deltaValues[2]) < NOISE) deltaValues[2] = (float) 0.0;
                //lastValues[0] = event.values[0];
                //lastValues[1] = event.values[1];
                //lastValues[2] = event.values[2];
                //Log.i("SMARTMOUSE", deltaValues[0]+","+deltaValues[1]+","+deltaValues[2]+","+ new Timestamp(event.timestamp));
                //sendMessage(deltaValues[0]+","+deltaValues[1]+","+deltaValues[2]+","+new Date().getTime());
                //Log.i("SMARTMOUSE", event.values[0] + "," + event.values[1] + "," + event.values[2] + "," + new Date().getTime());
                //Log.i("SMARTMOUSE", eventValues[0]+","+deltaValues[1]+","+deltaValues[2]+","+ new Timestamp(date.getTime()));
                try {
                    if (isWriting) {
                        output.write((event.values[0] + "," + event.values[1] + "," + event.values[2] + ","
                                + eventValuesWithoutNoise[0] + "," + eventValuesWithoutNoise[1] + "," + eventValuesWithoutNoise[2] + ","
                                + velocity[0] + "," + velocity[1] + "," + velocity[2] + ","
                                + displacement[0] + "," + displacement[1] + "," + displacement[2] + ","
                                + curr_timestamp + "\n").getBytes());
                        //output.write((event.values[0] + "," + event.values[1] + "," + event.values[2] + "," + displacement[0] * z_initial_displacement / displacement[2] + "," + displacement[1] + "," + displacement[2] + "," + curr_timestamp + "\n").getBytes());

                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                last_timestamp = curr_timestamp;
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // Do nothing here.
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
