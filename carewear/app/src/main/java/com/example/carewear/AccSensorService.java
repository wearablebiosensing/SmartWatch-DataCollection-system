package com.example.carewear;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.ArrayList;

public class AccSensorService extends Service implements SensorEventListener {
        String TAG = "AccelerometerService";
        CountDownTimer countDownTimer = null;
        private Sensor mAccelerometer;
//    public static final   String COUNTDOWN_BR  = "com.example.carewear";
        // Variables to store data streams from Sensosrs.

//        Intent intent = new  Intent(COUNTDOWN_BR);

        private String JSONdata;
        SensorManager mSensorManagerAcc;
        SensorManager mSensorManagerGry;
        SensorManager mSensorManagerHr;
        ArrayList<String> AccelerometerData = new ArrayList<String>();
        ArrayList<String> GryData = new ArrayList<String>();
        ArrayList<String> HRData = new ArrayList<String>();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize the Sensor Manager
        mSensorManagerAcc = (SensorManager) getSystemService(SENSOR_SERVICE);
        mSensorManagerGry = (SensorManager) getSystemService(SENSOR_SERVICE);
        mSensorManagerHr  = (SensorManager) getSystemService(SENSOR_SERVICE);
        onResume();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    protected void onResume() {
                // if recording acc data register listener for acc values.
                Sensor accelerometer = mSensorManagerAcc.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                // To change the sampling rate apply it in microseconds.
                // 1/#Hz * 1000000 micro seconds.
                mSensorManagerAcc.registerListener((SensorEventListener) this, accelerometer, (1 / 30) * 1000000); // 50 Hz // 20000 = 50Hz in microseconds
                Sensor gyroscope = mSensorManagerGry.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
                // To change the sampling rate apply it in microseconds.
                mSensorManagerGry.registerListener((SensorEventListener) this, gyroscope, (1 / 30) * 1000000); // 50 Hz // 20000 microseconds = 50Hz in
                Sensor heart_rate = mSensorManagerHr.getDefaultSensor(Sensor.TYPE_HEART_RATE);
                // To change the sampling rate apply it in microseconds.
                mSensorManagerHr.registerListener((SensorEventListener) this, heart_rate, (1 / 1) * 1000000); // 1 Hz // 1000000 = 1Hz

        }


        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }

        // SensorEvent: This class represents a Sensor event and holds information such as the sensor's type, the time-stamp, accuracy and of course the sensor's data.
        public void onSensorChanged(SensorEvent event) {
                        JSONdata = event.accuracy + "," + event.sensor;
                        // event.timestamp: The time in nanoseconds at which the event happened.
                        // event.values: public final float[]	values
                        String data_accelerometer = event.timestamp + "," + String.valueOf(event.values[0]) + "," + String.valueOf(event.values[1] + "," + String.valueOf(event.values[2]));
                        System.out.println("data_accelerometer: " + data_accelerometer);
                        AccelerometerData.add(data_accelerometer);
                        String data_gryo = event.sensor + "," + event.timestamp + "," + String.valueOf(event.values[0]) + "," + String.valueOf(event.values[1]) + "," + String.valueOf(event.values[2]);
                        GryData.add(data_gryo);
                       // System.out.println("gry_data: " + data_gryo);
                        String data_hr = event.sensor + "," + event.timestamp + "," + "0" + ",0," + String.valueOf(event.values[0]);
                        HRData.add(data_hr);
        }


}
