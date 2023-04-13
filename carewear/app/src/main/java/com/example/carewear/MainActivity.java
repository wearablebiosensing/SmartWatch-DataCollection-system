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
    private ActivityMainBinding binding;
//    public SharedPreferences isTimerFinished_sharedPreferences ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        txt = binding.text;
        // Init Shared Preferences.
//        isTimerFinished_sharedPreferences = getSharedPreferences("isTimerDone", MODE_PRIVATE);
        Intent clockTimer = new Intent(this, BackgroundTimer.class);
        startService(clockTimer);
        Log.i(TAG,"clockTimer Service has started Coundown begins.");

        Intent intentSensorActivity = new Intent(this, SensorService.class);
        startService(intentSensorActivity);
        Log.i(TAG,"intentSensorActivity Service has started");
        //Intent intentLocationService = new Intent(this, LocationService.class);
        //startService(intentLocationService);
        Log.i(TAG,"intentLocationService Service has started");

    }
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updatedTimer(intent);
           // int intent_isFinished = intent.getIntExtra("isTimerFinished",0);
            //System.out.println(TAG+ "Intent from Background Timer Variable: intent_isFinished " + intent_isFinished);

        }


    };

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(broadcastReceiver);
        Log.i(TAG, "unregistered broadcast revciever");
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(broadcastReceiver,new IntentFilter(BackgroundTimer.COUNTDOWN_BR));
        Log.i(TAG,"Registered Broadcast Reviever");
    }

    /*@Override
    protected void onStop() {
        stopService(new Intent(this,BackgroundTimmer.class));
        Log.i(TAG,"Registered Broadcast stopped");

        super.onStop();

    }*/
    private void  updatedTimer(Intent intent){
        if(intent.getExtras()!=null){
            long milisUntillFinished = intent.getLongExtra("countdown",30000);
            Log.i(TAG ,"Countdown remaining " +milisUntillFinished);
        }
    }
}