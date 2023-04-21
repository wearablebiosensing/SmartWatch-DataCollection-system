package com.example.carewear;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.ArrayList;

public class SensorService extends Service implements SensorEventListener {
        String TAG = "SensorService";
    public static final   String COUNTDOWN_BR  = "com.example.carewear";
        Intent intentSensorService = new Intent(COUNTDOWN_BR);
        public int fileIsWritten = 0;
        int intent_isFinished;
        FileIO fileio = new FileIO();
        SensorManager mSensorManagerAcc;
        SensorManager mSensorManagerGry;
        SensorManager mSensorManagerHr;
        ArrayList<String> AccelerometerData = new ArrayList<String>();
        ArrayList<String> GryData = new ArrayList<String>();
        ArrayList<String> HRData = new ArrayList<String>();
        boolean stop_data_collection = false;

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
    /*
    * Receive Intent from the BackgroundTimer class and set the intent_isFinished to
    1 once it is done to let the class know when to save the file.
    * */
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            intent_isFinished = intent.getIntExtra("isTimerFinished",0);

        }

    };



    @Override
    public void onDestroy() {
//        unregisterReceiver(broadcastReceiver);
        super.onDestroy();
    }

    protected void onResume() {
            //  Register broadcast receiver from Background Timer to check if it is over or not.
            registerReceiver(broadcastReceiver,new IntentFilter(BackgroundTimer.COUNTDOWN_BR));
            Log.i(TAG,"Registered Broadcast Receiver");

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

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    public void getAccelerometerData(SensorEvent event ){
        // System.out.println(TAG + ": Intent from Background Timer Variable: intent_isFinished " + intent_isFinished);
        // event.timestamp: The time in nanoseconds at which the event happened.
        // event.values: public final float[]	values
        String data_accelerometer = event.timestamp + "," + String.valueOf(event.values[0]) + "," + String.valueOf(event.values[1] + "," + String.valueOf(event.values[2]));
        //System.out.println("data_accelerometer: " + data_accelerometer);
        AccelerometerData.add(data_accelerometer);

    }
    public void getGryData(SensorEvent event ){
        String data_gryo =  "," + event.timestamp + "," + String.valueOf(event.values[0]) + "," + String.valueOf(event.values[1]) + "," + String.valueOf(event.values[2]);
        GryData.add(data_gryo);


        }
    public void getHrData(SensorEvent event){
        // System.out.println("gry_data: " + data_gryo);
        String data_hr = event.timestamp + "," + "-1" + ",-1," + String.valueOf(event.values[0]);
        HRData.add(data_hr);

    }


        // SensorEvent: This class represents a Sensor event and holds information such as the sensor's type, the time-stamp, accuracy and of course the sensor's data.@Override
        @Override
    public void onSensorChanged(SensorEvent event) {

            //Log.i(TAG,"onSensorChanged() broadcasted from timer intent_isFinished : " + intent_isFinished);
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                getAccelerometerData(event);
            }
            else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE){
                getGryData(event);
            }
            else if (event.sensor.getType() == Sensor.TYPE_HEART_RATE) {
                getHrData(event);

            }
            Log.i(TAG,"intent_isFinished: Registered Broadcast Receiver: " +intent_isFinished);

            // Get intent form background Timer and once finished save the files and clear the array data.
            if(intent_isFinished == 1){
                Log.d(TAG,"Message from backgroung timer that is done one: "+intent_isFinished);
                Log.d(TAG,"onSensorChanged() Call SAVE DATA CLASS hr/.");
                fileio.save_data( HRData, "1Hz" + "_hr");
                Log.d(TAG,"onSensorChanged() Call SAVE DATA CLASS gry/.");
                fileio.save_data( GryData, "30Hz" + "_gry");
                Log.d(TAG,"onSensorChanged() Call SAVE DATA CLASS acc/.");
                fileio.save_data( AccelerometerData, "30Hz" + "_acc");
                // set intent finished back to 0
                intent_isFinished = 0;
                // Clear the previous written array.
                HRData.clear();
                GryData.clear();
                AccelerometerData.clear();
               // fileIsWritten = 1;
            }
        }




}
