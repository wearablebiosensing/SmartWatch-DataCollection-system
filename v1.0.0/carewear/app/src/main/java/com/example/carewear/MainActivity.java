package com.example.carewear;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.carewear.databinding.ActivityMainBinding;
import com.google.firebase.FirebaseApp;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
/*
* GitHub Link:
* //https://github.com/wearablebiosensing/SmartWatch-DataCollection-system
*
* */

public class MainActivity extends Activity {
    private static final int PERMISSION_REQUEST_CODE = 1;
    String TAG = "MainActivity";
    private TextView txt;
    int intent_isFinished ;
    private ActivityMainBinding binding;
    Intent clockTimer;
    Intent intentSensorActivity;

    private TextView internetStatusTextView; // TextView for displaying internet status

    // BroadcastReceiver to listen for connectivity changes
    private BroadcastReceiver networkChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateInternetStatus();
        }
    };
    // Method to update the internet status indicator
    private void updateInternetStatus() {
        if (NetworkUtils.isNetworkAvailable(this)) {
            internetStatusTextView.setText("Internet: Connected");
            internetStatusTextView.setTextColor(Color.GREEN);
        } else {
            internetStatusTextView.setText("Internet: Not Connected");
            internetStatusTextView.setTextColor(Color.RED);
        }
    }

    Intent intentSensorActivityAcc;
    Intent intentSensorActivityGry;
    Intent intentSensorActivityHr;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Check for internet connectivity
        if (NetworkUtils.isNetworkAvailable(this)) {
            // Internet connection is available
            // Perform your tasks requiring internet here
            Toast.makeText(this, "Internet connection available", Toast.LENGTH_SHORT).show();
        } else {
            // No internet connection
            // Handle the lack of connectivity here
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
        }




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
        requestPermissions();

        //Display Internet Connection
        // Initialize the TextView for internet status
        internetStatusTextView = findViewById(R.id.internetStatusTextView);
        updateInternetStatus(); // Initial check for internet connectivity


        //ID
        TextView deviceIdTextView = findViewById(R.id.deviceIdTextView);
        String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        String displayId = deviceId.substring(0, Math.min(deviceId.length(), 8)); // Display first 8 characters

        // Set the processed ID to TextView
        deviceIdTextView.setText("Device ID: " + displayId);

    }



    // Method to check and request permissions
    private void requestPermissions() {
        String[] permissions = {
                Manifest.permission.BODY_SENSORS,
                Manifest.permission.BODY_SENSORS_BACKGROUND,
                Manifest.permission.FOREGROUND_SERVICE,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.WAKE_LOCK,
                Manifest.permission.RECEIVE_BOOT_COMPLETED,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.CHANGE_NETWORK_STATE,
        };

        // Check if the permissions are already granted
        if (checkPermissions(permissions)) {
            // Permissions are already granted
            // You can perform your desired action here
        } else {
            // Request permissions
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
        }
    }

    // Check if permissions are granted
    private boolean checkPermissions(String[] permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    // Handle permission request response
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permissions granted
                // You can perform your desired action here
            } else {
                // Permissions denied
                // You may handle the denied permission case here
            }
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
        // Unregister the BroadcastReceiver
        unregisterReceiver(networkChangeReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(broadcastReceiver,new IntentFilter(BackgroundTimer.COUNTDOWN_BR));
        Log.i(TAG,"Registered Broadcast Reviever");

        // Register BroadcastReceiver to listen for connectivity changes
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkChangeReceiver, filter);

    }

    private void  updatedTimer(Intent intent){
        if(intent.getExtras()!=null){
            long milisUntillFinished = intent.getLongExtra("countdown",300000);
            Log.i(TAG ,"Countdown remaining:  " +milisUntillFinished);
        }
    }

    // ...


    // ...


}