package com.example.carewear;


import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;


public class LocationService extends Service{
    String TAG = "LocationTrakerBackgroundService";
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        new Thread(
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

        ).start();
        return super.onStartCommand(intent, flags, startId);
    }
}