package com.example.carewear;

import android.app.Activity;
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
        Log.i(TAG,"clockTimer Service has started Coundown begins.");
        intentSensorActivity = new Intent(this, SensorService.class);
        startService(intentSensorActivity);
        Log.i(TAG,"intentSensorActivityAcc Service has started");

//        intentSensorActivityAcc = new Intent(this, AccSensorService.class);
//        startService(intentSensorActivityAcc);
//        Log.i(TAG,"intentSensorActivityAcc Service has started");

       // intentSensorActivityGry = new Intent(this, GrySensorService.class);
       // startService(intentSensorActivityGry);
       // Log.i(TAG,"intentSensorActivityGry Service has started");

      //  intentSensorActivityHr = new Intent(this, HrSensorService.class);
        //startService(intentSensorActivityHr);
      //  Log.i(TAG,"intentSensorActivityHr Service has started");


        //Intent intentLocationService = new Intent(this, LocationService.class);
        //startService(intentLocationService);
        Log.i(TAG,"intentLocationService Service has started");

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
 //       unregisterReceiver(broadcastReceiver);
        Log.i(TAG, "unregistered broadcast revciever");
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(broadcastReceiver,new IntentFilter(BackgroundTimer.COUNTDOWN_BR));
        Log.i(TAG,"Registered Broadcast Reviever");
    }

    private void  updatedTimer(Intent intent){
        if(intent.getExtras()!=null){
            long milisUntillFinished = intent.getLongExtra("countdown",30000);
            //Log.i(TAG ,"Countdown remaining " +milisUntillFinished);
        }
    }
}