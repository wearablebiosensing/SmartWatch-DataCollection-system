package com.example.carewear;
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
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.os.StatFs;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;


public class SensorService extends Service implements SensorEventListener {
        String TAG = "SensorService";
        public static final   String COUNTDOWN_BR  = "com.example.carewear";
        Intent intentSensorService = new Intent(COUNTDOWN_BR);
        int intent_isFinished;
        FileIO fileio = new FileIO();
        SensorManager mSensorManagerAcc;
        SensorManager mSensorManagerGry;
        SensorManager mSensorManagerHr;
        ArrayList<String> AccelerometerData = new ArrayList<String>();
        ArrayList<String> GryData = new ArrayList<String>();
        ArrayList<String> HRData = new ArrayList<String>();
        ArrayList<String> BatteryInfo = new ArrayList<String>();
        int level;



    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize the Sensor Manager
        mSensorManagerAcc = (SensorManager) getSystemService(SENSOR_SERVICE);
        mSensorManagerGry = (SensorManager) getSystemService(SENSOR_SERVICE);
        mSensorManagerHr  = (SensorManager) getSystemService(SENSOR_SERVICE);
        Log.d(TAG,"onCreate() mSensorManagerAcc:----  "+mSensorManagerAcc);
        Log.d(TAG,"onCreate() mSensorManagerGry:----  "+mSensorManagerGry);
        Log.d(TAG,"onCreate()  mSensorManagerHr:---- "+mSensorManagerHr);
//        File sdCard = Environment.getExternalStorageDirectory();
            /*Gets the SD CARD STORAGE AAVALIABLE Approx 7GB */
        StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
        long bytesAvailable;
        if (android.os.Build.VERSION.SDK_INT >=
                android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
            bytesAvailable = stat.getBlockSizeLong() * stat.getAvailableBlocksLong();
        }
        else {
            bytesAvailable = (long)stat.getBlockSize() * (long)stat.getAvailableBlocks();
        }
        long megAvailable = bytesAvailable / (1024 * 1024*1024);
        Log.e(TAG,"STORAGE INFO - Available GB : "+megAvailable);

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
    private BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context arg0, Intent intent) {
            // TODO Auto-generated method stub
            level = intent.getIntExtra("level", 0);
            Log.d(TAG,"mBatInfoReceiver BATTERY LEVEL INFO ------------------- "+ String.valueOf(level) + "%");
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
            LocalDateTime now = LocalDateTime.now();
            System.out.println(dtf.format(now));
            String batteryinfo= dtf.format(now) + ","+ String.valueOf(level);
            BatteryInfo.add(batteryinfo);
            //contentTxt.setText(String.valueOf(level) + "%");
        }
    };

   @Override
   public int onStartCommand(final Intent intent, int flags, int startId) {
       final String CHANNELID = "Foreground Service ID";
       NotificationChannel channel = new NotificationChannel(
               CHANNELID,
               CHANNELID,
               NotificationManager.IMPORTANCE_LOW
       );

       getSystemService(NotificationManager.class).createNotificationChannel(channel);
       Notification.Builder notification = new Notification.Builder(this, CHANNELID)
               .setContentText("Service is running")
               .setContentTitle("Service enabled")
               .setSmallIcon(R.drawable.ic_launcher_background);

       startForeground(1001, notification.build());

       registerReceiver(broadcastReceiver,new IntentFilter(BackgroundTimer.COUNTDOWN_BR));
       registerReceiver(mBatInfoReceiver,
               new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
       onResume();


       return super.onStartCommand(intent,flags,startId);
   }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    protected void onResume() {

        //super.onResume();

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

            Sensor mHeartRate = mSensorManagerHr.getDefaultSensor(Sensor.TYPE_HEART_RATE);
                if (mSensorManagerHr.getDefaultSensor(Sensor.TYPE_HEART_RATE) != null) {
                    Log.d(TAG,"============== HR SENSOR  AVAILABLE==============");
                } else {
                    Log.d(TAG,"============== HR SENSOR NOT AVAILABLE==============");
                }
            //Log.d(TAG,"HEART RATE SENSOR ----------: "+heart_rate);

        // To change the sampling rate apply it in microseconds.
            mSensorManagerHr.registerListener((SensorEventListener) this, mHeartRate, (1 / 1) * 1000000); // 1 Hz // 1000000 = 1Hz
            Log.d(TAG,"HEART RATE SENSOR mSensorManagerHr ----------: "+mSensorManagerHr);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void getAccelerometerData(SensorEvent event ){
        // System.out.println(TAG + ": Intent from Background Timer Variable: intent_isFinished " + intent_isFinished);
        // event.timestamp: The time in nanoseconds at which the event happened.
        // event.values: public final float[]	values
        String data_accelerometer = event.timestamp + "," + String.valueOf(event.values[0]) + "," + String.valueOf(event.values[1] + "," + String.valueOf(event.values[2]));
        //Log.d(TAG,"acc_data: "+data_accelerometer);

        //System.out.println("data_accelerometer: " + data_accelerometer);
        AccelerometerData.add(data_accelerometer);

    }
    public void getGryData(SensorEvent event ){
        String data_gryo =   event.timestamp + "," + String.valueOf(event.values[0]) + "," + String.valueOf(event.values[1]) + "," + String.valueOf(event.values[2]);
       // Log.d(TAG,"data_gryo: "+data_gryo);
        GryData.add(data_gryo);
    }
    public void getHrData(SensorEvent event){
        String data_hr =   event.timestamp + "," + String.valueOf(event.values[0]);

       //String data_hr = event.timestamp + "," + String.valueOf(event.values[0]); //event.timestamp + "," + String.valueOf(event.values[0]); //format("%d,%s", event.timestamp, event.values[0]);
        Log.d(TAG,"hr_data: "+data_hr); // 402550836440 , 401550836440
        HRData.add(data_hr);

    }


        // SensorEvent: This class represents a Sensor event and holds information such as the sensor's type, the time-stamp, accuracy and of course the sensor's data.@Override
        @Override
    public void onSensorChanged(SensorEvent event) {
           // onResume(); // call this to register listiner for everytime HR was registered.

            // Log.i(TAG,"intent_isFinished: Intent recieved - : " +intent_isFinished);
           // Log.d(TAG,"SENSOR TYPE: event.sensor.getType() "+event.sensor.getType());
            //Log.d(TAG,"Sensor.TYPE_ACCELEROMETER  "+Sensor.TYPE_ACCELEROMETER);
            //Log.d(TAG,"Sensor.TYPE_GYROSCOPE  "+Sensor.TYPE_GYROSCOPE);
            //Log.d(TAG,"Sensor.TYPE_HEART_RATE  "+Sensor.TYPE_HEART_RATE);
           // Log.i(TAG,"onSensorChanged() broadcasted from timer intent_isFinished : " + intent_isFinished);
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

                getAccelerometerData(event);
            }// start_time - 148597546056210 # second st time - 148837473364559
            if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE){
                getGryData(event);
            }
            if (event.sensor.getType() == Sensor.TYPE_HEART_RATE) {
                getHrData(event);

            }
            //Log.d(TAG,"onSensorChanged() HR DATA --" +HRData);
            //Log.d(TAG,"onSensorChanged() ACC DATA --" +AccelerometerData);
            //Log.d(TAG,"onSensorChanged() GRY DATA --" +GryData);

            // Get intent form background Timer and once finished save the files and clear the array data.
            if(intent_isFinished == 1){
                //Log.d(TAG,"Message from backgroung timer that is done one: "+intent_isFinished);
                //Log.d(TAG,"onSensorChanged() Call SAVE DATA CLASS hr/.");
                //Log.d(TAG,"onSensorChanged() Call SAVE DATA CLASS gry/.");
//                BatteryInfo.add(batteryinfo);
                Log.d(TAG,"onSensorChanged(): bBATTERY INFO" +BatteryInfo);
                fileio.save_data( BatteryInfo, "onchange" + "_battery");

                fileio.save_data( GryData, "30Hz" + "_gry");
               // Log.d(TAG,"onSensorChanged() Call SAVE DATA CLASS gry/.");
                Log.d(TAG,"onSensorChanged() ACC DATA --" +AccelerometerData);
                fileio.save_data( AccelerometerData, "30Hz" + "_acc");
                Log.d(TAG,"onSensorChanges() HRData: "+HRData);

                fileio.save_data( HRData, "1Hz" + "_hr");
                //Log.d(TAG,"onSensorChanged() Call SAVE DATA CLASS hr/." +HRData);

                // Clear the previous written array.
                BatteryInfo.clear();
                GryData.clear();
                AccelerometerData.clear();
                HRData.clear();
                // set intent finished back to 0
                intent_isFinished = 0;

            }
        }



}
