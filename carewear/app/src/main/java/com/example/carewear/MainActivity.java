package com.example.carewear;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        txt = binding.text;
        clockTimer = new Intent(this, BackgroundTimer.class);
        startService(clockTimer);
        Log.i(TAG,"clockTimer Service has started countdown begins.");
        if(!foregroundServiceRunning()){
            intentSensorActivity = new Intent(this, SensorService.class);
            startForegroundService(intentSensorActivity);
        }

        Log.i(TAG,"intentSensorActivityAcc Service has started");
        //Intent intentLocationService = new Intent(this, LocationService.class);
        //startService(intentLocationService);
        Log.i(TAG,"intentLocationService Service has started");

    }
    public boolean foregroundServiceRunning(){
        ActivityManager activityManager = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
        for(ActivityManager.RunningServiceInfo service: activityManager.getRunningServices(Integer.MAX_VALUE)){
                if(SensorService.class.getName().equals(service.service.getClassName())){
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