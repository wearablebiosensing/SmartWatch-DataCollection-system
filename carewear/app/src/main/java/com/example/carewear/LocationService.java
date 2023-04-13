package com.example.carewear;


import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.ArrayList;


public class LocationService extends Service{
    String TAG = "LocationTrakerBackgroundService";
    int intent_isFinished;
    ArrayList<String> GPSlocationData = new ArrayList<String>();
    FileIO fileio = new FileIO();

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


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Thread locationThread  = new Thread(
                new Runnable() {
                    @Override
                    public void run() {

                        LocationTrackListener locationTrack;


                        while (true){
                            locationTrack = new LocationTrackListener(LocationService.this);

                            if (locationTrack.canGetLocation()) {
                                double longitude = locationTrack.getLongitude();
                                double latitude = locationTrack.getLatitude();
                                System.out.println(java.time.Clock.systemUTC().instant());
                                Log.e(TAG, "Track User Location Every 2 seconds \nTime:" + java.time.Clock.systemUTC().instant() + "\n Longitude: "+ Double.toString(longitude) + "\nLatitude:" + Double.toString(latitude));
                                GPSlocationData.add( java.time.Clock.systemUTC().instant() + "\n Longitude: "+ Double.toString(longitude) + "\nLatitude:" + Double.toString(latitude));
                            } else {

                                locationTrack.showSettingsAlert();
                            }

//                            Log.e(TAG, "Track User Location Every 2 seconds");
//                            Toast.makeText(getApplicationContext(), "Longitude:" + Double.toString(longitude) + "\nLatitude:" + Double.toString(latitude), Toast.LENGTH_SHORT).show();

                            try {
                                Thread.sleep(2000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                        }
                    }
                }

        );
        Log.d(TAG,"LocationService() Call SAVE DATA CLASS hr/." +intent_isFinished);

        if (intent_isFinished == 1){
            // Stop thread from running and save data.
            fileio.save_data( GPSlocationData, "0.5Hz" + "_location");

            Log.d(TAG,"LocationService() Call SAVE DATA CLASS hr/.");
            locationThread.interrupt();

        }
        else if (intent_isFinished == 0){
            locationThread.start();

        }


        return super.onStartCommand(intent, flags, startId);
    }
}