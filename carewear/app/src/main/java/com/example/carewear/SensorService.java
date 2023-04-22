package com.example.carewear;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
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
import android.os.PowerManager;
import android.util.Log;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.legacy.content.WakefulBroadcastReceiver;

import java.util.ArrayList;

public class SensorService extends Service implements SensorEventListener {
        String TAG = "SensorService";
        private static PowerManager.WakeLock wakeLock = null;
        public int isOnDestroy = 0;
        private static final String LOCK_TAG = "WALKING_DETECTOR";

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




    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize the Sensor Manager
        mSensorManagerAcc = (SensorManager) getSystemService(SENSOR_SERVICE);
        mSensorManagerGry = (SensorManager) getSystemService(SENSOR_SERVICE);
        mSensorManagerHr  = (SensorManager) getSystemService(SENSOR_SERVICE);
       // getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //acquireLock(this);


    }

    /*
    * Receive Intent from the BackgroundTimer class and set the intent_isFinished to
    1 once it is done to let the class know when to save the file.
    * */
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            intent_isFinished = intent.getIntExtra("isTimerFinished",0);
            //startWakefulService(context, SensorService.class);
        }
    };



   @Override
   public int onStartCommand(final Intent intent, int flags, int startId) {
       registerReceiver(broadcastReceiver,new IntentFilter(BackgroundTimer.COUNTDOWN_BR));
       onResume();
       final String CHANNEL_ID = "ForegroundSensorsService";
       Notification notification = new NotificationCompat.Builder(
               this, "ForegroundSensorsService")
               .setContentTitle("Foreground Sensors Service")
               .setContentText("Sensor Service Running")
               .setSmallIcon(R.mipmap.ic_launcher)
              .build();
       /* NotificationChannel channel = new NotificationChannel(CHANNEL_ID,CHANNEL_ID, NotificationManager.IMPORTANCE_LOW);
        getSystemService(NotificationManager.class).createNotificationChannel(channel);
      Notification.Builder notification = new Notification.Builder(this)
               .setContentText("Sensor Service is running..")
               .setContentTitle("Service Enabled")
               .setSmallIcon(R.drawable.ic_launcher_background); */

       startForeground(1,notification);


       return super.onStartCommand(intent,flags,startId);
   }
   /*@Override
    public void onDestroy() {
            Log.d(TAG,"UNRegistered Broadcast Receiver");
            if(isOnDestroy == 1){
                unregisterReceiver(broadcastReceiver);

            }

       //registerReceiver(broadcastReceiver,new IntentFilter(BackgroundTimer.COUNTDOWN_BR));

        super.onDestroy();
    }*/

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    protected void onResume() {
            Log.d(TAG, "onResume(): isOnDestroy "+isOnDestroy);
            isOnDestroy = 1;
            //  Register broadcast receiver from Background Timer to check if it is over or not.
            //Log.i(TAG,"Registered Broadcast Receiver");

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
            Log.i(TAG,"intent_isFinished: Intent recieved - : " +intent_isFinished);

           // Log.i(TAG,"onSensorChanged() broadcasted from timer intent_isFinished : " + intent_isFinished);
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                getAccelerometerData(event);
            }
            else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE){
                getGryData(event);
            }
            else if (event.sensor.getType() == Sensor.TYPE_HEART_RATE) {
                getHrData(event);

            }
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
    @SuppressLint("InvalidWakeLockTag")
    public static synchronized void acquireLock(Context ctx) {
        if (wakeLock == null) {
            PowerManager mgr = (PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
            wakeLock = mgr.newWakeLock(PowerManager.FULL_WAKE_LOCK, LOCK_TAG);
            wakeLock.setReferenceCounted(true);
        }
        wakeLock.acquire();
    }

    public static synchronized void releaseLock() {
        if (wakeLock != null) {
            if (wakeLock.isHeld())
            {
                wakeLock.release();
            }
        }
    }


}
