package com.example.carewear;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.example.carewear.databinding.ActivityMainBinding;


public class MainActivity extends Activity {
    String TAG = "MainActivity";
    private TextView txt;
    int intent_isFinished ;
    private ActivityMainBinding binding;
    Intent clockTimer;
    Intent intentSensorActivity;

    Intent intentSensorActivityAcc;
    Intent intentSensorActivityGry;
    Intent intentSensorActivityHr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "Build Version -------- " + String.valueOf(Build.VERSION.SDK_INT));
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        txt = binding.text;
        clockTimer = new Intent(this, BackgroundTimer.class);
        Context context = getApplicationContext();
        if(!foregroundServiceRunning()) {
            startForegroundService(clockTimer);

            Log.i(TAG,"clockTimer Service has started countdown begins.");
            intentSensorActivity = new Intent(this, SensorService.class);
            startForegroundService(intentSensorActivity);

        }


        Log.i(TAG,"intentSensorActivityAcc Service has started");
        //Intent intentLocationService = new Intent(this, LocationService.class);
        //startService(intentLocationService);
        Log.i(TAG,"intentLocationService Service has started");
        if (checkSelfPermission(Manifest.permission.BODY_SENSORS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions( new String[]{Manifest.permission.BODY_SENSORS}, 1);

        }
       /* if(checkSelfPermission(Manifest.permission.HIGH_SAMPLING_RATE_SENSORS) != PackageManager.PERMISSION_GRANTED)){
            requestPermissions( new String[]{Manifest.permission.HIGH_SAMPLING_RATE_SENSORS}, 1);

        }*/
        if(checkSelfPermission(Manifest.permission.BODY_SENSORS_BACKGROUND) != PackageManager.PERMISSION_GRANTED ){
            requestPermissions( new String[]{Manifest.permission.BODY_SENSORS_BACKGROUND}, 1);

        }
        if(checkSelfPermission(Manifest.permission.WAKE_LOCK) != PackageManager.PERMISSION_GRANTED ) {
            requestPermissions( new String[]{Manifest.permission.WAKE_LOCK}, 1);

        }
        if(checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ) {
            requestPermissions( new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);

        }




        }
    public boolean foregroundServiceRunning(){
        ActivityManager activityManager = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
        for(ActivityManager.RunningServiceInfo service: activityManager.getRunningServices(Integer.MAX_VALUE)){
                if(SensorService.class.getName().equals(service.service.getClassName()) &&
                        BackgroundTimer.class.getName().equals(service.service.getClassName())

                ){
                    return true;
                }

        }
        return false;
    }
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updatedTimer(intent);
            intent_isFinished = intent.getIntExtra("isTimerFinished",0);

        }


    };

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(broadcastReceiver,new IntentFilter(BackgroundTimer.COUNTDOWN_BR));
        Log.i(TAG,"Registered Broadcast Reviever");

    }

    private void  updatedTimer(Intent intent){
        if(intent.getExtras()!=null){
            long milisUntillFinished = intent.getLongExtra("countdown",300000);
            Log.i(TAG ,"Countdown remaining:  " +milisUntillFinished);
        }
    }
}